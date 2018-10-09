package com.redhat.coderland.reactica;

import com.redhat.coderland.reactica.model.Ride;
import com.redhat.coderland.reactica.model.User;
import io.reactivex.Completable;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.reactivex.CompletableHelper;
import io.vertx.reactivex.config.ConfigRetriever;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.eventbus.MessageConsumer;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.ext.web.handler.BodyHandler;
import io.vertx.reactivex.ext.web.handler.StaticHandler;
import io.vertx.reactivex.ext.web.handler.sockjs.SockJSHandler;
import me.escoffier.reactive.amqp.AmqpConfiguration;
import me.escoffier.reactive.amqp.AmqpToEventBus;
import me.escoffier.reactive.amqp.AmqpVerticle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

public class BillboardVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LogManager.getLogger(BillboardVerticle.class.getName());

  private WebClient client;
  private JsonArray last = new JsonArray();


  private long rideDuration;
  private Integer numberOfUsers;

  @Override
  public void start(Future<Void> done) {
    client = WebClient.create(vertx, new WebClientOptions().setDefaultHost("event-generator").setDefaultPort(8080));

    ConfigRetriever retriever = ConfigRetriever.create(vertx);

    retriever.rxGetConfig().doOnSuccess(json -> {
      configure(json);
      retriever.listen(c -> configure(c.getNewConfiguration()));
    }).ignoreElement()
      .andThen(initQueueEventsListener())
      .andThen(initNewUserListener())
      .andThen(initWaitingTimeListener())
      .andThen(deployAMQPVerticle())
      .andThen(setupSimulatorControl())
      .andThen(setupWebApp())
      .doOnComplete(() -> LOGGER.info("Initialization done"))
      .subscribe(CompletableHelper.toObserver(done));
  }

  private void configure(JsonObject json) {
    if (json == null) {
      return;
    }
    LOGGER.info("Configuring the billboard");
    rideDuration = json.getLong("duration-in-seconds", Ride.DEFAULT_RIDE_DURATION);
    numberOfUsers = json.getInteger("users-per-ride", Ride.DEFAULT_USER_ON_RIDE);
  }

  private Completable initWaitingTimeListener() {
    MessageConsumer<JsonObject> consumer = vertx.eventBus().consumer("waiting-time");
    consumer.handler(message -> {
      LOGGER.info("Received the expected wait time {}", message.body().encode());
      JsonObject attributes = new JsonObject().put("expected_wait_time",
        ((message.body().getLong("calculated-wait-time") + 1) * 1000));
      vertx.eventBus().send("queue:attributes", attributes);
    });
    return consumer.rxCompletionHandler();
  }

  private Completable setupSimulatorControl() {
    MessageConsumer<JsonObject> consumer = vertx.eventBus().consumer("control");
    consumer.handler(message -> {
      JsonObject body = message.body();
      String command = body.getString("cmd");
      switch (command) {
        case "start-ride-simulator":
          client.post("/simulators/ride").rxSendJsonObject(new JsonObject().put("enabled", true)).subscribe();
          break;
        case "stop-ride-simulator":
          client.post("/simulators/ride").rxSendJsonObject(new JsonObject().put("enabled", false)).subscribe();
          break;
        case "start-user-simulator":
          client.post("/simulators/users").rxSendJsonObject(new JsonObject().put("enabled", true)).subscribe();
          break;
        case "stop-user-simulator":
          client.post("/simulators/users").rxSendJsonObject(new JsonObject().put("enabled", false)).subscribe();
          break;
        case "clear-users":
          client.delete("/simulators/users").rxSend().subscribe();
          break;
        default:
          LOGGER.error("Invalid command: " + command);
      }
    });
    return consumer.rxCompletionHandler();
  }

  private JsonArray getLastReceivedQueue() {
    return last;
  }

  private Completable deployAMQPVerticle() {
    AmqpToEventBus currentQueue = new AmqpToEventBus();
    currentQueue.setAddress("cl-queue");
    currentQueue.setQueue("CL_QUEUE");

    AmqpToEventBus waitingTime = new AmqpToEventBus();
    waitingTime.setAddress("waiting-time");
    waitingTime.setQueue("QLC_QUEUE");

    AmqpConfiguration configuration = new AmqpConfiguration()
      .setContainer("amqp-examples")
      .setHost("eventstream-amq-amqp")
      .setPort(5672)
      .setUser("user")
      .setPassword("user123")
      .addAmqpToEventBus(currentQueue)
      .addAmqpToEventBus(waitingTime);

    return vertx.rxDeployVerticle(AmqpVerticle.class.getName(), new DeploymentOptions().setConfig(JsonObject.mapFrom(configuration))).ignoreElement();
  }

  private Completable initQueueEventsListener() {
    MessageConsumer<JsonObject> consumer = vertx.eventBus().consumer("cl-queue");
    consumer
      .handler(msg -> {
        JsonArray queue = msg.body().getJsonArray("queue");
        JsonArray res = new JsonArray();
        AtomicInteger numberOfPeopleWaiting = new AtomicInteger();
        queue
          .stream()
          .map(o -> (JsonObject) o)
          .sorted((j1, j2) -> j2.getLong("enterQueueTime").compareTo(j1.getLong("enterQueueTime")))
          .forEach(json -> {
          String user = json.getString("name");
          long enteredAt = json.getLong("enterQueueTime") * 1000; // UI expect milliseconds
          String state = json.getString("currentState");

          long eta = 0;
          if (state.equalsIgnoreCase(User.STATE_IN_QUEUE)) {
            eta =
              (Instant.now().toEpochMilli() / 1000
              + (numberOfPeopleWaiting.incrementAndGet() / numberOfUsers * rideDuration)
              )
              * 1000; // To milliseconds
          }

          res.add(new JsonObject()
            .put("name", user)
            .put("entered", enteredAt)
            .put("state", state)
            .put("eta", eta)
          );
        });

        // Send new queue to UI
        this.last = res;
        vertx.eventBus().send("queue:state", res);
      });
    return consumer.rxCompletionHandler();
  }

  private Completable initNewUserListener() {
    MessageConsumer<JsonObject> consumer = vertx.eventBus().consumer("queue:enter");
    consumer
      .handler(msg -> {
        String name = msg.body().getString("name");
        LOGGER.info("Adding user {}", name);
        addUserToQueue(name).subscribe();
      });
    return consumer.rxCompletionHandler();
  }

  private Completable addUserToQueue(String user) {
    return client.post("/user")
      .rxSendJsonObject(new JsonObject().put("name", user))
      .ignoreElement();
  }

  private Completable setupWebApp() {
    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());

    router.get("/api/queue/in-queue").handler(ctx -> {
      final JsonArray json = getLastReceivedQueue();
      ctx.response().putHeader("Content-Type", "application/json");
      ctx.response().end(json.encode());
    });

    router.get("/health").handler(rc -> rc.response().end("OK"));

    BridgeOptions opts = new BridgeOptions()
      .addOutboundPermitted(new PermittedOptions().setAddress("queue:state"))
      .addOutboundPermitted(new PermittedOptions().setAddress("queue:attributes"))
      .addInboundPermitted(new PermittedOptions().setAddress("queue:enter"))
      .addInboundPermitted(new PermittedOptions().setAddress("control"));

    // Create the event bus bridge and add it to the router.
    SockJSHandler ebHandler = SockJSHandler.create(vertx).bridge(opts);
    router.route("/eb/*").handler(ebHandler);
    router.route().handler(StaticHandler.create());

    return vertx.createHttpServer().requestHandler(router::accept).rxListen(8080).ignoreElement();
  }
}


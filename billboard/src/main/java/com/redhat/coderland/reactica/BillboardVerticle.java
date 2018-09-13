package com.redhat.coderland.reactica;

import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.reactivex.CompletableHelper;
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

public class BillboardVerticle extends AbstractVerticle {

  private static final int MS_PER_MIN = 60 * 1000;
  private static final Logger LOGGER = LogManager.getLogger(BillboardVerticle.class.getName());

  private WebClient client;
  private JsonArray last = new JsonArray();

  @Override
  public void start(Future<Void> done) {
    // TODO use real ETA computation
    vertx.setPeriodic(5000, t -> {
      // ETA should be expressed in milliseconds - e.g. for a 2 minute expected wait time,
      // pass 2 * MS_PER_MIN
      long expected_wait_time = 2 * MS_PER_MIN;
      JsonObject queue_attributes = new JsonObject().put("expected_wait_time", expected_wait_time);
      vertx.eventBus().send("queue:attributes", queue_attributes);
    });

    client = WebClient.create(vertx, new WebClientOptions().setDefaultHost("event-generator").setDefaultPort(8080));

    initQueueEventsListener()
      .andThen(initNewUserListener())
      .andThen(deployAMQPVerticle())
      .andThen(setupSimulatorControl())
      .andThen(setupWebApp())
      .doOnComplete(() -> LOGGER.info("Initialization done"))
      .subscribe(CompletableHelper.toObserver(done));
  }

  private CompletableSource setupSimulatorControl() {
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

    AmqpConfiguration configuration = new AmqpConfiguration()
      .setContainer("amqp-examples")
      .setHost("eventstream-amq-amqp")
      .setPort(5672)
      .setUser("user")
      .setPassword("user123")
      .addAmqpToEventBus(currentQueue);

    return vertx.rxDeployVerticle(AmqpVerticle.class.getName(), new DeploymentOptions().setConfig(JsonObject.mapFrom(configuration))).ignoreElement();
  }

  private Completable initQueueEventsListener() {
    MessageConsumer<JsonObject> consumer = vertx.eventBus().consumer("cl-queue");
    consumer
      .handler(msg -> {
        JsonArray queue = msg.body().getJsonArray("queue");
        JsonArray res = new JsonArray();
        queue.forEach(o -> {
          JsonObject json = (JsonObject) o;
          String user = json.getString("name");
          long enteredAt = json.getLong("enterTime") * 1000; // UI expect milliseconds
          String state = json.getString("currentState");

          res.add(new JsonObject()
            .put("name", user)
            .put("entered", enteredAt)
            .put("state", state)
            .put("eta", System.currentTimeMillis()) // TODO Fix me.
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


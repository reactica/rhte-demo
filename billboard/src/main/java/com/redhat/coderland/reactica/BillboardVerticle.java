package com.redhat.coderland.reactica;

import com.redhat.coderland.reactica.model.User;
import io.reactivex.Completable;
import io.reactivex.Single;
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
import me.escoffier.reactive.rhdg.AsyncCache;
import me.escoffier.reactive.rhdg.DataGridClient;
import me.escoffier.reactive.rhdg.DataGridConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

public class BillboardVerticle extends AbstractVerticle {

  private static final int MS_PER_MIN = 60 * 1000;
  private static final Logger LOGGER = LogManager.getLogger(BillboardVerticle.class.getName());

  private List<JsonObject> queue = new ArrayList<>();
  private AsyncCache<String, String> cache;

  private WebClient client;

  @Override
  public void start(Future<Void> done) {
    // TODO use real ETA computation
    vertx.setPeriodic(5000, t -> {
      long estimatedWaitingTime = queue.stream().filter(j -> j.getString("state").equalsIgnoreCase(User.STATE_IN_QUEUE)).count() / 10 * MS_PER_MIN;
      JsonObject queue_attributes = new JsonObject().put("expected_wait_time", estimatedWaitingTime);
      vertx.eventBus().send("queue:attributes", queue_attributes);
    });

    client = WebClient.create(vertx, new WebClientOptions().setDefaultHost("event-generator").setDefaultPort(8080));

    Single<DataGridClient> single = DataGridClient.create(vertx, new DataGridConfiguration()
      .setHost("eventstore-dg-hotrod")
      .setPort(11333));

    single.flatMap(client -> client.<String, String>getCache("users"))
      .subscribe(
        cache -> {
          this.cache = cache;
          LOGGER.info("User Cache initialized");
          done.complete(null);
        },
        err -> {
          LOGGER.error("Unable to initialize the User Cache");
          done.fail(err);
        }
      );

    listenForClQueue()
      .andThen(listenForUsers())
      .andThen(deployAMQPVerticle())
      .andThen(setupWebApp())
      .doOnComplete(() -> LOGGER.info("Initialization done"))
      .subscribe(CompletableHelper.toObserver(done));
  }

  private JsonArray getQueue() {
    JsonArray result = new JsonArray();
    for (JsonObject json : queue) {
      result.add(json);
    }
    return result;
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

  private Completable listenForClQueue() {
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
        vertx.eventBus().send("queue:state", res);
      });
    return consumer.rxCompletionHandler();
  }

  private Completable listenForUsers() {
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
      // TODO Fix this...
      final JsonArray json = getQueue();
      ctx.response().putHeader("Content-Type", "application/json");
      ctx.response().end(json.encode());
    });

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


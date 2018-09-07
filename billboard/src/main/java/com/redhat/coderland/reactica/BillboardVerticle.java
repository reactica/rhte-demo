package com.redhat.coderland.reactica;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;

import java.util.Date;

public class BillboardVerticle extends AbstractVerticle {

  private static final int MS_PER_MIN = 60 * 1000;

  // TODO: remove (for testing only)
  private static String[] NAMES = new String[]{"James", "Rodney", "Thomas", "Clement", "Dan", "Sameer"};
  private static String[] STATES = new String[]{"IN_QUEUE", "ON_RIDE", "COMPLETED_RIDE"};

  private Logger log = LoggerFactory.getLogger(BillboardVerticle.class.getName());

  @Override
  public void start() {
    // TODO: remove (for test purposes only)
    System.setProperty("vertx.disableFileCaching", "true");

    Router router = Router.router(vertx);

    router.route().handler(BodyHandler.create());

    router.get("/api/queue/all").handler(ctx -> {
      // TODO: remove (for test purposes only)
      final JsonArray json = generateRandomQueueState();
      ctx.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/json");
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

    // TODO: re-enable cache (disabled for testing only)
    router.route().handler(StaticHandler.create().setCachingEnabled(false).setMaxAgeSeconds(0));

    vertx.createHttpServer().requestHandler(router::accept).listen(8080);

    EventBus eb = vertx.eventBus();

    // TODO: remove (for test purposes only)
    vertx.setPeriodic(5000, t -> {
      // send back random queue
      final JsonArray queue_state = generateRandomQueueState();
      eb.send("queue:state", queue_state);

      // send back random expected wait time 1-5 minutes
      final JsonObject queue_attributes = new JsonObject();
      queue_attributes.put("expected_wait_time", MS_PER_MIN + Math.round(Math.random() * 4 * MS_PER_MIN));
      eb.send("queue:attributes", queue_attributes);
    });

    MessageConsumer<JsonObject> control_consumer = eb.consumer("control");

    // TODO: complete reception of start/stop messages to start/stop ride
    control_consumer.handler(message -> {
      log.info("Received CONTROL message: " + message.body().toString());
      JsonObject controlObj = message.body();
      String cmd = controlObj.getString("cmd");

      switch (cmd) {
        case "start":
          // TODO
          log.info("Starting ride");
          break;
        case "stop":
          // TODO
          log.info("Stopping ride");
          break;
        default:
          log.error("Unknown command: " + cmd);
      }
    });

    MessageConsumer<JsonObject> rider_cosumer = eb.consumer("queue:enter");

    // TODO: complete reception of start/stop messages to start/stop ride
    rider_cosumer.handler(message -> {
      log.info("Received RIDER ENTRY message: " + message.body().toString());
      JsonObject entry_message = message.body();
      String rider_name = entry_message.getString("name");
      log.info("Rider entering queue: " + rider_name);
    });

  }

  // TODO: remove (for test purposes only)
  private static String getRandomName() {
    return NAMES[(int) Math.floor(Math.random() * NAMES.length)];
  }

  // TODO: remove (for test purposes only)
  private static String getRandomState() {
    return STATES[(int) Math.floor(Math.random() * STATES.length)];
  }


  // TODO: remove (for test purposes only)
  private JsonArray generateRandomQueueState() {
    final JsonArray json = new JsonArray();

    // create random representation of queue state
    int count = 5 + (int)Math.round(Math.random() * 5);
    for (int i = 0; i < count; i++) {
      String state = getRandomState();
      long eta = 0L;
      if ("IN_QUEUE".equals(state)) {
        eta = new Date().getTime() + Math.round(Math.random() * 10 * MS_PER_MIN);
      }
      json.add(new JsonObject()
        .put("name", getRandomName())
        .put("entered", new Date().getTime() - Math.round(Math.random() * 10 * MS_PER_MIN))
        .put("state", state)
        .put("eta", eta)
      );
    }
    return json;
  }
}


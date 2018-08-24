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
import java.util.HashMap;
import java.util.Map;

public class Server extends AbstractVerticle {

    // TODO: remove (for testing only)
    private static String[] NAMES = new String[]{"James", "Rodney", "Thomas", "Clement", "Dan", "Sameer"};
    private static String[] STATES = new String[]{"IN_QUEUE", "ON_RIDE", "COMPLETED_RIDE"};

    private Logger log = LoggerFactory.getLogger(Server.class.getName());

    // TODO: remove
    private static String getRandomName() {
        return NAMES[(int) Math.floor(Math.random() * NAMES.length)];
    }

    // TODO: remove
    private static String getRandomState() {
        return STATES[(int) Math.floor(Math.random() * STATES.length)];
    }

    // TODO: save config elsewhere?
    private Map<String, Object> config = new HashMap<String, Object>() {
      {
        put("averageRideTimeMinutes", 5);
      }
    };

    @Override
    public void start() throws Exception {
        // TODO: remove
        System.setProperty("vertx.disableFileCaching", "true");

        Router router = Router.router(vertx);

        router.route().handler(BodyHandler.create());

        // get the current config
        router.get("/api/config").handler(ctx -> {
          ctx.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/json");
          ctx.response().end(new JsonObject(config).encode());
        });

        router.get("/api/queue/all").handler(ctx -> {
            // TODO: remove (for testing only)
            final JsonArray json = new JsonArray();


            for (String name : NAMES) {
                String state = getRandomState();
                long eta = 0L;
                if ("IN_QUEUE".equals(state)) {
                    eta = new Date().getTime() + Math.round(Math.random() * 10 * 60 * 1000);
                }
                json.add(new JsonObject()
                        .put("name", name)
                        .put("entered", new Date().getTime() - Math.round(Math.random() * 10 * 60 * 1000))
                        .put("state", state)
                        .put("eta", eta)
                );
            }

            ctx.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            ctx.response().end(json.encode());
        });

        BridgeOptions opts = new BridgeOptions()
                .addOutboundPermitted(new PermittedOptions().setAddress("queue:updates"))
                .addInboundPermitted(new PermittedOptions().setAddress("control"));

        // Create the event bus bridge and add it to the router.
        SockJSHandler ebHandler = SockJSHandler.create(vertx).bridge(opts);
        router.route("/eb/*").handler(ebHandler);

        // TODO: re-enable cache (disabled for testing only)
        router.route().handler(StaticHandler.create().setCachingEnabled(false).setMaxAgeSeconds(0));

        vertx.createHttpServer().requestHandler(router::accept).listen(8080);

        EventBus eb = vertx.eventBus();

        // TODO: remove (just for testing purposes)
        vertx.setPeriodic(5000, t -> {
            // Create a timestamp string
            final JsonArray json = new JsonArray();

            for (String name : NAMES) {
                json.add(new JsonObject()
                        .put("name", getRandomName())
                        .put("entered", new Date().getTime() - Math.round(Math.random() * 10 * 60 * 1000))
                        .put("state", getRandomState())
                        .put("eta", new Date().getTime() + Math.round(Math.random() * 10 * 60 * 1000))
                );
            }
            eb.send("queue:updates", json);
        });

        MessageConsumer<JsonObject> consumer = eb.consumer("control");

      // TODO: complete reception of start/stop messages to start/stop ride
        consumer.handler(message -> {
          log.info("Received CONTROL message: " + message.body().toString());
          JsonObject controlObj = message.body();
          String cmd = controlObj.getString("cmd");

          switch (cmd) {
            case "start":
              // TODO
              log.info("Starting ride");
              break;
            case "stop":
              log.info("Stopping ride");
              break;
            case "config":
              JsonObject params = controlObj.getJsonObject("params");
              config = params.getMap();
              break;
            default:
              log.error("Unknown command: " + cmd);
          }
        });

    }
}


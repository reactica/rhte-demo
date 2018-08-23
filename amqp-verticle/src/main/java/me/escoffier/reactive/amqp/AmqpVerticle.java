package me.escoffier.reactive.amqp;

import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.proton.*;
import io.vertx.reactivex.core.AbstractVerticle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.ApplicationProperties;
import org.apache.qpid.proton.message.Message;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AmqpVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LogManager.getLogger("AMQP Verticle");

  @Override
  public void start(Future<Void> completion) {
    LOGGER.info("Starting AMQP verticle");
    AmqpConfiguration configuration = config().mapTo(AmqpConfiguration.class);
    ProtonClient client = ProtonClient.create(vertx.getDelegate());
    Future<ProtonConnection> future = Future.future();
    client.connect(configuration.getHost(), configuration.getPort(), configuration.getUser(), configuration.getUser(),
      connection -> {
        if (connection.succeeded()) {
          LOGGER.info("Connected to the AMQP broker {}", configuration.getHost());
          future.complete(connection.result());
        } else {
          LOGGER.info("Unable to connect to AMQP broker {}", configuration.getHost(), connection.cause());
          future.fail(connection.cause());
        }
      });


    future
      .compose(connection -> {
        LOGGER.info("Configuring connection...");
        connection.setContainer(configuration.getContainer());
        connection.open();
        return Future.succeededFuture(connection);
    })
    .compose(connection -> {
      LOGGER.info("Weaving event bus to AMQP connections");
      // Start listeners to send event bus messages to AMQP
      configuration.getEventBusToAmqp().forEach(bridge -> {
        ProtonSender sender = connection.createSender(bridge.getQueue());
        LOGGER.info("Event Bus {} => AMQP {}", bridge.getAddress(), bridge.getQueue());
        vertx.eventBus().<JsonObject>consumer(bridge.getAddress(), msg -> {
          Message message = Message.Factory.create();
          message.setAddress(bridge.getQueue());
          message.setBody(new AmqpValue(msg.body().encode()));
          Map<String, Object> properties = new HashMap<>();
          msg.headers().names().forEach(key -> properties.put(key, msg.headers().get(key)));
          message.setApplicationProperties(new ApplicationProperties(properties));
          sender.send(message);
        });
        sender.open();
      });

      return Future.succeededFuture(connection);
    })
    .compose(connection -> {
      LOGGER.info("Weaving AMQP to event bus connections");
      // Start consumers to send AMQP messages to the event bus (AMQP -> Event Bus
      List<AmqpToEventBus> bridges = configuration.getAmqpToEventBus();
      bridges.forEach(bridge -> {
        LOGGER.info("AMQP {} => Event Bus {}", bridge.getQueue(), bridge.getAddress());
        ProtonReceiver receiver = connection.createReceiver(bridge.getQueue());
        receiver.handler((delivery, request) -> {
          // Convert to headers.
          ApplicationProperties properties = request.getApplicationProperties();
          JsonObject json = new JsonObject((String) ((AmqpValue) request.getBody()).getValue());
          DeliveryOptions options = new DeliveryOptions();
          properties.getValue().forEach((key, value) -> options.addHeader(key, value.toString()));
          if (bridge.isPublish()) {
            vertx.eventBus().publish(bridge.getAddress(), json, options);
          } else {
            vertx.eventBus().send(bridge.getAddress(), json, options);
          }
        });
        receiver.open();
      });
      return Future.succeededFuture();
    })
    .setHandler(res -> completion.handle(res.mapEmpty()));
  }

}

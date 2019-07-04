package me.escoffier.reactive.amqp;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.amqp.AmqpClientOptions;
import io.vertx.amqp.AmqpReceiverOptions;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.amqp.*;
import io.vertx.reactivex.core.AbstractVerticle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class AmqpVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LogManager.getLogger("AMQP Verticle");

  @Override
  public Completable rxStart() {
    LOGGER.info("Starting AMQP verticle");
    AmqpConfiguration configuration = config().mapTo(AmqpConfiguration.class);
    AmqpClient amqp = AmqpClient.create(vertx, new AmqpClientOptions()
      .setHost(configuration.getHost())
      .setPort(configuration.getPort())
      .setUsername(configuration.getUser())
      .setPassword(configuration.getPassword())
      .setContainerId(configuration.getContainer())
    );
    return amqp.rxConnect()
      .doOnSuccess(x -> LOGGER.info("Connected to the AMQP broker {}", configuration.getHost()))
      .doOnError(err -> LOGGER.info("Unable to connect to AMQP broker {}", configuration.getHost(), err))
      .flatMapCompletable(connection -> {
        List<Single<?>> singles = new ArrayList<>();

        LOGGER.info("Weaving event bus to AMQP connections");
        // Start listeners to send event bus messages to AMQP
        configuration.getEventBusToAmqp().forEach(bridge -> singles.add(setupAmqpSender(connection, bridge)));
        LOGGER.info("Weaving AMQP to event bus connections");
        // Start consumers to send AMQP messages to the event bus (AMQP -> Event Bus
        configuration.getAmqpToEventBus().forEach(bridge -> singles.add(setupAmqpReceiver(connection, bridge)));
        return Single.merge(singles).ignoreElements();
      });
  }

  private Single<AmqpReceiver> setupAmqpReceiver(AmqpConnection connection, AmqpToEventBus bridge) {
    LOGGER.info("AMQP {} => Event Bus {}", bridge.getQueue(), bridge.getAddress());
    return connection.rxCreateReceiver(bridge.getQueue(), new AmqpReceiverOptions().setDurable(true),
      message -> {
        // Convert to headers.
        JsonObject properties = message.applicationProperties();
        JsonObject body = message.bodyAsJsonObject();
        DeliveryOptions options = new DeliveryOptions();
        properties.forEach(entry -> options.addHeader(entry.getKey(), entry.getValue().toString()));
        if (bridge.isPublish()) {
          vertx.eventBus().publish(bridge.getAddress(), body, options);
        } else {
          vertx.eventBus().send(bridge.getAddress(), body, options);
        }
      });
  }

  private Single<AmqpSender> setupAmqpSender(AmqpConnection connection, EventBusToAmqp bridge) {
    return connection.rxCreateSender(bridge.getQueue())
      .doOnSuccess(sender -> {
        LOGGER.info("Event Bus {} => AMQP {}", bridge.getAddress(), bridge.getQueue());
        vertx.eventBus().<JsonObject>consumer(bridge.getAddress(), msg -> {
          AmqpMessageBuilder message = AmqpMessageBuilder.create().durable(true);
          JsonObject headers = new JsonObject();
          msg.headers().names().forEach(key -> headers.put(key, msg.headers().get(key)));
          message.address(bridge.getQueue()).withJsonObjectAsBody(msg.body()).applicationProperties(headers);
          sender.send(message.build());
        });
      });
  }

}

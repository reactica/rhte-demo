package com.redhat.coderland.reactica.eventstore;

import io.vertx.core.DeploymentOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import me.escoffier.reactive.amqp.AmqpConfiguration;
import me.escoffier.reactive.amqp.AmqpToEventBus;
import me.escoffier.reactive.amqp.AmqpVerticle;
import me.escoffier.reactive.amqp.EventBusToAmqp;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;

public class MainVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LogManager.getLogger("MainVerticle");

  @Override
  public void start() {

    LOGGER.info("Starting deployment of Main Verticle");
    AmqpToEventBus amqpToEventBus = new AmqpToEventBus();
    amqpToEventBus.setAddress("user-events");
    amqpToEventBus.setQueue("USER_QUEUE");

    EventBusToAmqp eventBusToAmqp = new EventBusToAmqp();
    eventBusToAmqp.setAddress("user-events");
    eventBusToAmqp.setQueue("USER_QUEUE");

    AmqpConfiguration configuration = new AmqpConfiguration()
      .setContainer("amqp-examples")
      .setHost("eventstream-amq-amqp")
      .setPort(5672)
      .setUser("user")
      .setPassword("user123")
      .setAmqpToEventBus(Collections.singletonList(amqpToEventBus))
      .setEventBusToAmqp(Collections.singletonList(eventBusToAmqp));

    LOGGER.info("Deploying Other Verticles");
    vertx.rxDeployVerticle(AmqpVerticle.class.getName(), new DeploymentOptions().setConfig(JsonObject.mapFrom(configuration)))
      .flatMap(x -> vertx.rxDeployVerticle(UserEventReceiverVerticle.class.getName()))
      //.flatMap(x -> vertx.rxDeployVerticle(MessageGenerator.class.getName()))
      .subscribe();
  }

}

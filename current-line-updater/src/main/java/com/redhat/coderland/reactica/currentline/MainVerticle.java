package com.redhat.coderland.reactica.currentline;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
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
//    AmqpToEventBus amqpToEventBus = new AmqpToEventBus();
//    amqpToEventBus.setAddress("current-line-events");
//    amqpToEventBus.setQueue("CL_QUEUE");

    EventBusToAmqp eventBusToAmqp = new EventBusToAmqp();
    eventBusToAmqp.setAddress("current-line-events");
    eventBusToAmqp.setQueue("CL_QUEUE");

    AmqpConfiguration configuration = new AmqpConfiguration()
      .setContainer("amqp-examples")
      .setHost("eventstream-amq-amqp")
      .setPort(5672)
      .setUser("user")
      .setPassword("user123")
//      .setAmqpToEventBus(Collections.singletonList(amqpToEventBus))
      .setEventBusToAmqp(Collections.singletonList(eventBusToAmqp));

    //For localhost
    JsonObject datagridConfigLocalhost = new JsonObject()
      .put("host","localhost")
      .put("port",11222);

    //For OpenShift
    JsonObject datagridConfigOpenshift = new JsonObject()
      .put("host","eventstore-dg-hotrod")
      .put("port",11333);


    LOGGER.info("Deploying Other Verticles");

    //For localhost
//    vertx.rxDeployVerticle(CurrentLineUpdaterVerticle.class.getName(),new DeploymentOptions().setConfig(datagridConfigLocalhost)).subscribe();


    //For openshift
    vertx.rxDeployVerticle(AmqpVerticle.class.getName(), new DeploymentOptions().setConfig(JsonObject.mapFrom(configuration)))
      .flatMap(x -> vertx.rxDeployVerticle(CurrentLineUpdaterVerticle.class.getName(),new DeploymentOptions().setConfig(datagridConfigOpenshift)))
      .subscribe();
  }

}

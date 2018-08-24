package example;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import me.escoffier.reactive.amqp.AmqpConfiguration;
import me.escoffier.reactive.amqp.AmqpToEventBus;
import me.escoffier.reactive.amqp.AmqpVerticle;
import me.escoffier.reactive.amqp.EventBusToAmqp;

import java.util.Collections;

public class MainVerticle extends AbstractVerticle {

  @Override
  public void start() {
    AmqpToEventBus amqpToEventBus = new AmqpToEventBus();
    amqpToEventBus.setAddress("results");
    amqpToEventBus.setQueue("ENTER_EVENT_QUEUE");

    EventBusToAmqp eventBusToAmqp = new EventBusToAmqp();
    eventBusToAmqp.setAddress("messages");
    eventBusToAmqp.setQueue("ENTER_EVENT_QUEUE");

    AmqpConfiguration configuration = new AmqpConfiguration()
      .setContainer("amqp-examples")
      .setHost("eventstream-amq-amqp")
      .setPort(5672)
      .setUser("user")
      .setPassword("user123")
      .setAmqpToEventBus(Collections.singletonList(amqpToEventBus))
      .setEventBusToAmqp(Collections.singletonList(eventBusToAmqp));

    vertx.rxDeployVerticle(AmqpVerticle.class.getName(), new DeploymentOptions().setConfig(JsonObject.mapFrom(configuration)))
    .flatMap(x -> vertx.rxDeployVerticle(SenderVerticle.class.getName()))
    .flatMap(x -> vertx.rxDeployVerticle(ReceiverVerticle.class.getName()))
    .flatMap(x -> vertx.rxDeployVerticle(VerticleUsingCache.class.getName()))
      .subscribe();

  }
}

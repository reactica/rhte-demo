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
  public void start() throws Exception {
    AmqpToEventBus amqpToEventBus = new AmqpToEventBus();
    amqpToEventBus.setAddress("results");
    amqpToEventBus.setQueue("reactica/responses");

    EventBusToAmqp eventBusToAmqp = new EventBusToAmqp();
    eventBusToAmqp.setAddress("messages");
    eventBusToAmqp.setQueue("reactica/responses");

    AmqpConfiguration configuration = new AmqpConfiguration();
    configuration.setContainer("amqp-examples");
    configuration.setHost("reactica-broker-amq-amqp");
    configuration.setAmqpToEventBus(Collections.singletonList(amqpToEventBus));
    configuration.setEventBusToAmqp(Collections.singletonList(eventBusToAmqp));

    vertx.rxDeployVerticle(AmqpVerticle.class.getName(), new DeploymentOptions().setConfig(JsonObject.mapFrom(configuration)))
    .flatMap(x -> vertx.rxDeployVerticle(SenderVerticle.class.getName()))
    .flatMap(x -> vertx.rxDeployVerticle(ReceiverVerticle.class.getName()))
      .subscribe();

  }
}

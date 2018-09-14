package com.redhat.coderland.reactica.qlc;

import io.reactivex.Completable;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.CompletableHelper;
import io.vertx.reactivex.config.ConfigRetriever;
import io.vertx.reactivex.core.AbstractVerticle;
import me.escoffier.reactive.amqp.AmqpConfiguration;
import me.escoffier.reactive.amqp.AmqpVerticle;
import me.escoffier.reactive.amqp.EventBusToAmqp;

public class MainVerticle extends AbstractVerticle {


  @Override
  public void start(Future<Void> done) throws Exception {
    ConfigRetriever retriever = ConfigRetriever.create(vertx);
    deployAMQPVerticle()
      .andThen(deployQueueLengthCalculator(retriever))
      .subscribe(CompletableHelper.toObserver(done));
  }


  private Completable deployQueueLengthCalculator(ConfigRetriever retriever) {
    return retriever.rxGetConfig()
      .flatMapCompletable(json -> {
        JsonObject user = json.getJsonObject("user-simulator");
        JsonObject ride = json.getJsonObject("ride-simulator");

        retriever.listen(change -> {
          JsonObject configuration = change.getNewConfiguration();
          vertx.eventBus().publish("configuration", configuration);
        });

        return vertx.rxDeployVerticle(QueueLengthCalculator.class.getName(), new DeploymentOptions().setConfig(user)).ignoreElement();
      });
  }


  private Completable deployAMQPVerticle() {
    EventBusToAmqp user_queue = new EventBusToAmqp();
    user_queue.setAddress("to-qlc-queue");
    user_queue.setQueue("QLC_QUEUE");


    AmqpConfiguration configuration = new AmqpConfiguration()
      .setContainer("amqp-examples")
      .setHost("eventstream-amq-amqp")
      .setPort(5672)
      .setUser("user")
      .setPassword("user123")
      .addEventBusToAmqp(user_queue);

    return vertx.rxDeployVerticle(AmqpVerticle.class.getName(), new DeploymentOptions().setConfig(JsonObject.mapFrom(configuration))).ignoreElement();
  }
}

package com.redhat.coderland.reactica;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.Future;
import io.vertx.reactivex.core.AbstractVerticle;
import me.escoffier.reactive.rhdg.AsyncCache;
import me.escoffier.reactive.rhdg.DataGridClient;
import me.escoffier.reactive.rhdg.DataGridConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Random;

/**
 * Creates users and put them in the queue.
 * Generation pace is "random" but around 10 seconds
 */
public class UserSimulatorVerticle extends AbstractVerticle  {

  private Random random = new Random();

  private static final Logger LOGGER = LogManager.getLogger(UserSimulatorVerticle.class);

  /**
   * Cache User name -> Json encoded user
   */
  private AsyncCache<String, String> cache;

  @Override
  public void start(Future<Void> done) {
    Single<DataGridClient> single = DataGridClient.create(vertx, new DataGridConfiguration()
      .setHost("eventstore-dg-hotrod")
      .setPort(11333));

    single.flatMap(client -> client.<String, String>getCache("users"))
      .flatMap(cache -> cache.clear().andThen(Single.just(cache)))
      .subscribe(
        cache -> {
          this.cache = cache;
          LOGGER.info("User Simulator initialized");
          enqueueUserCreation();
          done.complete(null);
        },
        err -> {
          LOGGER.error("Unable to initialize the User Simulator");
          done.fail(err);
        }
      );
  }

  private void enqueueUserCreation() {
    int delay = random.nextInt(10000) + ((random.nextBoolean() ? -1 : 1 ) * random.nextInt(3000));
    if (delay < 100) {
      // Avoid big burst
      delay = 5000;
    }
    vertx.setTimer(delay, x -> {
      User user = new User();
      LOGGER.info("Creating user {}", user.getName());
      cache.put(user.getName(), user.asJson())
        .andThen(addUserToQueue(user))
        .doOnComplete(this::enqueueUserCreation)
        .subscribe();
    });
  }

  private Completable addUserToQueue(User user) {
    return cache.put(user.getName(), user.putInQueue().asJson())
      .doOnComplete(() -> vertx.eventBus().send(Events.USER_EVENTS, Events.create(Events.USER_IN_QUEUE, user)));
  }

}

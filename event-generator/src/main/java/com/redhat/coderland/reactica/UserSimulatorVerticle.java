package com.redhat.coderland.reactica;

import com.redhat.coderland.reactica.model.User;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import me.escoffier.reactive.rhdg.AsyncCache;
import me.escoffier.reactive.rhdg.DataGridClient;
import me.escoffier.reactive.rhdg.DataGridConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Random;

/**
 * Creates users and put them in the queue.
 *
 * Generation pace is "random" but around {@code DEFAULT_PERIOD} +/- {@code DEFAULT_JITTER} seconds.
 */
public class UserSimulatorVerticle extends AbstractVerticle  {

  private static final int DEFAULT_PERIOD = 20;
  private static final int DEFAULT_JITTER = 10;

  private Random random = new Random();

  private static final Logger LOGGER = LogManager.getLogger(UserSimulatorVerticle.class);

  /**
   * Cache User name -> Json encoded user
   */
  private AsyncCache<String, String> cache;

  private long period;
  private int jitter;
  private boolean enabled;

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
          configure(config());
          vertx.eventBus().<JsonObject>consumer("configuration", m -> configure(m.body().getJsonObject("user-simulator")));
          vertx.eventBus().<Boolean>consumer("user-simulator-toggle", b -> toggle(b.body()));
          done.complete(null);
        },
        err -> {
          LOGGER.error("Unable to initialize the User Simulator");
          done.fail(err);
        }
      );
  }

  private void toggle(boolean enabled) {
    boolean isCurrentlyEnabled = this.enabled;
    this.enabled = enabled;
    if (! isCurrentlyEnabled  && enabled) {
      LOGGER.info("Restarting user simulator");
      // Restart generation
      enqueueUserCreation();
    }
    if (isCurrentlyEnabled && !enabled) {
      LOGGER.info("Stopping user simulator");
    }
  }

  private void configure(JsonObject json) {
    if (json == null) {
      return;
    }
    LOGGER.info("Configuring the user simulator");
    period = json.getInteger("period-in-seconds", DEFAULT_PERIOD);
    jitter = json.getInteger("jitter-in-seconds", DEFAULT_JITTER);
    boolean isCurrentlyEnabled = enabled;
    enabled = json.getBoolean("enabled", false);
    LOGGER.info("User simulator configured with: period={} jitter={} enabled={}", period, jitter, enabled);

    if (! isCurrentlyEnabled  && enabled) {
      LOGGER.info("Restarting user generation");
      // Restart generation
      enqueueUserCreation();
    }
  }

  private void enqueueUserCreation() {
    if (! enabled) {
      return;
    }

    long delay = getDelayInMilliSeconds();
    vertx.setTimer(delay, x -> {
      if (! enabled) {
        return;
      }

      String name = CuteNameService.generate();
      User user = new User(name, name);
      LOGGER.info("Creating user {}", user.getName());
      cache.put(user.getName(), JsonObject.mapFrom(user).encode())
        .andThen(addUserToQueue(user))
        .doOnComplete(this::enqueueUserCreation)
        .subscribe();
    });
  }

  private long getDelayInMilliSeconds() {
    long delay = period + ((random.nextBoolean() ? -1 : 1 ) * random.nextInt(jitter));
    if (delay < 1) {
      // Avoid big burst
      delay = 5;
    }
    return delay * 1000; // Don't forget to convert to millis
  }

  private Completable addUserToQueue(User user) {
    return cache.put(user.getName(), JsonObject.mapFrom(user.putInQueue()).encode())
      .doOnComplete(() -> vertx.eventBus().send(Events.USER_EVENTS, Events.create(Events.USER_IN_QUEUE, user)));
  }

}

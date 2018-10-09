package com.redhat.coderland.reactica;

import com.redhat.coderland.reactica.model.Ride;
import com.redhat.coderland.reactica.model.User;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import me.escoffier.reactive.rhdg.AsyncCache;
import me.escoffier.reactive.rhdg.DataGridClient;
import me.escoffier.reactive.rhdg.DataGridConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Simulates ride.
 * <p>
 * Each ride takes {@code DEFAULT_RIDE_DURATION} + ~@{code DEFAULT_JITTER_DURATION} seconds.
 * Each ride takes at most @{code DEFAULT_USER_ON_RIDE} users from the queue (in order).
 */
public class RideSimulator extends AbstractVerticle {
  private static final Logger LOGGER = LogManager.getLogger(RideSimulator.class);

  private AsyncCache<String, String> cache;
  private Random random = new Random();

  private long duration;
  private int jitter;
  private int numberOfUsers;
  private boolean enabled;


  @Override
  public void start(Future<Void> done) {
    Single<DataGridClient> single = DataGridClient.create(vertx, new DataGridConfiguration()
      .setHost("eventstore-dg-hotrod")
      .setPort(11333));

    single.flatMap(client -> client.<String, String>getCache("users"))
      .subscribe(
        cache -> {
          this.cache = cache;
          configure(config());
          LOGGER.info("Ride Simulator initialized");
          vertx.eventBus().<JsonObject>consumer("configuration", m -> configure(m.body().getJsonObject("ride-simulator")));
          vertx.eventBus().<Boolean>consumer("ride-simulator-toggle", b -> toggle(b.body()));
          done.complete(null);
        },
        err -> {
          LOGGER.error("Unable to initialize the Ride Simulator");
          done.fail(err);
        }
      );

    vertx.setPeriodic(60000, x -> cleanup());
  }

  private void toggle(boolean enabled) {
    boolean isCurrentlyEnabled = this.enabled;
    this.enabled = enabled;
    if (! isCurrentlyEnabled  && enabled) {
      LOGGER.info("Restarting ride");
      // Restart generation
      enqueueRide();
    }
    if (isCurrentlyEnabled && !enabled) {
      LOGGER.info("Stopping ride");
    }
  }

  private void configure(JsonObject json) {
    if (json == null) {
      return;
    }
    LOGGER.info("Configuring the ride simulator");
    duration = json.getLong("duration-in-seconds", Ride.DEFAULT_RIDE_DURATION);
    jitter = json.getInteger("jitter-in-seconds", Ride.DEFAULT_JITTER_DURATION);
    numberOfUsers = json.getInteger("users-per-ride", Ride.DEFAULT_USER_ON_RIDE);

    boolean isCurrentlyEnabled = enabled;
    enabled = json.getBoolean("enabled", true);
    LOGGER.info("Ride simulator configured with: duration={} jitter={} users={} enabled={}", duration, jitter, numberOfUsers, enabled);

    if (! isCurrentlyEnabled  && enabled) {
      LOGGER.info("Restarting ride");
      // Restart generation
      enqueueRide();
    }
  }

  private void enqueueRide() {
    if (! enabled) {
      return;
    }

    Ride ride = new Ride().setState(Ride.STATE_PLANNED);

    LOGGER.info("Onboarding ride {}", JsonObject.mapFrom(ride).encode());
    getUsers()
      .subscribe(list -> {
        LOGGER.info("Have been selected for ride {} : {}", ride.getUuid(), list.stream().map(User::getName).collect(Collectors.toList()));
        // Onboarding -  First put the users on ride, and emit event for each user
        Flowable.fromIterable(list)
          .flatMapCompletable(u -> cache.put(u.getName(), JsonObject.mapFrom(u.onRide()).encode()).doOnComplete(() -> sendOnRideEvent(u, ride)))
          .doOnComplete(() -> LOGGER.info("The users {} have been put on the ride", list.stream().map(User::getName).collect(Collectors.toList())))
          .subscribe(
            () -> {
              // Start the ride
              // Send an event about the ride that is starting
              sendRideStartedEvent(ride.setState(Ride.STATE_IN_PROGRESS));
              LOGGER.info("Ride {} is now in progress", ride.getUuid());

              // Schedule the termination
              vertx.setTimer(getRideDurationInMilliseconds(), x -> {
                // Ride completed, send event
                ride.setState(Ride.STATE_COMPLETED);
                sendRideCompletedEvent(ride);
                LOGGER.info("Ride {} completed", ride.getUuid());
                // Update the users and remove them from the cache
                Flowable.fromIterable(list)
                  .map(User::completed)
                  .doOnNext(u -> sendUserCompletedEvent(u, ride))
                  .flatMapCompletable(u -> cache.put(u.getName(), JsonObject.mapFrom(u.completed()).encode()))
                  .doOnComplete(() -> LOGGER.info("The users {} have completed their ride, bye bye!",
                    list.stream().map(User::getName).collect(Collectors.toList())))
                  .subscribe(
                    this::enqueueRide, // Plan next ride
                    err -> LOGGER.error("Unable to complete a ride", err)
                  );
              });
            },
            err -> LOGGER.error("Unable to start a ride", err)
          );
      });
  }

  private void cleanup() {
    long now = Instant.now().toEpochMilli() / 1000;
    long max = 1 * 60;
    cache.all().map(Map::values)
      .subscribe(all ->
        all.stream()
          .map(s -> Json.decodeValue(s, User.class))
          .filter(u -> u.getCurrentState().equalsIgnoreCase(User.STATE_RIDE_COMPLETED))
          .filter(u -> u.getEnterQueueTime() < now - max)
          .forEach(u -> {
            LOGGER.info("Removing {} from cache - ride completed and entered the queue {} minutes ago", u.getName(), (now - u.getEnterQueueTime()) / 60.0);
            cache.remove(u.getName()).subscribe();
          })
      );
  }

  private void sendUserCompletedEvent(User user, Ride ride) {
    vertx.eventBus().send(Events.USER_EVENTS,
      Events.create(Events.USER_COMPLETED, user, ride));
  }

  private void sendRideStartedEvent(Ride ride) {
    vertx.eventBus().send(Events.RIDE_EVENTS, Events.create(Events.RIDE_STARTED, ride));
  }

  private void sendRideCompletedEvent(Ride ride) {
    vertx.eventBus().send(Events.RIDE_EVENTS, Events.create(Events.RIDE_COMPLETED, ride));
  }

  private void sendOnRideEvent(User user, Ride ride) {
    vertx.eventBus().send(Events.USER_EVENTS,
      Events.create(Events.USER_ON_RIDE, user, ride));
  }

  private long getRideDurationInMilliseconds() {
    long l = duration + random.nextInt(jitter);
    if (l < 1) {
      l = duration;
    }
    return l * 1000;
  }

  private Single<List<User>> getUsers() {
    return cache.all().map(all -> all.values()
      .stream()
      .map(s -> Json.decodeValue(s, User.class))
      .filter(user -> user.getCurrentState().equalsIgnoreCase(User.STATE_IN_QUEUE))
      .sorted(Comparator.comparing(User::getEnterQueueTime))
      .collect(Collectors.toList())
    )
      .map(sorted ->
        sorted.stream()
          .limit(numberOfUsers)
          .collect(Collectors.toList())
      );
  }


}

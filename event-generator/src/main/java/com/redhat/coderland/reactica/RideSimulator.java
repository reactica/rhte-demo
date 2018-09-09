package com.redhat.coderland.reactica;

import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vertx.core.Future;
import io.vertx.reactivex.core.AbstractVerticle;
import me.escoffier.reactive.rhdg.AsyncCache;
import me.escoffier.reactive.rhdg.DataGridClient;
import me.escoffier.reactive.rhdg.DataGridConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Simulates ride.
 * <p>
 * Each ride takes 1 minutes + ~10 seconds.
 * Each ride takes at most 10 users from the queue (in order)
 */
public class RideSimulator extends AbstractVerticle {


  private AsyncCache<String, String> cache;
  private Random random = new Random();
  private static final Logger LOGGER = LogManager.getLogger(UserSimulatorVerticle.class);

  @Override
  public void start(Future<Void> done) {
    Single<DataGridClient> single = DataGridClient.create(vertx, new DataGridConfiguration()
      .setHost("eventstore-dg-hotrod")
      .setPort(11333));

    single.flatMap(client -> client.<String, String>getCache("users"))
      .subscribe(
        cache -> {
          this.cache = cache;
          LOGGER.info("Ride Simulator initialized");
          enqueueRide();
          done.complete(null);
        },
        err -> {
          LOGGER.error("Unable to initialize the Ride Simulator");
          done.fail(err);
        }
      );
  }

  private void enqueueRide() {
    Ride ride = new Ride().setState(Ride.State.PLANNED);

    LOGGER.info("Onboarding ride {}", ride.asJson());
    getUsers()
      .subscribe(list -> {
        LOGGER.info("Have been selected for ride {} : {}", ride.getUuid(), list.stream().map(User::getName).collect(Collectors.toList()));
        // Onboarding -  First put the users on ride, and emit event for each user
        Flowable.fromIterable(list)
          .flatMapCompletable(u -> cache.put(u.getName(), u.onRide().asJson()).doOnComplete(() -> sendOnRideEvent(u, ride)))
          .doOnComplete(() -> LOGGER.info("The users {} have been put on the ride", list.stream().map(User::getName).collect(Collectors.toList())))
          .subscribe(
            () -> {
              // Start the ride
              // Send an event about the ride that is starting
              sendRideStartedEvent(ride.setState(Ride.State.IN_PROGRESS));
              LOGGER.info("Ride {} is now in progress", ride.getUuid());

              // Schedule the termination
              vertx.setTimer(getRideDuration(), x -> {
                // Ride completed, send event
                sendRideCompletedEvent(ride);
                LOGGER.info("Ride {} completed", ride.getUuid());
                // Update the users and remove them from the cache
                Flowable.fromIterable(list)
                  .map(User::completed)
                  .doOnNext(u -> sendUserCompletedEvent(u, ride))
                  .flatMapCompletable(u -> cache.remove(u.getName()))
                  .doOnComplete(() -> {
                    LOGGER.info("The users {} have completed their ride, bye bye!", list.stream().map(User::getName).collect(Collectors.toList()));
                  })
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

  private int getRideDuration() {
    return 60000 + random.nextInt(10000);
  }

  private Single<List<User>> getUsers() {
    return cache.all().map(Map::values)
      .map(all ->
        all.stream()
          .map(User::fromJson)
          .filter(user -> user.getState() == User.State.IN_QUEUE)
          .sorted((u1, u2) -> Long.compare(u2.getEnteredQueueAt(), u1.getCompletedRideAt()))
          .limit(10)
          .collect(Collectors.toList()));
  }


}

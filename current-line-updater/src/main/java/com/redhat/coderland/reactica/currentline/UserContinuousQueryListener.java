package com.redhat.coderland.reactica.currentline;

import com.redhat.coderland.reactica.model.User;
import io.vertx.core.json.JsonArray;
import io.vertx.reactivex.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.infinispan.query.api.continuous.ContinuousQueryListener;

import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class UserContinuousQueryListener implements ContinuousQueryListener<String, User> {

  private static final Logger LOGGER = LogManager.getLogger("UserContinuousQueryListener");
  private static final String CURRENT_LINE_EVENTS = "current-line-events";

  private final Vertx vertx;

  private Map<String, User> current = new ConcurrentHashMap<>();

  public UserContinuousQueryListener(Vertx vertx) {
    this.vertx = vertx;
  }

  private JsonObject generateOutput() {
    long currentTime = Instant.now().toEpochMilli() / 1000;
    JsonObject json = new JsonObject();
    Stream<User> inQueueOrOnRide = current.values().stream()
      .filter(user -> {
        if (user.getCurrentState().equals(User.STATE_IN_QUEUE) || user.getCurrentState().equals(User.STATE_ON_RIDE)) {
          return true;
        } else {
          return false;
        }
      });
    Stream<User> completedSorted = current.values().stream()
      .filter(user -> user.getCurrentState().equals(User.STATE_RIDE_COMPLETED))
      .sorted((j1,j2) -> new Long(j2.getCompletedRideTime()).compareTo(j1.getCompletedRideTime()));

    Stream<User> userStream = Stream.concat(inQueueOrOnRide, completedSorted.limit(10));

    JsonArray queue = userStream
      .map(JsonObject::mapFrom).collect(JsonArray::new, JsonArray::add, JsonArray::addAll);

    return json.put("queue", queue);
  }

  private void fire() {
    JsonObject users = generateOutput();
    LOGGER.info("Send the following list of users to the eventBus at time( " + (Instant.now().toEpochMilli()/1000) + ") : " + users.encodePrettily());
    vertx.eventBus().send(CURRENT_LINE_EVENTS, users);
  }

  @Override
  public void resultJoining(String key, User value) {
    LOGGER.info("Received an new value for user with key " + key + " and value " + value);
    current.put(key, value);
    fire();
  }

  @Override
  public void resultUpdated(String key, User value) {
    LOGGER.info("Received an updated value for user with key " + key + " and value " + value);
    current.put(key, value);
    fire();
  }

  @Override
  public void resultLeaving(String key) {
    LOGGER.info("Received an removed value for user with key " + key);
    current.remove(key);
    fire();
  }
}

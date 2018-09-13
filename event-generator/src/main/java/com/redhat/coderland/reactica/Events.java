package com.redhat.coderland.reactica;

import com.redhat.coderland.reactica.model.Ride;
import com.redhat.coderland.reactica.model.User;
import io.vertx.core.json.JsonObject;

public class Events {

  public static String USER_EVENTS = "user-events";
  public static String RIDE_EVENTS = "ride-events";

  // Type of events

  /**
   * User added on the queue.
   */
  public static String USER_IN_QUEUE = "user-in-queue";

  /**
   * User on ride.
   */
  public static String USER_ON_RIDE = "user-on-ride";

  /**
   * User leaving the ride (ride completed)
   */
  public static String USER_COMPLETED = "user-ride-completed";

  /**
   * A ride just started.
   */
  public static String RIDE_STARTED = "ride-started";

  /**
   * A ride just completed.
   */
  public static String RIDE_COMPLETED = "ride-completed";

  public static JsonObject create(String event, User user, Ride ride) {
    JsonObject json = new JsonObject()
      .put("event", event);
    if (user != null) {
      json.put("user", JsonObject.mapFrom(user));
    }
    if (ride != null) {
      json.put("ride", JsonObject.mapFrom(ride));
    }
    return json;
  }

  public static JsonObject create(String event, Ride ride) {
    return create(event, null, ride);
  }

  public static JsonObject create(String event, User user) {
    return create(event, user, null);
  }

}

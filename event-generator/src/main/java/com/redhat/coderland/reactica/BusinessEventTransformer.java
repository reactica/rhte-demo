package com.redhat.coderland.reactica;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;

/**
 * I always wanted to name a class like that.
 *
 * Get internal event, produce business events and forward them to an event bus address listened by the AMQP bridge.
 *
 */
public class BusinessEventTransformer extends AbstractVerticle  {


  @Override
  public void start() {

    vertx.eventBus()
      .<JsonObject>consumer(Events.USER_EVENTS)
      .handler(message -> onUserEvent(message.body()));

    vertx.eventBus()
      .<JsonObject>consumer(Events.RIDE_EVENTS)
      .handler(message -> onRideEvent(message.body()));
  }

  private void onRideEvent(JsonObject event) {
    Ride ride = Ride.fromJson(event.getJsonObject("ride"));

    JsonObject business = new JsonObject()
      .put("uuid", ride.getUuid())
      .put("state", ride.getState().name())
      .put("name", ride.getUuid()); // TODO do we need a name for the ride?

    vertx.eventBus().send("to-ride-event-queue", business);
  }

  private void onUserEvent(JsonObject event) {
    User user = User.fromJson(event.getJsonObject("user"));

    Ride ride = null;
    if (event.getJsonObject("ride") != null) {
      ride = Ride.fromJson(event.getJsonObject("ride"));
    }

    JsonObject business = new JsonObject()
      .put("id", user.getName())
      .put("name", user.getName())
      .put("currentState", user.getState().name())
      .put("enterTime", user.getEnteredQueueAt());

    if (ride != null) {
      business.put("rideId", ride.getUuid());
    }

    System.out.println("Forwarding event to 'to-user-queue' and 'to-enter-event-queue' " + business.encode());
    vertx.eventBus().send("to-user-queue", business);
    vertx.eventBus().send("to-enter-event-queue", business);
  }


}

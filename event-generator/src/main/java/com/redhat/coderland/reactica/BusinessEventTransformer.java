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

    JsonObject business = new JsonObject()
      .put("id", user.getName()) // TODO Do we need a uuid?
      .put("name", user.getName())
      .put("current_state", user.getState().name())
      .put("enter_time", user.getEnteredQueueAt());

    System.out.println("Forwarding event to 'to-user-queue' and 'to-enter-event-queue' " + business.encode());
    vertx.eventBus().send("to-user-queue", business);
    vertx.eventBus().send("to-enter-event-queue", business);
  }


}

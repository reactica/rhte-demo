package com.redhat.coderland.reactica;

import com.redhat.coderland.reactica.model.Ride;
import com.redhat.coderland.reactica.model.User;
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
    Ride ride = event.getJsonObject("ride").mapTo(Ride.class);

    JsonObject business = new JsonObject()
      .put("uuid", ride.getUuid())
      .put("state", ride.getState())
      .put("name", User.RIDE_ID);

    vertx.eventBus().send("to-ride-event-queue", business);
  }

  private void onUserEvent(JsonObject event) {
    User user = event.getJsonObject("user").mapTo(User.class);

    JsonObject business = new JsonObject()
      .put("id", user.getName())
      .put("name", user.getName())
      .put("rideId", user.getRideId())
      .put("currentState", user.getCurrentState())
      .put("enterQueueTime", user.getEnterQueueTime())
      .put("completedRideTime", user.getCompletedRideTime());

    vertx.eventBus().send("to-user-queue", business);
    vertx.eventBus().send("to-enter-event-queue", business);
  }


}

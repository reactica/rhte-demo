package com.redhat.coderland.reactica;

import io.vertx.core.json.JsonObject;

public class User {

  private String name = CuteNameService.generate();

  private State state = State.UNKNOWN;

  private long enteredQueueAt = 0L;

  private long startedRideAt = 0L;

  private long completedRideAt = 0L;

  public static User fromJson(String json) {
    return fromJson(new JsonObject(json));
  }

  public enum State {
    UNKNOWN,
    IN_QUEUE,
    ON_RIDE,
    COMPLETED_RIDE
  }

  public String getName() {
    return name;
  }

  private User setName(String name) {
    this.name = name;
    return this;
  }

  public State getState() {
    return state;
  }

  private User setState(State state) {
    this.state = state;
    return this;
  }

  public long getEnteredQueueAt() {
    return enteredQueueAt;
  }

  private User setEnteredQueueAt(long enteredQueueAt) {
    this.enteredQueueAt = enteredQueueAt;
    return this;
  }

  public long getStartedRideAt() {
    return startedRideAt;
  }

  private User setStartedRideAt(long startedRideAt) {
    this.startedRideAt = startedRideAt;
    return this;
  }

  public long getCompletedRideAt() {
    return completedRideAt;
  }

  private User setCompletedRideAt(long completedRideAt) {
    this.completedRideAt = completedRideAt;
    return this;
  }

  public JsonObject toJson() {
    return new JsonObject()
      .put("kind", User.class.getName())
      .put("name", name).put("state", state.name())
      .put("entered-queue-at", enteredQueueAt)
      .put("started-ride-at", startedRideAt)
      .put("completed-ride-at", completedRideAt);
  }

  public String asJson() {
    return toJson().encode();
  }

  public static User fromJson(JsonObject json) {
    return new User().setName(json.getString("name"))
      .setState(State.valueOf(json.getString("state", State.UNKNOWN.name())))
      .setEnteredQueueAt(json.getLong("entered-queue-at", 0L))
      .setStartedRideAt(json.getLong("started-ride-at", 0L))
      .setCompletedRideAt(json.getLong("completed-ride-at", 0L));
  }

  public User putInQueue() {
    setState(State.IN_QUEUE)
      .setEnteredQueueAt(System.currentTimeMillis());
    return this;
  }

  public User onRide() {
    setState(State.ON_RIDE)
      .setStartedRideAt(System.currentTimeMillis());
    return this;
  }

  public User completed() {
    setState(State.COMPLETED_RIDE)
      .setCompletedRideAt(System.currentTimeMillis());
    return this;
  }

}

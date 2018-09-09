package com.redhat.coderland.reactica;

import io.vertx.core.json.JsonObject;

import java.util.UUID;

public class Ride {

  public enum State {
    IN_PROGRESS,
    COMPLETED,
    PLANNED,
    UNKNOWN
  }

  private String uuid;

  private State state;

  public Ride() {
    this.uuid = UUID.randomUUID().toString();
  }

  public String getUuid() {
    return uuid;
  }

  public Ride setUuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  public State getState() {
    return state;
  }

  public Ride setState(State state) {
    this.state = state;
    return this;
  }

  public JsonObject toJson() {
    return new JsonObject()
      .put("kind", Ride.class.getName())
      .put("uuid", uuid).put("state", state.name());
  }

  public String asJson() {
    return toJson().encode();
  }

  public static Ride fromJson(JsonObject json) {
    return new Ride()
      .setState(State.valueOf(json.getString("state", State.UNKNOWN.name())))
      .setUuid(json.getString("uuid"));
  }

}

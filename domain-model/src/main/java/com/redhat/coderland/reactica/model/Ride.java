package com.redhat.coderland.reactica.model;


import java.util.UUID;

public class Ride {

  public static final String STATE_UNKNOWN = "UNKNOWN";
  public static final String STATE_PLANNED = "PLANNED";
  public static final String STATE_IN_PROGRESS = "IN_PROGRESS";
  public static final String STATE_COMPLETED = "COMPLETED";

  private String uuid;

  private String state;

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

  public String getState() {
    return state;
  }

  public Ride setState(String state) {
    this.state = state;
    return this;
  }


}

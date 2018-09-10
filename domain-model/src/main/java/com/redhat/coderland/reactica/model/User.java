package com.redhat.coderland.reactica.model;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class User {
  private String id;
  private String name;
  private String rideId;
  private String currentState;
  private long enterTime; //Time in seconds since beginning of Epoch in UTC

  /**
   * Constructs an empty User Object
   */
  public User() {
  }

  /**
   * Constructs a new User object with state 'IN_QUEUE' and with the current time.
   * @param id
   * @param name
   */
  public User(String id, String name) {
    this.id = id;
    this.name = name;
    this.currentState = "IN_QUEUE";
    this.enterTime = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);

  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getRideId() {
    return rideId;
  }

  public void setRideId(String rideId) {
    this.rideId = rideId;
  }

  public String getCurrentState() {
    return currentState;
  }

  public void setCurrentState(String currentState) {
    this.currentState = currentState;
  }

  public long getEnterTime() {
    return enterTime;
  }

  public void setEnterTime(long enterTime) {
    this.enterTime = enterTime;
  }
}

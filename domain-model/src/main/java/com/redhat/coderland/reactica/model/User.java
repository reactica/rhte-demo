package com.redhat.coderland.reactica.model;

import java.time.Instant;

public class User {

  public static final String STATE_UNKNOWN = "UNKNOWN";
  public static final String STATE_IN_QUEUE = "IN_QUEUE";
  public static final String STATE_ON_RIDE = "ON_RIDE";
  public static final String STATE_RIDE_COMPLETED = "COMPLETED_RIDE";

  public static final String RIDE_ID = "reactica";

  private String id;
  private String name;
  private String rideId = RIDE_ID;
  private String currentState;
  private long enterQueueTime; //Time in seconds since beginning of Epoch in UTC
  private long completedRideTime; //Time in seconds since beginning of Epoch in UTC

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
    this.currentState = STATE_UNKNOWN;
    this.enterQueueTime = -1;

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

  public long getEnterQueueTime() {
    return enterQueueTime;
  }

  public void setEnterQueueTime(long enterQueueTime) {
    this.enterQueueTime = enterQueueTime;
  }

  public long getCompletedRideTime() {
    return completedRideTime;
  }

  public void setCompletedRideTime(long completedRideTime) {
    this.completedRideTime = completedRideTime;
  }

  @Override
  public String toString() {
    return "User{" +
      "id='" + id + '\'' +
      ", name='" + name + '\'' +
      ", rideId='" + rideId + '\'' +
      ", currentState='" + currentState + '\'' +
      ", enterQueueTime=" + enterQueueTime +
      ", completeRideTime=" + completedRideTime +
      '}';
  }

  public synchronized User putInQueue() {
    setCurrentState(STATE_IN_QUEUE);
    setEnterQueueTime(Instant.now().toEpochMilli() / 1000);
    return this;
  }

  public User onRide() {
    setCurrentState(STATE_ON_RIDE);
    return this;
  }

  public User completed() {
    setCompletedRideTime(Instant.now().toEpochMilli() / 1000);
    setCurrentState(STATE_RIDE_COMPLETED);
    return this;
  }

}

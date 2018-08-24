package me.escoffier.reactive.amqp;

public class EventBusToAmqp {

  private String queue;

  private String address;

  public String getQueue() {
    return queue;
  }

  public void setQueue(String queue) {
    this.queue = queue;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

}

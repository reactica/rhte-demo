package me.escoffier.reactive.amqp;

public class AmqpToEventBus {

  private String queue;

  private String address;

  private boolean publish;

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

  public boolean isPublish() {
    return publish;
  }

  public void setPublish(boolean publish) {
    this.publish = publish;
  }
}

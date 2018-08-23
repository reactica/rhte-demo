package me.escoffier.reactive.amqp;

import java.util.List;

public class AmqpConfiguration {

  private String host = "localhost";

  private int port = 5672;

  private String user;

  private String password;

  private String container;
  private List<AmqpToEventBus> amqpToEventBus;
  private List<EventBusToAmqp> eventBusToAmqp;

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getContainer() {
    return container;
  }

  public void setContainer(String container) {
    this.container = container;
  }

  public void setAmqpToEventBus(List<AmqpToEventBus> list) {
    this.amqpToEventBus = list;
  }

  public List<AmqpToEventBus> getAmqpToEventBus() {
    return this.amqpToEventBus;
  }

  public List<EventBusToAmqp> getEventBusToAmqp() {
    return eventBusToAmqp;
  }

  public void setEventBusToAmqp(List<EventBusToAmqp> eventBusToAmqp) {
    this.eventBusToAmqp = eventBusToAmqp;
  }
}

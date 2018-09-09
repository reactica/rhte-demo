package me.escoffier.reactive.amqp;

import java.util.ArrayList;
import java.util.List;

public class AmqpConfiguration {

  private String host = "localhost";

  private int port = 5672;

  private String user;

  private String password;

  private String container;
  private List<AmqpToEventBus> amqpToEventBus = new ArrayList<>();
  private List<EventBusToAmqp> eventBusToAmqp = new ArrayList<>();

  public String getHost() {
    return host;
  }

  public AmqpConfiguration setHost(String host) {
    this.host = host;
    return this;
  }

  public int getPort() {
    return port;
  }

  public AmqpConfiguration setPort(int port) {
    this.port = port;
    return this;
  }

  public String getUser() {
    return user;
  }

  public AmqpConfiguration setUser(String user) {
    this.user = user;
    return this;
  }

  public String getPassword() {
    return password;
  }

  public AmqpConfiguration setPassword(String password) {
    this.password = password;
    return this;
  }

  public String getContainer() {
    return container;
  }

  public AmqpConfiguration setContainer(String container) {
    this.container = container;
    return this;
  }

  public AmqpConfiguration setAmqpToEventBus(List<AmqpToEventBus> list) {
    this.amqpToEventBus = list;
    return this;
  }

  public List<AmqpToEventBus> getAmqpToEventBus() {
    return this.amqpToEventBus;
  }

  public List<EventBusToAmqp> getEventBusToAmqp() {
    return eventBusToAmqp;
  }

  public AmqpConfiguration setEventBusToAmqp(List<EventBusToAmqp> eventBusToAmqp) {
    this.eventBusToAmqp = eventBusToAmqp;
    return this;
  }

  public AmqpConfiguration addAmqpToEventBus(AmqpToEventBus toBeAdded) {
    this.amqpToEventBus.add(toBeAdded);
    return this;
  }

  public AmqpConfiguration addEventBusToAmqp(EventBusToAmqp toBeAdded) {
    this.eventBusToAmqp.add(toBeAdded);
    return this;
  }
}

package me.escoffier.reactive.rhdg;

public class DataGridConfiguration {

  private String host;

  private int port = 11222;

  public String getHost() {
    return host;
  }

  public DataGridConfiguration setHost(String host) {
    this.host = host;
    return this;
  }

  public int getPort() {
    return port;
  }

  public DataGridConfiguration setPort(int port) {
    this.port = port;
    return this;
  }
}

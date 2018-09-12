package me.escoffier.reactive.rhdg;

import org.infinispan.client.hotrod.marshall.ProtoStreamMarshaller;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.MessageMarshaller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataGridConfiguration {

  private String host;

  private int port = 11222;
  private Marshaller marshaller;
  private List<ProtoRecord> proto = new ArrayList<>();

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

  public Marshaller getMarshaller() {
    return marshaller;
  }

  public DataGridConfiguration addMarshaller(Marshaller marshaller) {
    this.marshaller = marshaller;
    return this;
  }

  public DataGridConfiguration addProtoFile(String path, MessageMarshaller marshaller, boolean registerSchemaOnServer) {
    this.proto.add(new ProtoRecord(path, marshaller, registerSchemaOnServer));
    return this;
  }

  public List<ProtoRecord> getProto() {
    return proto;
  }

  public static class ProtoRecord {
    private String path;
    private MessageMarshaller marshaller;
    private boolean registerOnServer;

    ProtoRecord(String path, MessageMarshaller marshaller, boolean registerOnServer) {
      this.path = path;
      this.marshaller = marshaller;
      this.registerOnServer = registerOnServer;
    }

    public String getPath() {
      return path;
    }

    public MessageMarshaller getMarshaller() {
      return marshaller;
    }

    public boolean isRegisterOnServer() {
      return registerOnServer;
    }
  }
}

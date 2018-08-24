package me.escoffier.reactive.rhdg.impl;

import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryCreated;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryModified;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryRemoved;
import org.infinispan.client.hotrod.annotation.ClientListener;
import org.infinispan.client.hotrod.event.ClientCacheEntryCreatedEvent;
import org.infinispan.client.hotrod.event.ClientCacheEntryModifiedEvent;
import org.infinispan.client.hotrod.event.ClientCacheEntryRemovedEvent;

@ClientListener
public class RemoteCacheListener {
  private final Vertx vertx;
  private final String address;

  RemoteCacheListener(Vertx vertx, String address) {
    this.vertx = vertx;
    this.address = address;
  }

  @ClientCacheEntryCreated
  public void handleCreatedEvent(ClientCacheEntryCreatedEvent e) {
    vertx.eventBus().publish(address, new JsonObject().put("event", "created").put("entry", e.getKey()));
  }

  @ClientCacheEntryModified
  public void handleModifiedEvent(ClientCacheEntryModifiedEvent e) {
    vertx.eventBus().publish(address, new JsonObject().put("event", "update").put("entry", e.getKey()));
  }

  @ClientCacheEntryRemoved
  public void handleRemovedEvent(ClientCacheEntryRemovedEvent e) {
    vertx.eventBus().publish(address, new JsonObject().put("event", "removed").put("entry", e.getKey()));
  }
}

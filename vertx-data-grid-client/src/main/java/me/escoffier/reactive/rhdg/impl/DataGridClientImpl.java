package me.escoffier.reactive.rhdg.impl;

import io.reactivex.Single;
import io.vertx.reactivex.core.Vertx;
import marshallers.UserMarshaller;
import me.escoffier.reactive.rhdg.AsyncCache;
import me.escoffier.reactive.rhdg.DataGridClient;
import me.escoffier.reactive.rhdg.DataGridConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.marshall.ProtoStreamMarshaller;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.SerializationContext;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class DataGridClientImpl implements DataGridClient {

  private static final Logger LOGGER = LogManager.getLogger("Data Grid Client");

  private final Vertx vertx;
  private final DataGridConfiguration config;
  private RemoteCacheManager caches;
  private Map<String, AsyncCache> cachesByName = new ConcurrentHashMap<>();

  public DataGridClientImpl(Vertx vertx, DataGridConfiguration configuration) {
    this.vertx = vertx;
    this.config = configuration;
  }

  public Single<DataGridClient> init() {
    return remoteCacheManager()
      .map(x -> this);
  }

  private Single<RemoteCacheManager> remoteCacheManager() {
    if (this.caches != null) {
      return Single.just(this.caches);
    }
    ConfigurationBuilder cb = new ConfigurationBuilder();
    LOGGER.info("JDG location: {}:{}", config.getHost(), config.getPort());
    cb.addServer()
      .host(config.getHost())
      .port(config.getPort())
      .marshaller(new ProtoStreamMarshaller());
    return vertx.<RemoteCacheManager>rxExecuteBlocking(
      future -> {
        RemoteCacheManager manager = new RemoteCacheManager(cb.build());

        try {
          LOGGER.info("Registering protobuff stream marshaller for the User object....");
          SerializationContext serCtx = ProtoStreamMarshaller.getSerializationContext(manager);
          serCtx.registerProtoFiles(FileDescriptorSource.fromResources("/user.proto"));
          serCtx.registerMarshaller(new UserMarshaller());

          future.complete(manager);
        } catch (IOException e) {
          LOGGER.error("Error trying to register protobuff marshaller for the User with message " + e.getMessage());
          future.fail(e);
        }
      }
    ).doOnSuccess(rcm -> caches = rcm);
  }

  /**
   * Executed on a worker thread.
   */
  private <K, V> Single<AsyncCache<K, V>> retrieveCache(String name) {
    LOGGER.info("Retrieving cache {} using Infinispan", name);
    return vertx.rxExecuteBlocking(
      future -> {
        try {
          RemoteCache<K, V> cache = caches.getCache(name);
          future.complete(new AsyncCacheImpl<>(vertx, cache));
        } catch (Exception e) {
          LOGGER.error("Unable to retrieve the cache {}", name, e);
          future.fail(e);
        }
      }
    );
  }

  @SuppressWarnings("unchecked")
  public <K, V> Single<AsyncCache<K, V>> getCache(String name) {
    AsyncCache<K, V> cache = cachesByName
      .get(Objects.requireNonNull(name, "The name must be set"));
    if (cache != null) {
      return Single.just(cache);
    }

    Single<AsyncCache<K, V>> single = retrieveCache(name);
    return single
      .doOnSuccess(c -> LOGGER.info("Cache " + name + " has been retrieved successfully"))
      .doOnSuccess(c -> cachesByName.put(name, c));
  }


}

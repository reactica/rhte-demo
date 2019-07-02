package me.escoffier.reactive.rhdg.impl;

import io.reactivex.Single;
import io.vertx.reactivex.core.Vertx;
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
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;

import java.io.*;
import java.util.List;
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
      .port(config.getPort());

    if (config.getMarshaller() != null) {
        cb.marshaller(new ProtoStreamMarshaller());
    }

    return vertx.<RemoteCacheManager>rxExecuteBlocking(
      future -> {
        RemoteCacheManager manager = new RemoteCacheManager(cb.build());
        List<DataGridConfiguration.ProtoRecord> proto = config.getProto();
        proto.forEach(rec -> {
          try {
            LOGGER.info("Registering protobuff stream marshaller: {} => {}", rec.getPath(), rec.getMarshaller());
            SerializationContext serCtx = ProtoStreamMarshaller.getSerializationContext(manager);
            serCtx.registerProtoFiles(FileDescriptorSource.fromResources(rec.getPath()));
            serCtx.registerMarshaller(rec.getMarshaller());

            if (rec.isRegisterOnServer()) {
              RemoteCache<String, String> metadataCache = manager.getCache(ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME);
              metadataCache.putIfAbsent(rec.getPath(), readResource(rec.getPath()));
              String errors = metadataCache.get(ProtobufMetadataManagerConstants.ERRORS_KEY_SUFFIX);
              if (errors != null) {
                future.fail("Some Protobuff schema files contain errors:\n" + errors);
              }
              LOGGER.info("Successfully registered the schema {} in {}", rec.getPath(), ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME);
            }
          } catch (IOException e) {
            LOGGER.error("Error trying to register protobuff marshaller for the {}", rec.getPath(), e);
            future.tryFail(e);
          }
        });
        future.tryComplete(manager);
      }
    ).doOnSuccess(rcm -> caches = rcm).toSingle();
  }

  /**
   * Note that this is a blocking call, but since are only reading this when we start the verticle once we will accept this as a blocking
   *
   * @param resourcePath
   * @return
   * @throws IOException
   */
  private String readResource(String resourcePath) throws IOException {
    try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
      Reader reader = new InputStreamReader(is, "UTF-8");
      StringWriter writer = new StringWriter();
      char[] buf = new char[1024];
      int len;
      while ((len = reader.read(buf)) != -1) {
        writer.write(buf, 0, len);
      }
      return writer.toString();
    }
  }

  /**
   * Executed on a worker thread.
   */
  private <K, V> Single<AsyncCache<K, V>> retrieveCache(String name) {
    LOGGER.info("Retrieving cache {} using Infinispan", name);
    return vertx.<AsyncCache<K, V>>rxExecuteBlocking(
      future -> {
        try {
          RemoteCache<K, V> cache = caches.getCache(name);
          future.complete(new AsyncCacheImpl<>(vertx, cache));
        } catch (Exception e) {
          LOGGER.error("Unable to retrieve the cache {}", name, e);
          future.fail(e);
        }
      }
    ).toSingle();
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

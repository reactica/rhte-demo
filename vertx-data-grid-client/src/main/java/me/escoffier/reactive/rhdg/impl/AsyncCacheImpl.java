package me.escoffier.reactive.rhdg.impl;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.vertx.reactivex.core.Context;
import io.vertx.reactivex.core.Vertx;
import me.escoffier.reactive.rhdg.AsyncCache;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.Search;
import org.infinispan.commons.util.CloseableIterator;
import org.infinispan.query.api.continuous.ContinuousQuery;
import org.infinispan.query.api.continuous.ContinuousQueryListener;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

public class AsyncCacheImpl<K, V> implements AsyncCache<K, V> {
  private static final Logger LOGGER = LogManager.getLogger("InfinispanAsyncCache");

  private final Vertx vertx;
  private final RemoteCache<K, V> cache;
  private List<RemoteCacheListener> listeners = new CopyOnWriteArrayList<>();
  private List<ContinuousQuery<K, V>> queries = new CopyOnWriteArrayList<>();

  public AsyncCacheImpl(Vertx vertx, RemoteCache<K, V> cache) {
    this.vertx = Objects.requireNonNull(vertx, "Vertx must be set");
    this.cache = Objects.requireNonNull(cache, "The cache must be set");
  }

  private Context getContext() {
    return vertx.getOrCreateContext();
  }

  @Override
  public Completable put(K k, V v) {
    Objects.requireNonNull(k, "The key must be set");
    Objects.requireNonNull(v, "The value must be set");
    return getContext()
      .rxExecuteBlocking(
        fut -> {
          cache.put(k, v);
          fut.complete();
        }
      )
      .ignoreElement();
  }

  @Override
  public void listen(String address) {
    RemoteCacheListener listener = new RemoteCacheListener(vertx, address);
    listeners.add(listener);
    cache.addClientListener(listener);
  }

  @Override
  public Completable put(K k, V v, long amount, TimeUnit tu) {
    Objects.requireNonNull(k, "The key must be set");
    Objects.requireNonNull(v, "The value must be set");
    return getContext()
      .rxExecuteBlocking(
        fut -> {
          cache.put(k, v, amount, tu);
          fut.complete();
        }
      )
      .ignoreElement();
  }

  @Override
  public Maybe<V> get(K k) {
    Objects.requireNonNull(k, "The key must be set");
    return getContext()
      .<V>rxExecuteBlocking(
        fut -> {
          V v = cache.get(k);
          fut.complete(v);
        }
      ).flatMap(value -> {
        if (value != null) {
          return Maybe.just(value);
        }
        return Maybe.empty();
      });
  }

  @Override
  public Single<V> get(K k, V def) {
    Objects.requireNonNull(k, "The key must be set");
    return get(k)
      .toSingle(def);
  }

  @Override
  public Completable remove(K k) {
    Objects.requireNonNull(k, "The key must be set");
    return getContext()
      .rxExecuteBlocking(
        fut -> {
          cache.remove(k);
          fut.complete();
        }
      )
      .doOnError(err -> LOGGER.error("Error on remove", err))
      .ignoreElement();
  }

  @Override
  public Completable clear() {
    return getContext()
      .rxExecuteBlocking(
        fut -> {
          cache.clear();
          fut.complete();
        }
      )
      .doOnError(err -> LOGGER.error("Error on clear", err))
      .ignoreElement();
  }

  @Override
  public String name() {
    return cache.getName();
  }

  @Override
  public Single<Integer> size() {
    return getContext().<Integer>
      rxExecuteBlocking(
        fut -> fut.complete(cacheSize())
      ).toSingle();
  }

  private int cacheSize() {
    try {
      return cache.size();
    } catch (Throwable t) {
      LOGGER.error("Error on size", t);
      throw t;
    }
  }

  @Override
  public Single<Boolean> replace(K key, V oldValue, V newValue) {
    return
      getContext()
        .<Boolean>rxExecuteBlocking(future -> future.complete(cache.replace(key, oldValue, newValue))).toSingle();
  }

  @Override
  public Single<Map<K, V>> all() {
    return
      getContext()
        .<Map<K, V>>rxExecuteBlocking(
          fut -> {
            CloseableIterator<Map.Entry<Object, Object>> iterator = cache.retrieveEntries(null, 100);
            Map<K, V> map = new LinkedHashMap<>();
            iterator.forEachRemaining(entry -> map.put((K) entry.getKey(), (V) entry.getValue()));
            fut.complete(map);
          }
        )
        .doOnError(err -> LOGGER.error("Error on all", err))
        .toSingle();
  }

  @Override
  public Completable registerContinuousQuery(Query query, ContinuousQueryListener<K, V> listener) {
    return Single.just(query)
      .observeOn(Schedulers.single())
      .map(q -> {
        ContinuousQuery<K, V> continuousQuery = Search.getContinuousQuery(this.cache);
        continuousQuery.addContinuousQueryListener(query, listener);
        queries.add(continuousQuery);
        return continuousQuery;
      }).ignoreElement();
  }

  @Override
  public Completable close() {
    return vertx.rxExecuteBlocking(
      fut -> {
        listeners.forEach(cache::removeClientListener);
        queries.forEach(ContinuousQuery::removeAllListeners);
        listeners.clear();
        queries.clear();
      }
    ).ignoreElement();
  }

  @Override
  public QueryFactory getQueryFactory() {
    return Search.getQueryFactory(this.cache);
  }
}

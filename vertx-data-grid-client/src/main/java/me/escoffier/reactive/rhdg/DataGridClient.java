package me.escoffier.reactive.rhdg;

import io.reactivex.Single;
import io.vertx.reactivex.core.Vertx;
import me.escoffier.reactive.rhdg.impl.DataGridClientImpl;

public interface DataGridClient {

  static Single<DataGridClient> create(Vertx vertx, DataGridConfiguration configuration) {
    DataGridClientImpl client = new DataGridClientImpl(vertx, configuration);
    return client.init();
  }

  <K, V> Single<AsyncCache<K, V>> getCache(String name);




}

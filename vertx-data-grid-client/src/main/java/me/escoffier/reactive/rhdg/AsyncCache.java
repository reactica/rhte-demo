package me.escoffier.reactive.rhdg;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public interface AsyncCache<K, V> {

  Completable put(K k, V v);

  void listen(String address);

  Completable put(K k, V v, long amount, TimeUnit tu);

  Maybe<V> get(K k);

  Single<V> get(K k, V def);

  Completable remove(K k);

  Completable clear();

  String name();

  Single<Integer> size();

  Single<Boolean> replace(K key, V oldValue, V newValue);

  Single<Map<K, V>> all();
}

package example;

import io.reactivex.Single;
import io.vertx.reactivex.core.AbstractVerticle;
import me.escoffier.reactive.rhdg.DataGridClient;
import me.escoffier.reactive.rhdg.DataGridConfiguration;

import java.util.concurrent.atomic.AtomicInteger;

public class VerticleUsingCache extends AbstractVerticle {

  @Override
  public void start() {
    Single<DataGridClient> single = DataGridClient.create(vertx, new DataGridConfiguration()
      .setHost("eventstore-dg-hotrod")
      .setPort(11333));

    AtomicInteger count = new AtomicInteger();
    single
      .flatMap(client -> client.getCache(""))
      .doOnSuccess(cache ->
        vertx.setPeriodic(3000, x -> cache.put("message", count.incrementAndGet()).subscribe()))
      .doOnSuccess(cache ->
        vertx.setPeriodic(3000, x -> cache.get("message", -1).doOnSuccess(v -> System.out.println("Retrieve " + v + " from cache")).subscribe()))
      .doOnSuccess(cache -> {
        vertx.eventBus().consumer("my-cache-updates").handler(msg -> System.out.println("Cache update: " + msg.body()));
        cache.listen("my-cache-updates");
      })
      .subscribe();


  }

}

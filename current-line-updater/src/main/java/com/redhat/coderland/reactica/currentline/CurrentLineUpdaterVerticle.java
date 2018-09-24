package com.redhat.coderland.reactica.currentline;

import com.redhat.coderland.reactica.model.User;
import io.vertx.core.Future;
import io.vertx.reactivex.CompletableHelper;
import io.vertx.reactivex.core.AbstractVerticle;
import me.escoffier.reactive.rhdg.AsyncCache;
import me.escoffier.reactive.rhdg.DataGridClient;
import me.escoffier.reactive.rhdg.DataGridConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.infinispan.client.hotrod.marshall.ProtoStreamMarshaller;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;
import org.infinispan.query.dsl.SortOrder;

import java.time.Instant;

public class CurrentLineUpdaterVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LogManager.getLogger("CurrentLineUpdaterVerticle");
  private static final String USER_PROTOBUFF_DEFINITION_FILE = "/user.proto";
  private static final String USEREVENTS_CACHENAME = "userevents";

  private AsyncCache<String, User> cache;

  @Override
  public void start(Future<Void> done) throws Exception {
    LOGGER.info("Starting deployment of " + this.getClass().getName());

    DataGridClient.create(
      vertx,
      new DataGridConfiguration()
        .setHost(config().getString("host"))
        .setPort(config().getInteger("port"))
        .addMarshaller(new ProtoStreamMarshaller())
        .addProtoFile(USER_PROTOBUFF_DEFINITION_FILE, new UserMarshaller(), true)
    )
      .doOnSuccess(client -> LOGGER.info("Successfully created a Data Grid Client"))
      .flatMap(client -> client.<String, User>getCache(USEREVENTS_CACHENAME))
      .doOnSuccess(cache -> {
        LOGGER.info("Successfully got the cache {} ", USEREVENTS_CACHENAME);
        this.cache = cache;
      })
      .flatMapCompletable(cache -> {
        QueryFactory queryFactory = cache.getQueryFactory();
        Query query = queryFactory.from(User.class)
          .having("rideId").eq("reactica")
          .and()
          .having("currentState").in(User.STATE_IN_QUEUE,User.STATE_ON_RIDE,User.STATE_RIDE_COMPLETED)
          .orderBy("enterQueueTime", SortOrder.DESC)
          .build();

        return cache.registerContinuousQuery(query, new UserContinuousQueryListener(vertx));
      }).doOnComplete(() -> LOGGER.info("Successfully connected the continuous query"))
      .subscribe(CompletableHelper.toObserver(done));
  }

  @Override
  public void stop(Future<Void> done) {
    if (this.cache != null) {
      this.cache.close().subscribe(CompletableHelper.toObserver(done));
    } else {
      done.complete();
    }
  }

}

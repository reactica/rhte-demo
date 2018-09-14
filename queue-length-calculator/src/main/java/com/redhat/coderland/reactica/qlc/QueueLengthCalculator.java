package com.redhat.coderland.reactica.qlc;

import com.redhat.coderland.reactica.model.User;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.CompletableHelper;
import io.vertx.reactivex.RxHelper;
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

import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class QueueLengthCalculator extends AbstractVerticle {
  private static final Logger LOGGER = LogManager.getLogger("CurrentLineUpdaterVerticle");
  private static final String USER_PROTOBUFF_DEFINITION_FILE = "/user.proto";
  private static final String USEREVENTS_CACHENAME = "userevents";

  private static final long DEFAULT_RIDE_DURATION = 60;
  private static final int DEFAULT_USER_ON_RIDE = 10;

  private AsyncCache<String, User> cache;


  private long duration;
  private int numberOfUsers;



  @Override
  public void start(Future<Void> done) throws Exception {
    LOGGER.info("Starting " + this.getClass().getName());


    DataGridClient.create(
      vertx,
      new DataGridConfiguration()
        .setHost("eventstore-dg-hotrod")
        .setPort(11333)
        .addMarshaller(new ProtoStreamMarshaller())
        .addProtoFile(USER_PROTOBUFF_DEFINITION_FILE, new UserMarshaller(), true)
    )
    .doOnSuccess(client -> LOGGER.info("Successfully created a Data Grid Client"))
    .flatMap(client -> client.<String, User>getCache(USEREVENTS_CACHENAME))
    .subscribe(cache -> {
      LOGGER.info("Successfully got the cache {} ", USEREVENTS_CACHENAME);
      this.cache = cache;
      configure(config());

      Scheduler scheduler = io.vertx.reactivex.core.RxHelper.scheduler(vertx);

      // Create a periodic event stream using Vertx scheduler
      Flowable<Long> o = Flowable.interval(10, TimeUnit.SECONDS, scheduler);

      o.subscribe(time -> {
        QueryFactory queryFactory = cache.getQueryFactory();
        Query queueCountQuery = queryFactory.from(User.class)
          .having("currentState").eq(User.STATE_IN_QUEUE)
          .and()
          .having("rideId").eq("reactica")
          .build();
        int queueSize = queueCountQuery.list().size();
        LOGGER.info("Current queue length is " + queueSize);
        int numberOfRidesToLastPerson = Math.floorDiv(queueSize, numberOfUsers);
        LOGGER.info("The last person in queue will approx will wait " + numberOfRidesToLastPerson + " number of rides");
        long approxWaitTime = numberOfRidesToLastPerson * duration;
        LOGGER.info("Calculated the approx waittime to " + approxWaitTime);
        JsonObject qlcEventMessage = new JsonObject().put("calculated-wait-time", approxWaitTime);
        LOGGER.info("Sending queue length event message: " + qlcEventMessage.encodePrettily() );
        vertx.eventBus().send("to-qlc-queue", qlcEventMessage);
        LOGGER.info("Message sent");
      });

//      vertx.setPeriodic(10000, time -> LOGGER.info("Querying the datagrid for the current line at time " + time ));

      done.complete(null);
    });
  }

  private void configure(JsonObject json) {
    if (json == null) {
      return;
    }
    LOGGER.info("Configuring the ride simulator");
    duration = json.getLong("duration-in-seconds", DEFAULT_RIDE_DURATION);
    numberOfUsers = json.getInteger("users-per-ride", DEFAULT_USER_ON_RIDE);

  }
}

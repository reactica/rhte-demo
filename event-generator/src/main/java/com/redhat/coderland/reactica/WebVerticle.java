package com.redhat.coderland.reactica;

import com.redhat.coderland.reactica.model.User;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.CompletableHelper;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.handler.BodyHandler;
import me.escoffier.reactive.rhdg.AsyncCache;
import me.escoffier.reactive.rhdg.DataGridClient;
import me.escoffier.reactive.rhdg.DataGridConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WebVerticle extends AbstractVerticle  {


  private static final String CONTENT_TYPE = "Content-Type";
  private static final String JSON_CONTENT_TYPE = "application/json; charset=UTF-8";
  private AsyncCache<String, String> cache;
  private AsyncCache<String, String> usereventcache;
  private static final Logger LOGGER = LogManager.getLogger(WebVerticle.class);

  @Override
  public void start(Future<Void> done) {
    initializeCache()
      .andThen(initializeUserEventCache())
      .andThen(initializeHttpServer())
      .subscribe(CompletableHelper.toObserver(done));
  }

  private Completable initializeUserEventCache() {
    Single<DataGridClient> single = DataGridClient.create(vertx, new DataGridConfiguration()
      .setHost("eventstore-dg-hotrod")
      .setPort(11333));
    return single.flatMap(client -> client.<String, String>getCache("userevents"))
      .doOnSuccess(ac -> this.usereventcache = ac)
      .ignoreElement();
  }

  private Completable initializeCache() {
    Single<DataGridClient> single = DataGridClient.create(vertx, new DataGridConfiguration()
      .setHost("eventstore-dg-hotrod")
      .setPort(11333));
    return single.flatMap(client -> client.<String, String>getCache("users"))
      .doOnSuccess(ac -> this.cache = ac)
      .ignoreElement();
  }

  private Completable initializeHttpServer() {
    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());
    router.get("/queue").handler(this::getQueue);
    router.get("/on-ride").handler(this::getOnRide);
    router.get("/completed").handler(this::getCompleted);
    router.get("/all").handler(this::all);
    router.get("/health").handler(rc -> rc.response().end("OK"));
    router.post("/user").handler(this::addUser);

    router.post("/simulators/users").handler(this::toggleUserSimulator);
    router.delete("/simulators/users").handler(this::clearUsers);
    router.post("/simulators/ride").handler(this::toggleRideSimulator);

    return vertx.createHttpServer()
      .requestHandler(router::accept)
      .rxListen(8080)
      .ignoreElement();
  }

  private void toggleUserSimulator(RoutingContext rc) {
    boolean enabled = rc.getBodyAsJson().getBoolean("enabled");
    vertx.eventBus().send("user-simulator-toggle", enabled);
    rc.response()
      .setStatusCode(204)
      .end();
  }

  private void toggleRideSimulator(RoutingContext rc) {
    boolean enabled = rc.getBodyAsJson().getBoolean("enabled");
    vertx.eventBus().send("ride-simulator-toggle", enabled);
    rc.response()
      .setStatusCode(204)
      .end();
  }

  private void addUser(RoutingContext rc) {
    JsonObject json = rc.getBodyAsJson();
    String name = json.getString("name", CuteNameService.generate());

    User user = new User(name, name).putInQueue();

    cache.put(user.getName(), JsonObject.mapFrom(user).encode())
      .subscribe(() -> {
        vertx.eventBus().send(Events.USER_EVENTS, Events.create(Events.USER_IN_QUEUE, user));
        JsonObject res = new JsonObject()
          .put("result", "User " + name + " added").put("name", name);
        rc.response().putHeader(CONTENT_TYPE, JSON_CONTENT_TYPE).end(res.encode());
      });
  }

  private void all(RoutingContext rc) {
    getUsers(null)
      .map(Json::encode)
      .subscribe(res -> rc.response().putHeader(CONTENT_TYPE, JSON_CONTENT_TYPE).end(res));
  }

  private void getCompleted(RoutingContext rc) {
    getUsers(User.STATE_RIDE_COMPLETED)
      .map(Json::encode)
      .subscribe(res -> rc.response().putHeader(CONTENT_TYPE, JSON_CONTENT_TYPE).end(res));
  }

  private void getOnRide(RoutingContext rc) {
    getUsers(User.STATE_ON_RIDE)
      .map(Json::encode)
      .subscribe(res -> rc.response().putHeader(CONTENT_TYPE, JSON_CONTENT_TYPE).end(res));
  }

  private void getQueue(RoutingContext rc) {
    getUsers(User.STATE_IN_QUEUE)
      .map(Json::encode)
      .subscribe(res -> rc.response().putHeader(CONTENT_TYPE, JSON_CONTENT_TYPE).end(res));
  }

  private void clearUsers(RoutingContext rc) {
    LOGGER.info("Clearing users from cache");
    cache.clear().andThen(usereventcache.clear()).subscribe(() ->  rc.response().setStatusCode(204).end());
  }

  private Single<List<User>> getUsers(String state) {
    return cache.all().map(Map::values)
      .map(all ->
        all.stream()
          .map(s -> Json.decodeValue(s, User.class))
          .filter(user -> {
            if (state != null) {
              return user.getCurrentState().equalsIgnoreCase(state);
            } else {
              return true; // Accept all
            }
          })
          .sorted((u1, u2) -> Long.compare(u2.getEnterQueueTime(), u1.getEnterQueueTime()))
          .collect(Collectors.toList()));
  }
}

package com.redhat.coderland.reactica.eventstore;

import com.redhat.coderland.reactica.model.User;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import me.escoffier.reactive.rhdg.DataGridClient;
import me.escoffier.reactive.rhdg.DataGridConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UserEventReceiverVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LogManager.getLogger("UserEventReceiverVerticle");

  @Override
  public void start() throws Exception {

    Single<DataGridClient> single = DataGridClient.create(vertx, new DataGridConfiguration()
      .setHost("eventstore-dg-hotrod")
      .setPort(11333));

    vertx.eventBus().consumer("user-events",message -> {
      LOGGER.info("RECEIVED USER EVENT: " + message.body().toString());

      JsonObject userEvent = JsonObject.mapFrom(message.body());
      User user = userEvent.mapTo(User.class);
      single.flatMap(client -> client.getCache("userevents"))
        .doOnSuccess(cache -> {
          cache.put(user.getId(),user);
          LOGGER.info("Saved user with id " + user.getId() + " to the Data Grid");
        })
        .subscribe();
    });
  }
}

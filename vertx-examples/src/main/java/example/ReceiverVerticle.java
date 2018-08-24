package example;

import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.atomic.AtomicInteger;

public class ReceiverVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LogManager.getLogger("ReceiverVerticle");

  @Override
  public void start() {
    vertx.eventBus().<JsonObject>consumer("results", msg -> {
      LOGGER.info("Receiving {} {}", msg.body().getString("message"), msg.body().getInteger("count"));
    });
  }
}

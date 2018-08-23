package example;

import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.atomic.AtomicInteger;

public class SenderVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LogManager.getLogger("SenderVerticle");

  @Override
  public void start() {
    AtomicInteger count = new AtomicInteger();
    vertx.setPeriodic(5000, x -> {
      LOGGER.info("Sending a message to the event bus");
      vertx.eventBus().send("messages", new JsonObject().put("message", "hello").put("count", count.getAndIncrement()));
    });
  }
}

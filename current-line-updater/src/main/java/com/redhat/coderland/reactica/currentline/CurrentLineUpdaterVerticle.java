package com.redhat.coderland.reactica.currentline;

import com.redhat.coderland.reactica.model.User;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.Search;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.marshall.ProtoStreamMarshaller;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.MessageMarshaller;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.query.api.continuous.ContinuousQuery;
import org.infinispan.query.api.continuous.ContinuousQueryListener;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;
import org.infinispan.query.dsl.SortOrder;
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;

import java.io.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CurrentLineUpdaterVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LogManager.getLogger("CurrentLineUpdaterVerticle");
  private static final String USER_PROTOBUFF_DEFINITION_FILE = "/user.proto";
  private static final String USEREVENTS_CACHENAME = "userevents";

  private RemoteCache<String,User> cache;

  final Map<String, User> matches = new ConcurrentHashMap<>();

  ContinuousQueryListener<String, User> listener = new ContinuousQueryListener<String, User>() {
    @Override
    public void resultJoining(String key, User value) {
      matches.put(key, value);
      LOGGER.info("Received an new value for user with key " + key + " and value " + value);
      vertx.eventBus().send("current-line-events", JsonObject.mapFrom(matches));
    }

    @Override
    public void resultUpdated(String key, User value) {
      matches.replace(key, value);
      LOGGER.info("Received an updated value for user with key " + key + " and value " + value);
      vertx.eventBus().send("current-line-events", JsonObject.mapFrom(matches));
    }

    @Override
    public void resultLeaving(String key) {
      matches.remove(key);
      LOGGER.info("Received an removed value for user with key " + key);
      vertx.eventBus().send("current-line-events", JsonObject.mapFrom(matches));
    }
  };



  @Override
  public void start() throws Exception {
    LOGGER.info("Staring deployment of " + this.getClass().getName());
    ConfigurationBuilder cb = new ConfigurationBuilder();
    cb.addServer()
      .host(config().getString("host"))
      .port(config().getInteger("port"))
      .marshaller(new ProtoStreamMarshaller());
    RemoteCacheManager cacheManager = new RemoteCacheManager(cb.build());
    SerializationContext serCtx = ProtoStreamMarshaller.getSerializationContext(cacheManager);
    serCtx.registerProtoFiles(FileDescriptorSource.fromResources(USER_PROTOBUFF_DEFINITION_FILE));
    serCtx.registerMarshaller(new MessageMarshaller<User>() {
      @Override
      public User readFrom(MessageMarshaller.ProtoStreamReader reader) throws IOException {
        User user = new User();
        user.setId(reader.readString("id"));
        user.setName(reader.readString("name"));
        user.setRideId(reader.readString("rideId"));
        user.setCurrentState(reader.readString("currentState"));
        user.setEnterTime(reader.readLong("enterTime"));

        return user;
      }

      @Override
      public void writeTo(MessageMarshaller.ProtoStreamWriter writer, User user) throws IOException {
        writer.writeString("id",user.getId());
        writer.writeString("name",user.getName());
        writer.writeString("rideId","reactica");
        writer.writeString("currentState",user.getCurrentState());
        writer.writeLong("enterTime",user.getEnterTime());

      }

      @Override
      public Class<? extends User> getJavaClass() {
        return User.class;
      }

      @Override
      public String getTypeName() {
        return "com.redhat.coderland.reactica.model.User";
      }
    });

    LOGGER.info("Successfully created a RemoteCacheManager");

    this.cache = cacheManager.getCache(USEREVENTS_CACHENAME);

    LOGGER.info("Successfully got the RemoteCache instance for " + USEREVENTS_CACHENAME);

    // register the schemas with the server too

    RemoteCache<String, String> metadataCache = cacheManager.getCache(ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME);
    metadataCache.putIfAbsent(USER_PROTOBUFF_DEFINITION_FILE, readResource(USER_PROTOBUFF_DEFINITION_FILE));
    String errors = metadataCache.get(ProtobufMetadataManagerConstants.ERRORS_KEY_SUFFIX);
    if (errors != null) {
      throw new IllegalStateException("Some Protobuff schema files contain errors:\n" + errors);
    }

    LOGGER.info("Successfully registered the schema " + USER_PROTOBUFF_DEFINITION_FILE + " in " + ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME);

    QueryFactory queryFactory = Search.getQueryFactory(this.cache);
    Query query = queryFactory.from(User.class)
      .having("rideId").eq("reactica")
      .orderBy("enterTime", SortOrder.DESC)
      .build();
    ContinuousQuery<String, User> continuousQuery = Search.getContinuousQuery(this.cache);
    continuousQuery.addContinuousQueryListener(query, listener);

    LOGGER.info("Successfully connected the continuous query " + listener.getClass().getName());


  }

  @Override
  public void stop() {
    if(this.cache != null) {
      Search.getContinuousQuery(this.cache).removeContinuousQueryListener(listener);
      this.cache.stop();
    }
  }


  /**
   * Note that this is a blocking call, but since are only reading this when we start the verticle once we will accept this as a blocking
   * @param resourcePath
   * @return
   * @throws IOException
   */
  private String readResource(String resourcePath) throws IOException {
    try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
      Reader reader = new InputStreamReader(is, "UTF-8");
      StringWriter writer = new StringWriter();
      char[] buf = new char[1024];
      int len;
      while ((len = reader.read(buf)) != -1) {
        writer.write(buf, 0, len);
      }
      return writer.toString();
    }
  }

}

package io.github.stefanosbou;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import io.github.stefanosbou.kafka.custom.message.CustomMessage;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.KeyStoreOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.jwt.JWTOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.JWTAuthHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;
import io.vertx.core.eventbus.EventBus;

import java.util.HashMap;
import java.util.Map;

import static io.github.stefanosbou.helpers.Config.KAFKA_TOPIC;

public class ApiEndpointVerticle extends AbstractVerticle {
   private static final int DEFAULT_PORT = 8080;
   private static final String EVENTBUS_ADDRESS = "updates";

   private JWTAuth jwtProvider;
   private KafkaProducer<Long, CustomMessage.Order> producer;

   @Override
   public void start(Future<Void> future) {
      EventBus bus = vertx.eventBus();
      Router router = Router.router(vertx);

      int port = config().getInteger("http.port", DEFAULT_PORT);

      // Create a JWT Auth Provider
      jwtProvider = JWTAuth.create(vertx, new JWTAuthOptions()
         .setKeyStore(new KeyStoreOptions()
            .setPath("keystore.jceks")
            .setPassword("secret")));

      DBObserverThread dbObserverThread = new DBObserverThread(bus);
      dbObserverThread.start();

      // use producer for interacting with Apache Kafka
      producer = KafkaProducer.create(vertx, getKafkaConfig());

      // protect the API
      router.route("/api/*").handler(JWTAuthHandler.create(jwtProvider, "/api/token"));

      // this route is excluded from the auth handler
      router.post("/api/token").handler(this::tokenGeneratorHandler);

      // body handler
      router.route().handler(BodyHandler.create());
      router.post("/api/orders").handler(this::tradeHandler);
      router.route("/eventbus/*").handler(
         SockJSHandler.create(vertx).bridge(
            new BridgeOptions().addOutboundPermitted(
               new PermittedOptions().setAddress(EVENTBUS_ADDRESS))));

      HttpServerOptions options = new HttpServerOptions();
      vertx.createHttpServer(options).requestHandler(router::accept).listen(port, ar -> {
         if (ar.succeeded()) {
            future.complete();
         } else {
            future.fail(ar.cause());
         }
      });
   }

   private void tokenGeneratorHandler(RoutingContext routingContext) {
      // on the verify endpoint once you verify the identity of the user by its username/password
      // now for any request to protected resources you should pass this string in the HTTP header Authorization as:
      // Authorization: Bearer <token>
      routingContext.response().putHeader("Content-Type", "text/plain");
      routingContext.response().end(jwtProvider.generateToken(new JsonObject(), new JWTOptions().setExpiresInSeconds(3600)));

   }

   private void tradeHandler(RoutingContext routingContext) {
      JsonObject requestBody;
      try {
         requestBody = routingContext.getBodyAsJson();
      } catch(Exception e) {
         routingContext.fail(400);
         return;
      }
      CustomMessage.Order.Builder order = CustomMessage.Order.newBuilder();

      try {
         JsonFormat.parser().merge(requestBody.encodePrettily(), order);
         KafkaProducerRecord<Long, CustomMessage.Order> record =
            KafkaProducerRecord.create(KAFKA_TOPIC, order.build());
         producer.write(record);
         routingContext.response().end();
      } catch (InvalidProtocolBufferException e) {
         routingContext.fail(400);
      }
   }

   private Map<String, String> getKafkaConfig() {
      Map<String, String> config = new HashMap<>();
      config.put("bootstrap.servers", "localhost:9092");
      config.put("key.serializer", "org.apache.kafka.common.serialization.LongSerializer");
      config.put("value.serializer", "io.github.stefanosbou.kafka.custom.message.OrderSerializer");
      config.put("acks", "1");
      return config;
   }




}

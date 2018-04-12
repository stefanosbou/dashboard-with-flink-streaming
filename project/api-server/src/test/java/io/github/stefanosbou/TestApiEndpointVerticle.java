package io.github.stefanosbou;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.WebClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.ServerSocket;

@RunWith(VertxUnitRunner.class)
public class TestApiEndpointVerticle {
   Vertx vertx;
   Integer port;

   /**
    * Before executing our test, let's deploy our verticle.
    * <p/>
    * This method instantiates a new Vertx and deploy the verticle. Then, it waits in the verticle has successfully
    * completed its start sequence (thanks to `context.asyncAssertSuccess`).
    *
    * @param context the test context.
    */
   @Before
   public void setUp(TestContext context) throws IOException {
      vertx = Vertx.vertx();
      Async async = context.async();

      // Let's configure the verticle to listen on the 'test' port (randomly picked).
      // We create deployment options and set the _configuration_ json object:
      ServerSocket socket = new ServerSocket(0);
      port = socket.getLocalPort();
      socket.close();

      DeploymentOptions options = new DeploymentOptions()
         .setConfig(new JsonObject().put("http.port", port)
         );

      vertx.deployVerticle(new ApiEndpointVerticle(), options, ar -> {
         if (ar.succeeded()) {
            async.complete();
         } else {
            context.fail(ar.cause());
         }
      });
   }

   /**
    * This method, called after our test, just cleanup everything by closing the vert.x instance
    *
    * @param context the test context
    */
   @After
   public void tearDown(TestContext context) {
      vertx.close(context.asyncAssertSuccess());
   }

   /**
    * Let's ensure that our application behaves correctly.
    *
    * @param context the test context
    */
   @Test
   public void testApiIsProtected(TestContext context) {
      Async async = context.async();
      WebClient client = WebClient.create(vertx);

      client.get(port, "localhost", "/api/orders").send(ar -> {
         if (ar.succeeded()) {
            if(ar.result().statusCode() == 401){
               async.complete();
            } else {
               context.fail();
            }
         } else {
            context.fail(ar.cause().getMessage());
         }
      });
   }

   @Test
   public void testApiGetToken(TestContext context) {
      Async async = context.async();
      WebClient client = WebClient.create(vertx);

      client.post(port, "localhost", "/api/token").send(ar -> {
         if (ar.succeeded()) {
            if(ar.result().statusCode() == 200){
               async.complete();
            } else {
               context.fail();
            }
         } else {
            context.fail(ar.cause().getMessage());
         }
      });
   }

   @Test
   public void testApiPostMalformedOrder(TestContext context) {
      Async async = context.async();
      WebClient client = WebClient.create(vertx);

      JsonObject malformedOrderJson = new JsonObject()
         .put("user", "12345")
         .put("currencyFrom", "EUR")
         .put("currencyTo", "USD")
         .put("amountSell", 1000)
         .put("amountBuy", 747.1)
         .put("rate", 0.7471)
         .put("timePlaced", "24­JAN­15 10:27:44")
         .put("originatingCountry", "US");

      client.post(port, "localhost", "/api/token").send(ar -> {
         if (ar.succeeded()) {
            if(ar.result().statusCode() == 200){
               client.post(port, "localhost", "/api/orders")
                  .putHeader("Authorization", "Bearer " + ar.result().bodyAsString())
                  .sendJsonObject(malformedOrderJson, res -> {
                  if (res.succeeded()) {
                     if(res.result().statusCode() == 400){
                        async.complete();
                     } else {
                        context.fail();
                     }
                  } else {
                     context.fail(res.cause().getMessage());
                  }
               });
            } else {
               context.fail();
            }
         } else {
            context.fail(ar.cause().getMessage());
         }
      });
   }

   @Test
   public void testApiPostEmptyOrder(TestContext context) {
      Async async = context.async();
      WebClient client = WebClient.create(vertx);

      client.post(port, "localhost", "/api/token").send(ar -> {
         if (ar.succeeded()) {
            if (ar.result().statusCode() == 200) {
               client.post(port, "localhost", "/api/orders")
                  .putHeader("Authorization", "Bearer " + ar.result().bodyAsString())
                  .send(res -> {
                     if (res.succeeded()) {
                        if (res.result().statusCode() == 400) {
                           async.complete();
                        } else {
                           context.fail();
                        }
                     } else {
                        context.fail(res.cause().getMessage());
                     }
                  });
            } else {
               context.fail();
            }
         } else {
            context.fail(ar.cause().getMessage());
         }
      });
   }



}
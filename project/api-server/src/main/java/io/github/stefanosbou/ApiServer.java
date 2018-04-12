package io.github.stefanosbou;

import io.vertx.core.Vertx;

public class ApiServer {
   public static void main(String[] args) {
      Vertx vertx = Vertx.vertx();

      vertx.deployVerticle(new ApiEndpointVerticle(), res -> {
         if(res.succeeded()){
            System.out.println("Successfully deployed ApiEndpointVerticle");
         } else {
            System.out.println("Failed to deploy ApiEndpointVerticle");
            System.out.println(res.cause().getLocalizedMessage());
         }
      });



   }
}

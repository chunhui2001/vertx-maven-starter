package com.shenmao.vertx.starter;

import com.shenmao.vertx.starter.configuration.ApplicationConfig;
import com.shenmao.vertx.starter.configuration.SqlQueriesConfig;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.FileNotFoundException;

public class MainVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);

  static  {

  }

  @Override
  public void start(Future<Void> startFuture) throws FileNotFoundException {

    Future<String> dbVerticleDeployment = Future.future();
    vertx.deployVerticle(new WikiDatabaseVerticle(), dbVerticleDeployment.completer());

    dbVerticleDeployment.compose(id -> {

      Future<String> httpVerticleDeployment = Future.future();

      vertx.deployVerticle(
          "com.shenmao.vertx.starter.HttpServerVerticle",
          new DeploymentOptions().setInstances(2),
          httpVerticleDeployment.completer()
      );

      return httpVerticleDeployment;

    }).setHandler(ar -> {

      if (ar.succeeded()) {
        startFuture.complete();
      } else {
        startFuture.fail(ar.cause());
      }

    });

  }

}

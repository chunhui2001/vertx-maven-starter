package com.shenmao.vertx.starter;

import com.shenmao.vertx.starter.database.wikipage.WikiPageDatabaseVerticle;
import com.shenmao.vertx.starter.database.wikipage.WikiPageDbService;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class MainVerticle extends AbstractVerticle {

  private static final Logger logger = LoggerFactory.getLogger(MainVerticle.class);


  private Future<String> deployWikiPageVerticle() {
    Future<String> future = Future.future();
    vertx.deployVerticle(new WikiPageDatabaseVerticle(),new DeploymentOptions().setInstances(1), future.completer());
    return future;
  }

  private Future<String> startHttpServer() {


    Future<String> future = Future.future();
    vertx.deployVerticle(
      "com.shenmao.vertx.starter.verticle.demo.DemoVerticle",
      new DeploymentOptions().setInstances(2), future.completer());
    return future;
  }

  @Override
  public void start(Future<Void> startFuture) throws Exception {

    deployWikiPageVerticle()
      .compose(id -> startHttpServer())
    .setHandler(ar -> {

        if (ar.succeeded()) {
          startFuture.complete();
        } else {
          startFuture.fail(ar.cause());
        }
    });

  }





}

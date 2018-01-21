package com.shenmao.vertx.starter;

import com.shenmao.vertx.starter.database.wikipage.WikiPageDatabaseVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.core.AbstractVerticle;
import rx.Single;

public class MainVerticleRx extends AbstractVerticle {

  private static final Logger logger = LoggerFactory.getLogger(MainVerticle.class);


  private Single<String> deployWikiPageVerticle() {
    return vertx.rxDeployVerticle(
      "com.shenmao.vertx.starter.database.wikipage.WikiPageDatabaseVerticle"
    , new DeploymentOptions().setInstances(1));
  }

  private Single<String> startHttpServer() {
    return vertx.rxDeployVerticle(
      "com.shenmao.vertx.starter.verticle.demo.DemoVerticle",
      new DeploymentOptions().setInstances(2));
  }

  @Override
  public void start(Future<Void> startFuture) throws Exception {

    deployWikiPageVerticle()
      .flatMap(id -> {
        return startHttpServer();
      }).subscribe( id -> startFuture.complete(), startFuture::fail);

  }





}

package com.shenmao.vertx.starter.http;

import com.shenmao.vertx.starter.Application;
import com.shenmao.vertx.starter.actions.DefaultAction;
import com.shenmao.vertx.starter.configuration.ApplicationConfig;
import com.shenmao.vertx.starter.routers.VertxRouter;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.shiro.ShiroAuth;
import io.vertx.ext.auth.shiro.ShiroAuthOptions;
import io.vertx.ext.auth.shiro.ShiroAuthRealmType;
import io.vertx.ext.web.Router;

public class HttpServerVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerVerticle.class);
  private static final String wikiDbQueue = "wikidb.queue";

  @Override
  public void start(Future<Void> startFuture) throws Exception {

    HttpServer server = vertx.createHttpServer();

    Router router = new VertxRouter(vertx).getRouter();

    int portNumber = Integer.parseInt(Application.getAppConfig().get(ApplicationConfig.AppConfig.APP_PORT));


    server
      .requestHandler(router::accept)
      .listen(portNumber, ar -> {
        if (ar.succeeded()) {
          LOGGER.info("HTTP server running on port " + portNumber); startFuture.complete();
        } else {
          LOGGER.error("Could not start a HTTP server", ar.cause()); startFuture.fail(ar.cause());
        }
      });

  }


}

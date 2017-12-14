package com.shenmao.vertx.starter.http;

import com.shenmao.vertx.starter.Application;
import com.shenmao.vertx.starter.configuration.ApplicationConfig;
import com.shenmao.vertx.starter.routers.VertxRouter;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Router;

public class HttpServerVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerVerticle.class);
  private static final String wikiDbQueue = "wikidb.queue";

  @Override
  public void start(Future<Void> startFuture) throws Exception {

    HttpServer server = vertx.createHttpServer();

//    HttpServer server = vertx.createHttpServer(new HttpServerOptions() .setSsl(true)
//      .setKeyStoreOptions(new JksOptions()
//        .setPath("ssh_keys/server-keystore.jks")
//        .setPassword("secret")));

//    webClient = WebClient.create(vertx, new WebClientOptions() .setDefaultHost("localhost")
//      .setDefaultPort(8080)
//      .setSsl(true) 1
//      .setTrustOptions(new JksOptions().setPath("server-keystore.jks").setPassword("secret")));

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

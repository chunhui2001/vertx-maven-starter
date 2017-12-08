package com.shenmao.vertx.starter;

import com.shenmao.vertx.starter.actions.DefaultAction;
import com.shenmao.vertx.starter.routers.VertxRouter;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;

public class HttpServerVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerVerticle.class);

  public static final String CONFIG_HTTP_SERVER_PORT = "http.server.port";
//  public static final String CONFIG_WIKIDB_QUEUE = "wikidb.queue";
//  private String wikiDbQueue = "wikidb.queue";

  @Override
  public void start(Future<Void> startFuture) throws Exception {

//    wikiDbQueue = config().getString(CONFIG_WIKIDB_QUEUE, "wikidb.queue");

    HttpServer server = vertx.createHttpServer();

    Router router = new VertxRouter(new DefaultAction(vertx)).getRouter();

    int portNumber = config().getInteger(CONFIG_HTTP_SERVER_PORT, 9180);

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

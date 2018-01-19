package com.shenmao.vertx.starter.verticle.demo;

import com.shenmao.vertx.starter.database.wikipage.WikiPageDbService;
import com.shenmao.vertx.starter.exceptions.PurposeException;
import com.shenmao.vertx.starter.passport.AuthHandlerImpl;
import com.shenmao.vertx.starter.serialization.ChainSerialization;
import com.shenmao.vertx.starter.serialization.SerializeOptions;
import com.shenmao.vertx.starter.serialization.SerializeType;
import com.shenmao.vertx.starter.sessionstore.RedisSessionStore;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.jwt.JWTOptions;
import io.vertx.ext.bridge.BridgeEventType;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.*;
import io.vertx.ext.web.handler.impl.TimeoutHandlerImpl;
import io.vertx.ext.web.handler.sockjs.BridgeEvent;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.web.sstore.SessionStore;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Set;

import static com.shenmao.vertx.starter.database.wikipage.WikiPageDatabaseVerticle.CONFIG_WIKIDB_QUEUE;

public class DemoVerticle extends AbstractVerticle {

  private static final int _PORT = 8081;
  public static final String _MOUNT_DIR = "/demo";

  private static final Logger logger = LoggerFactory.getLogger(DemoVerticle.class);

  @Override
  public void start() {


    Router router = Router.router(vertx);

    // Serving static resources
    router.route("/scripts/*").handler(StaticHandler.create("static/scripts").setCachingEnabled(false).setIncludeHidden(true).setDirectoryListing(true));
    router.route("/assets/*").handler(StaticHandler.create("static/assets").setCachingEnabled(false).setIncludeHidden(true).setDirectoryListing(true));
    router.route("/images/*").handler(StaticHandler.create("static/images").setCachingEnabled(false).setIncludeHidden(true).setDirectoryListing(true));
    router.route("/uploads/*").handler(StaticHandler.create("static/uploads").setCachingEnabled(false).setIncludeHidden(true).setDirectoryListing(true));

    // router.route().handler(TimeoutHandler.create(50000));
    // router.route().handler(VertxExceptionHandler.create(5000));
    // router.route().failureHandler(ErrorHandler.create(true));

    router.route(HttpMethod.GET, "/demo").handler(routingContext -> routingContext.reroute("/demo/index"));
    router.mountSubRouter("/demo", vertxDemoRouter());

    vertx.createHttpServer().requestHandler(router::accept).listen(_PORT);


  }


  public Router vertxDemoRouter() {

    Router router = DemoRouter.create(vertx, AuthHandlerImpl.create(vertx, DemoVerticle._MOUNT_DIR)).getRouter();

    vertx.exceptionHandler(new Handler<Throwable>() {
      @Override
      public void handle(Throwable throwable) {
//        System.out.println(throwable.getMessage() + ",,, vertx.exceptionHandler");
//        logger.error(throwable.getMessage());

      }

    });

    return router;
  }

}

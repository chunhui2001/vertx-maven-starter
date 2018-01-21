package com.shenmao.vertx.starter.verticle.demo;

import com.shenmao.vertx.starter.passport.AuthHandlerImpl;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.ext.web.handler.StaticHandler;

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

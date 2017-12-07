package com.shenmao.vertx.starter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.rjeschke.txtmark.Processor;
import com.shenmao.vertx.starter.actions.GlobalAction;
import com.shenmao.vertx.starter.routers.VertxRouter;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.templ.FreeMarkerTemplateEngine;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class MainVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);

  private JDBCClient dbClient;

  private static final String SQL_CREATE_PAGES_TABLE = "create table if not exists Pages (Id integer identity primary key, Name varchar(255) unique, Content clob)";
  private static final String SQL_GET_PAGE = "select Id, Name, Content from Pages where Id = ?";
  private static final String SQL_CREATE_PAGE = "insert into Pages values (NULL, ?, ?)";
  private static final String SQL_SAVE_PAGE = "update Pages set Content = ? where Id = ?";
  private static final String SQL_ALL_PAGES = "select Id, Name, Content from Pages";
  private static final String SQL_DELETE_PAGE = "delete from Pages where Id = ?";
  private static final String EMPTY_PAGE_MARKDOWN = "# A new page\n" +
                                                    "\n" +
                                                    "Feel-free to write in Markdown!\n";

  private final FreeMarkerTemplateEngine templateEngine = FreeMarkerTemplateEngine.create();






  @Override
  public void start(Future<Void> startFuture) {

    Future<Void> steps = new GlobalAction(vertx).prepareDatabase()
//                          .compose(v -> pourData())
                          .compose(v -> startHttpServer());

    steps.setHandler(ar -> {
      if (ar.succeeded()) {
        startFuture.complete();
      } else {
        startFuture.fail(ar.cause());
      }
    });

  }




  private Future<Void> startHttpServer() {

    Future<Void> future = Future.future();

    HttpServer server = vertx.createHttpServer();

    Router router = new VertxRouter(vertx).getRouter(vertx);

    server
      .requestHandler(router::accept)
      .listen(8080, ar -> {
      if (ar.succeeded()) {
        LOGGER.info("HTTP server running on port 8080");
        future.complete();
      } else {
        LOGGER.error("Could not start a HTTP server", ar.cause());
        future.fail(ar.cause());
      } });

    return future;

  }


}

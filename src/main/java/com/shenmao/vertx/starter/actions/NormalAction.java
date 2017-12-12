package com.shenmao.vertx.starter.actions;

import com.shenmao.vertx.starter.database.WikiDatabaseService;
import com.shenmao.vertx.starter.database.WikiDatabaseVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

public class NormalAction {

  private static final Logger LOGGER = LoggerFactory.getLogger(NormalAction.class);
  private static final String wikiDbQueue = WikiDatabaseVerticle.CONFIG_WIKIDB_QUEUE;

  private WikiDatabaseService dbService;

  private Vertx _vertx;

  public NormalAction(Vertx vertx) {
    _vertx = vertx;
    dbService = WikiDatabaseService.createProxy(vertx, wikiDbQueue);
  }

  public void loginHandler(RoutingContext context) {

    if (context.user() != null) {
      context.response().setStatusCode(303);
      context.response().putHeader("Location", "/index");
      context.response().end();
      return;
    }

    boolean error = context.queryParams().contains("error");

    context.put("title", "Login");
    context.put("error", error);

    ContextResponse.write(context, "/login.ftl");


  }


}

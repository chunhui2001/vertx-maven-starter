package com.shenmao.vertx.starter.actions;

import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;

public interface Action {

  public default void staticHandler(RoutingContext context) {
    context.request().response().sendFile(context.request().path().substring(1));
  }
  public void indexHandler(RoutingContext context);
  public void pageUpdateHandler(RoutingContext context);
  public void pageDeletionHandler(RoutingContext context);
  public void pageCreateHandler(RoutingContext context);
  public void pageRenderingHandler(RoutingContext context);
  public void backupHandler(RoutingContext context);

  public Vertx getVertx();

}

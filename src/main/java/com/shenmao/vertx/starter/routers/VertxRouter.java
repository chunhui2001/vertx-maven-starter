package com.shenmao.vertx.starter.routers;

import com.shenmao.vertx.starter.actions.GlobalAction;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class VertxRouter {

  GlobalAction _globalAction;

  public VertxRouter (Vertx vertx) {
    _globalAction = new GlobalAction(vertx);
  }

  public Router getRouter(Vertx vertx) {

    Router router = Router.router(vertx);

    router.get("/static/*").handler(_globalAction::staticHandler);

    router.get("/").handler(_globalAction::indexHandler);
    router.get("/wiki/:id").handler(_globalAction::pageRenderingHandler);
    router.post().handler(BodyHandler.create());
    router.post("/save").handler(_globalAction::pageUpdateHandler);
    router.post("/create").handler(_globalAction::pageCreateHandler);
    router.post("/delete").handler(_globalAction::pageDeletionHandler);

    return router;

  }
}

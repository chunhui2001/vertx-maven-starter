package com.shenmao.vertx.starter.routers;

import com.shenmao.vertx.starter.actions.Action;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class VertxRouter {

  Action _action;

  public VertxRouter (Action action) {
    _action = action;
  }

  public Router getRouter() {

    Router router = Router.router(_action.getVertx());

    router.get("/static/*").handler(_action::staticHandler);

    router.get("/").handler(_action::indexHandler);
    router.get("/wiki/:id").handler(_action::pageRenderingHandler);
    router.post().handler(BodyHandler.create());
    router.post("/save").handler(_action::pageUpdateHandler);
    router.post("/create").handler(_action::pageCreateHandler);
    router.post("/delete").handler(_action::pageDeletionHandler);

    return router;

  }
}

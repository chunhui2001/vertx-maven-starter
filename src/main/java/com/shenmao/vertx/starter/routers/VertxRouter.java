package com.shenmao.vertx.starter.routers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shenmao.vertx.starter.actions.Action;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    router.get("/jsontest").handler(rc -> {

      JsonArray array = new JsonArray();

      array.add(new JsonObject().put("greeting", "长成会asdf").put("name", (String) null));
      array.add(new JsonObject().put("greeting", "长成会asdf1"));
      array.add(new JsonObject().put("greeting", "长成会asdf2"));
      array.add(new JsonObject().put("greeting", "长成会asdf3"));
      array.add(new JsonObject().put("greeting", "长成会asdf4").put("isLock", false));

      JsonObject result = new JsonObject().put("error", false).put("data", array);

      rc.response()
        .putHeader("content-type", "application/json")
        .end(result.encode());

    });

    return router;

  }
}

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

    router.get("/").handler(rc -> {
      rc.response().setStatusCode(303);
      rc.response().putHeader("Location", "/index");
      rc.response().end();
    });

//    router.get("/index").handler(_action::indexHandler);
    router.routeWithRegex("/index(.json|.html|.xml)?").handler(_action::indexHandler);
//    router.get("/index.html").handler(_action::indexHandler);
    router.get("/wiki/:id").handler(_action::pageRenderingHandler);
    router.post().handler(BodyHandler.create());
    router.post("/save").handler(_action::pageUpdateHandler);
    router.post("/create").handler(_action::pageCreateHandler);
    router.post("/delete").handler(_action::pageDeletionHandler);
    router.get("/backup").handler(_action::backupHandler);


    /*Router apiRouter = Router.router(_action.getVertx());
    apiRouter.get("/pages").handler(this::apiRoot);
    apiRouter.get("/pages/:id").handler(this::apiGetPage);
    apiRouter.post().handler(BodyHandler.create());
    apiRouter.post("/pages").handler(this::apiCreatePage);
    apiRouter.put().handler(BodyHandler.create());
    apiRouter.put("/pages/:id").handler(this::apiUpdatePage);
    apiRouter.delete("/pages/:id").handler(this::apiDeletePage);

    router.mountSubRouter("/api", apiRouter);
    */



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

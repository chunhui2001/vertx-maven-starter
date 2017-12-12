package com.shenmao.vertx.starter.routers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shenmao.vertx.starter.actions.Action;
import com.shenmao.vertx.starter.actions.DefaultAction;
import com.shenmao.vertx.starter.actions.NormalAction;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.shiro.ShiroAuth;
import io.vertx.ext.auth.shiro.ShiroAuthOptions;
import io.vertx.ext.auth.shiro.ShiroAuthRealmType;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.*;
//import io.vertx.ext.web.handler.impl.FormLoginHandlerImpl;
import com.shenmao.vertx.starter.passport.FormLoginHandlerImpl;
import io.vertx.ext.web.sstore.LocalSessionStore;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.realm.Realm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VertxRouter {

  NormalAction _normalAction ;
  DefaultAction _defaultAction ;
  Vertx _vertx;
  AuthProvider _auth;
  AuthHandler _authHandler;

  public VertxRouter (Vertx vertx) {

    _vertx = vertx;

    _defaultAction = new DefaultAction(vertx);
    _normalAction = new NormalAction(vertx);

    _auth = ShiroAuth.create(_vertx, new ShiroAuthOptions()
      .setType(ShiroAuthRealmType.PROPERTIES)
      .setConfig(new JsonObject()
        .put("properties_path", "classpath:properties/web-users.properties")));



    System.out.println(RedirectAuthHandler.DEFAULT_LOGIN_REDIRECT_URL + ", DEFAULT_LOGIN_REDIRECT_URL");
    System.out.println(RedirectAuthHandler.DEFAULT_RETURN_URL_PARAM + ", DEFAULT_RETURN_URL_PARAM");

//    Realm realm = null;
//    DefaultSecurityManager securityManager = new DefaultSecurityManager(realm);
//    SecurityUtils.setSecurityManager(securityManager);

  }

  public Router getRouter() {

    Router router = Router.router(_defaultAction.getVertx());

    router.get("/static/*").handler(_defaultAction::staticHandler);

    router.get("/").handler(rc -> {
      rc.response().setStatusCode(303);
      rc.response().putHeader("Location", "/index");
      rc.response().end();
    });

    router.route().handler(CookieHandler.create());
    router.route().handler(BodyHandler.create());
    router.route().handler(SessionHandler.create(LocalSessionStore.create(_vertx)));
    router.route().handler(UserSessionHandler.create(_auth));


    _authHandler = RedirectAuthHandler.create(_auth, "/login");



    router.routeWithRegex("/index(.json|.html|.xml)?").handler(_authHandler);
    router.route("/wiki/*").handler(_authHandler);
    router.route("/save/*").handler(_authHandler);
    router.route("/create/*").handler(_authHandler);
    router.route("/delete/*").handler(_authHandler);
    router.route("/backup/*").handler(_authHandler);

    router.routeWithRegex("/index(.json|.html|.xml)?").handler(_defaultAction::indexHandler);
    router.get("/wiki/:id").handler(_defaultAction::pageRenderingHandler);
    router.post("/save").handler(_defaultAction::pageUpdateHandler);
    router.post("/create").handler(_defaultAction::pageCreateHandler);
    router.post("/delete").handler(_defaultAction::pageDeletionHandler);
    router.get("/backup").handler(_defaultAction::backupHandler);

    router.get("/login").handler(_normalAction::loginHandler);
    router.post("/login-auth").handler(new FormLoginHandlerImpl(_auth, "username", "password", "return_url", "/index"));


    router.get("/logout").handler(context -> {
      context.clearUser();
      context.response()
        .setStatusCode(302)
        .putHeader("Location", "/") .end();
    });



    return router;

  }
}

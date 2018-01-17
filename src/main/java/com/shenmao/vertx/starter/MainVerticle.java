package com.shenmao.vertx.starter;

import com.shenmao.vertx.starter.exceptions.PurposeException;
import com.shenmao.vertx.starter.passport.AuthHandlerImpl;
import com.shenmao.vertx.starter.serialization.*;
import com.shenmao.vertx.starter.sessionstore.RedisSessionStore;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.jwt.JWTOptions;
import io.vertx.ext.auth.shiro.ShiroAuth;
import io.vertx.ext.auth.shiro.ShiroAuthOptions;
import io.vertx.ext.auth.shiro.ShiroAuthRealmType;
import io.vertx.ext.bridge.BridgeEventType;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.*;
import io.vertx.ext.web.handler.sockjs.BridgeEvent;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Set;
import java.util.regex.Pattern;

public class MainVerticle extends AbstractVerticle {

  private static final Logger logger = LoggerFactory.getLogger(MainVerticle.class);

  @Override
  public void start() {

    vertx.deployVerticle("com.shenmao.vertx.starter.verticle.demo.DemoVerticle", res -> {
      if (res.succeeded()) {
        logger.info("Successful DemoVerticle deployment id is: " + res.result());
      } else {
        logger.error("Failed DemoVerticle deployment failed!");
      }
    });


  }





}

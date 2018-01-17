package com.shenmao.vertx.starter.verticle.demo;

import com.shenmao.vertx.starter.passport.AuthHandlerImpl;
import com.shenmao.vertx.starter.serialization.ChainSerialization;
import com.shenmao.vertx.starter.serialization.SerializeType;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.jwt.JWTOptions;
import io.vertx.ext.bridge.BridgeEventType;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.sockjs.BridgeEvent;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;

import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Set;

public class DefaultHandlers {

  private static final Logger logger = LoggerFactory.getLogger(DefaultHandlers.class);

  public static DefaultHandlers create(String mountDir) {
    return new DefaultHandlers(mountDir);
  }

  private String mountDir;

  public DefaultHandlers(String mountDir) {
    this.mountDir = mountDir;
  }

  public void accessToken(RoutingContext routingContext) {

    String username = null;
    String password = null;

    if (routingContext.getBodyAsString().isEmpty()) {
      username = routingContext.request().getParam("username");
      password = routingContext.request().getParam("password");
    } else {
      JsonObject jsonObject = routingContext.getBodyAsJson();
      username = jsonObject.getString("username");
      password = jsonObject.getString("password");
    }

    JsonObject jsonObject = new JsonObject()
      .put("username", username)
      .put("password", password);

    AuthHandlerImpl.getAuthProvider().authenticate(jsonObject, userAsyncResult -> {

      if (userAsyncResult.succeeded()) {

        // curl -u keesh:keesh http://localhost:8081/private/admin
        // curl -v -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJmb28iOiJiYXIgYmFyIiwiaWF0IjoxNTE1NDAzODY4fQ.wpsSj9sfborJvYVVVycUSnUSJSB52G8klrmgsStSm4Q" http://localhost:8081/private/admin

        routingContext.response().end(
          AuthHandlerImpl.getJWTAuthProvider().generateToken(new JsonObject().put("foo", "bar bar"), new JWTOptions()));
      } else {
        routingContext.fail(401);
      }

    });

  }

  public void login(RoutingContext routingContext) {

    JsonObject contextData = new JsonObject();

    ChainSerialization.create(routingContext)
      .putViewName("/login.html")
      .putContextData(contextData)
      .serialize();
  }

  public void logout(RoutingContext routingContext) {
    if (routingContext.user() != null) routingContext.clearUser();
    routingContext.response().setStatusCode(302).putHeader("Location", mountDir + "/").end();
  }

  public void catalogue(RoutingContext routingContext) {
    String productType = routingContext.request().getParam("param0");
    String productId = routingContext.request().getParam("param1");
    routingContext.response().putHeader("Content-Type", "text/html;charset=UTF-8");
    routingContext.response().end("productType: <b>"+productType+"</b> <br/>productId: <b>"+productId+"</b> ");
  }

  public void articles(RoutingContext routingContext) {

    String productType = routingContext.request().getParam("param0");
    String productId = routingContext.request().getParam("param1");

    JsonObject contextData = new JsonObject()
      .put("productType", productType)
      .put("productId", productId);

    ChainSerialization.create(routingContext)
      .putViewName("/articles/article_detail.html")
      .putContextData(contextData)
      .serialize();

  }

  public void uploadFiles(RoutingContext routingContext) {
    Set<FileUpload> uploads = routingContext.fileUploads();
    routingContext.response().end("<" + uploads.size() + "> files uploaded");
  }

  public void dashboard(RoutingContext routingContext) {
    JsonObject contextData = new JsonObject();

    ChainSerialization.create(routingContext)
      .putViewName("/dashboard/dashboard_index.html")
      .putContextData(contextData)
      .serialize();
  }

  public void chatRoom(RoutingContext routingContext) {

    ChainSerialization.create(routingContext)
      .putViewName("/chat_room/chat_room_index.html")
      .putContextData(new JsonObject())
      .serialize();
  }

  public void sendMessage(RoutingContext routingContext) {

    String message = routingContext.getBodyAsJson().getString("message");

    JsonObject contextData = new JsonObject()
      .put("username", routingContext.user().principal().getString("username"))
      .put("message", message )
      .put("dateTime", (new Timestamp(System.currentTimeMillis())).getTime());

    ChainSerialization.create(routingContext)
      .setSerializeType(SerializeType.WS)
      .setWsEndpoint("chat_room.1")
      .putContextData(contextData)
      .serialize();

  }


  public SockJSHandler eventBusHandler(Vertx vertx, String endpoint) {

    BridgeOptions options = new BridgeOptions()
      .addOutboundPermitted(new PermittedOptions().setAddressRegex(endpoint));

    Handler<BridgeEvent> bridgeEventHandler = event -> {

      /* if (event.type() == BridgeEventType.SOCKET_CREATED) {
        logger.info("A socket was created");
      } */

        /* JsonObject rawMessage = event.getRawMessage();

        // put some headers
        event.setRawMessage(rawMessage);

        JsonObject body = new JsonObject(rawMessage.getString("body"));

        // update body and send to client
        // TODO

        event.socket().write(rawMessage.encode()); */

      /* if (event.type() != BridgeEventType.SOCKET_PING) {
        System.out.println(event.type() + ", event.type()");
      } */

      if (event.type() == BridgeEventType.SOCKET_CLOSED) {
        // TODO
      }

      event.complete(true);

    };

    return SockJSHandler.create(vertx).bridge(options, bridgeEventHandler);

  }

}

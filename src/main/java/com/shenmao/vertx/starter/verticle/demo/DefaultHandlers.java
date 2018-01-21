package com.shenmao.vertx.starter.verticle.demo;

import com.shenmao.vertx.starter.database.wikipage.WikiPageDatabaseVerticle;
import com.shenmao.vertx.starter.database.wikipage.WikiPageDbService;
import com.shenmao.vertx.starter.exceptions.PurposeException;
import com.shenmao.vertx.starter.passport.AuthHandlerImpl;
import com.shenmao.vertx.starter.serialization.ChainSerialization;
import com.shenmao.vertx.starter.serialization.SerializeType;
import io.vertx.core.Handler;
import io.vertx.rxjava.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.jwt.JWTOptions;
import io.vertx.ext.bridge.BridgeEventType;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.FileUpload;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.handler.sockjs.BridgeEvent;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.rxjava.ext.web.handler.sockjs.SockJSHandler;
import rx.Single;

import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class DefaultHandlers {

  private static final Logger logger = LoggerFactory.getLogger(DefaultHandlers.class);

  public static DefaultHandlers create(WikiPageDbService wikiPageDbService, String mountDir) {

    return new DefaultHandlers(wikiPageDbService, mountDir);
  }

  WikiPageDbService wikiPageDbService;
  private String mountDir;

  public DefaultHandlers(WikiPageDbService wikiPageDbService, String mountDir) {
    this.wikiPageDbService = wikiPageDbService;
    this.mountDir = mountDir;
  }

  public void indexHandler(RoutingContext routingContext) {


    ChainSerialization.create(routingContext.getDelegate())
      .putViewName("/index.html")
      .putContextData(null)
      .serialize();
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

    AuthHandlerImpl.getAuthProvider().rxAuthenticate(jsonObject).flatMap(user -> {

      Single<Boolean> createSingle = user.rxIsAuthorised("create");
      Single<Boolean> updateSingle = user.rxIsAuthorised("update");
      Single<Boolean> deleteSingle = user.rxIsAuthorised("delete");

      return Single.zip(createSingle,updateSingle, deleteSingle, (canCreate, canUpdate, canDelete) -> {

        JsonObject userObject = new JsonObject()
          .put("username", user.principal().getString("username"))
          .put("canCreate", canCreate)
          .put("canUpdate", canUpdate)
          .put("canDelete", canDelete);

        JWTOptions jwtOptions = new JWTOptions().setSubject("Wiki API").setIssuer("Vert.x");

        return AuthHandlerImpl.getJWTAuthProvider().generateToken(userObject, jwtOptions);
      });

    }).subscribe(token -> {
      routingContext.response().putHeader("Context-Type", "text/plain").end(token);
    }, t -> routingContext.fail(401));

  }

  public void login(RoutingContext routingContext) {

    JsonObject contextData = new JsonObject();

    ChainSerialization.create(routingContext.getDelegate())
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

    ChainSerialization.create(routingContext.getDelegate())
      .putViewName("/articles/article_detail.html")
      .putContextData(contextData)
      .serialize();

  }

  public void uploadFiles(RoutingContext routingContext) {
    Set<FileUpload> uploads = routingContext.getDelegate().fileUploads();
    routingContext.response().end("<" + uploads.size() + "> files uploaded");
  }

  public void dashboard(RoutingContext routingContext) {
    JsonObject contextData = new JsonObject();

    ChainSerialization.create(routingContext.getDelegate())
      .putViewName("/dashboard/dashboard_index.html")
      .putContextData(contextData)
      .serialize();
  }

  public void chatRoom(RoutingContext routingContext) {

    ChainSerialization.create(routingContext.getDelegate())
      .putViewName("/chat_room/chat_room_index.html")
      .putContextData(new JsonObject())
      .serialize();
  }

  public void wikiPage(RoutingContext routingContext) {

    wikiPageDbService.fetchAllPages( reply -> {

      if (reply.succeeded()) {


        List<JsonObject> wikiPageList = reply.result();

        ChainSerialization.create(routingContext.getDelegate())
          .putViewName("/wiki_page/wiki_page_index.html")
          .putContextData(wikiPageList)
          .serialize();

      } else {
        logger.error(reply.cause());
      }

    });


  }

  public void sendMessage(RoutingContext routingContext) {

    String message = routingContext.getBodyAsJson().getString("message");

    JsonObject contextData = new JsonObject()
      .put("username", routingContext.user().principal().getString("username"))
      .put("message", message )
      .put("dateTime", (new Timestamp(System.currentTimeMillis())).getTime());

    ChainSerialization.create(routingContext.getDelegate())
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

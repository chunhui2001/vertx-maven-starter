package com.shenmao.vertx.starter.verticle.demo;

import com.shenmao.vertx.starter.database.wikipage.WikiPageDbService;
import com.shenmao.vertx.starter.exceptions.PurposeException;
import com.shenmao.vertx.starter.passport.AuthHandlerImpl;
import com.shenmao.vertx.starter.serialization.ChainSerialization;
import com.shenmao.vertx.starter.serialization.SerializeOptions;
import com.shenmao.vertx.starter.serialization.SerializeType;
import com.shenmao.vertx.starter.sessionstore.RedisSessionStore;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.jwt.JWTOptions;
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
import io.vertx.ext.web.sstore.SessionStore;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Set;

import static com.shenmao.vertx.starter.database.wikipage.WikiPageDatabaseVerticle.CONFIG_WIKIDB_QUEUE;

public class DemoVerticle extends AbstractVerticle {

  private static final int _PORT = 8081;
  private static final String _MOUNT_DIR = "/demo";

  private static final Logger logger = LoggerFactory.getLogger(DemoVerticle.class);

  DemoHandlers demoHandlers = null;
  DefaultHandlers defaultHandlers = null;
  WikiPageDbService wikiPageDbService = null;

  @Override
  public void start() {

    wikiPageDbService = WikiPageDbService.createProxy(vertx, CONFIG_WIKIDB_QUEUE);

    demoHandlers = DemoHandlers.create(_MOUNT_DIR);
    defaultHandlers = DefaultHandlers.create(wikiPageDbService, _MOUNT_DIR);

    Router router = Router.router(vertx);

    // Serving static resources
    router.route("/scripts/*").handler(StaticHandler.create("static/scripts").setCachingEnabled(false).setIncludeHidden(true).setDirectoryListing(true));
    router.route("/assets/*").handler(StaticHandler.create("static/assets").setCachingEnabled(false).setIncludeHidden(true).setDirectoryListing(true));
    router.route("/images/*").handler(StaticHandler.create("static/images").setCachingEnabled(false).setIncludeHidden(true).setDirectoryListing(true));
    router.route("/uploads/*").handler(StaticHandler.create("static/uploads").setCachingEnabled(false).setIncludeHidden(true).setDirectoryListing(true));

    router.route(HttpMethod.GET, "/demo").handler(routingContext -> routingContext.reroute("/demo/index"));
    router.mountSubRouter("/demo", vertxDemoRouter());


    vertx.createHttpServer().requestHandler(router::accept).listen(_PORT);

  }


  public Router vertxDemoRouter() {

    final Router vertxRouter = Router.router(vertx);

    AuthHandler authHandler = AuthHandlerImpl.create(vertx, _MOUNT_DIR);
    demoHandlers.wrapHandler(vertx, vertxRouter);

    /***** routers *****/
    vertxRouter.route("/eventbus/*").handler(defaultHandlers.eventBusHandler(vertx, "chat_room\\.[0-9]+"));

    // 基于权限验证的 router
    vertxRouter.route("/dashboard/*").handler(authHandler);
    vertxRouter.route("/chat-room/*").handler(authHandler);

    // 首页
    vertxRouter.route(HttpMethod.GET, "/").handler(routingContext -> routingContext.reroute("/index"));
    vertxRouter.routeWithRegex(HttpMethod.GET, "/index" + DemoHandlers._SUPPORT_EXTS_PATTERN).handler(defaultHandlers::indexHandler);

    // curl -v -X POST http://localhost:8081/ping
    Route routePingPost =  vertxRouter.route(HttpMethod.POST, "/ping");
    Route routePingGet =  vertxRouter.routeWithRegex(HttpMethod.GET, "/ping|/");

    routePingGet.handler(routingContext -> routingContext.fail(406));
    routePingPost.handler(defaultHandlers::indexHandler);

    // 基于路径参数的 router
    //Route routeProduct = vertxRouter.route(HttpMethod.GET, "/catalogue/products/:productType/:productId");
    vertxRouter
      .routeWithRegex(HttpMethod.GET, "/catalogue/products/(?<productType>[^\\/.]+)/(?<productId>[^\\/.]+)" + DemoHandlers._SUPPORT_EXTS_PATTERN)
      .handler(defaultHandlers::catalogue);

    // 基于正则表达式的 router
    // Route routeArticle = vertxRouter.routeWithRegex(HttpMethod.GET, "/articles/channel/(?<articleType>[^\\/]+)/(?<articleTag>[^\\/]+)(.json|.xml)?");
    vertxRouter
      .routeWithRegex(HttpMethod.GET, "/articles/channel/(?<articleType>[^\\/.]+)/(?<articleTag>[^\\/.]+)" + DemoHandlers._SUPPORT_EXTS_PATTERN)
      .handler(defaultHandlers::articles);

    /* curl \
    -F "userid=6887" \
    -F "filecomment=This is an image file" \
    -F "image=@/home/keesh/Desktop/timg.jpg" http://localhost:8081/file_uploads */
    vertxRouter.routeWithRegex(HttpMethod.POST, "/file_uploads" + DemoHandlers._SUPPORT_EXTS_PATTERN)
        .handler(defaultHandlers::uploadFiles);

    // curl -u keesh:keesh http://localhost:8081/dashboard
    vertxRouter.route("/dashboard").handler(defaultHandlers::dashboard);
    vertxRouter.route("/chat-room").handler(defaultHandlers::chatRoom);

    vertxRouter.route(HttpMethod.POST, "/chat-room/send-message")
               .consumes("*/json").handler(routingContext -> {
      /* JsonObject j1 = new JsonObject().put("username", "u1").put("message", "6ttt").put("dateTime", 1516180321963L);
      JsonObject j2 = new JsonObject().put("code", 200).put("message", "OK").put("data", j1);
      vertx.eventBus().publish("chat_room.1", j2); */
      defaultHandlers.sendMessage(routingContext);
    });

    vertxRouter.route(HttpMethod.GET, "/signout").handler(defaultHandlers::logout);
    vertxRouter.routeWithRegex(HttpMethod.POST, "/login-auth").handler(CSRFHandler.create("csrf secret here"));
    vertxRouter.route(HttpMethod.GET, "/login")
      .handler(CSRFHandler.create("csrf secret here"))
      .handler(defaultHandlers::login);

    // JWT access token
    // curl -v -X POST -F "username=keesh" -F "password=keesh" http://localhost:8081/access_token
    // curl -v -X POST -H "Content-Type:application/x-www-form-urlencoded" -d '{"username": "keesh", "password": "keesh"}' http://localhost:8081/access_token
    // curl -v -X POST -H "Content-Type:application/json" -d '{"username": "keesh", "password": "keesh"}' http://localhost:8081/access_token
    vertxRouter.routeWithRegex(HttpMethod.POST, "/access_token" + DemoHandlers._SUPPORT_EXTS_PATTERN)
               .handler(defaultHandlers::accessToken);

    vertxRouter.routeWithRegex(HttpMethod.POST,"/login-auth" + DemoHandlers._SUPPORT_EXTS_PATTERN)
      .handler(FormLoginHandler.create(AuthHandlerImpl.getAuthProvider(),
        "username", "password", "return_url", _MOUNT_DIR + "/"));

    // throw error purpose
    vertxRouter.routeWithRegex(HttpMethod.GET, "/throw" + DemoHandlers._SUPPORT_EXTS_PATTERN).handler(routingContext -> {
      throw new PurposeException();
      //throw new PurposeException("Throw an exception purpose!");
    });

    // 404 notfound page router
    vertxRouter.routeWithRegex(HttpMethod.GET, "/not-found" + DemoHandlers._SUPPORT_EXTS_PATTERN).handler(routingContext -> {
      routingContext.fail(404);
    });

    // 500 handler
    vertxRouter.routeWithRegex(HttpMethod.GET, "/error" + DemoHandlers._SUPPORT_EXTS_PATTERN).handler(demoHandlers::errorPageHandler);

    // not have route matchs
    vertxRouter.route("/*").handler(demoHandlers::notFoundHandler);

    return vertxRouter;
  }

}

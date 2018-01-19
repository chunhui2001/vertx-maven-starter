package com.shenmao.vertx.starter.verticle.demo;

import com.shenmao.vertx.starter.database.wikipage.WikiPageDbService;
import com.shenmao.vertx.starter.exceptions.PurposeException;
import com.shenmao.vertx.starter.passport.AuthHandlerImpl;
import com.shenmao.vertx.starter.sessionstore.RedisSessionStore;
import io.vertx.core.VertxException;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.sstore.SessionStore;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.*;

import static com.shenmao.vertx.starter.database.wikipage.WikiPageDatabaseVerticle.CONFIG_WIKIDB_QUEUE;

public class DemoRouter {

  private static final int KB = 1024;
  private static final int MB = 1024 * KB;

  public static DemoRouter create(Vertx vertx, AuthHandler authHandler) {
    return new DemoRouter(vertx, authHandler);
  }

  Vertx vertx;
  Router router;

  AuthHandler authHandler = null;
  DemoHandlers demoHandlers = null;
  DefaultHandlers defaultHandlers = null;
  WikiPageDbService wikiPageDbService = null;

  public DemoRouter(Vertx vertx, AuthHandler authHandler) {

    this.vertx = vertx;
    this.router = Router.router(vertx);
    this.authHandler = authHandler;

    wikiPageDbService = WikiPageDbService.createProxy(vertx, CONFIG_WIKIDB_QUEUE);
    demoHandlers = DemoHandlers.create(DemoVerticle._MOUNT_DIR);
    defaultHandlers = DefaultHandlers.create(wikiPageDbService, DemoVerticle._MOUNT_DIR);

    this.init();

  }

  private void init() {

    this.wrapRouterHandler(vertx, this.router);

    /***** routers *****/
    this.router.route("/eventbus/*").handler(defaultHandlers.eventBusHandler(vertx, "chat_room\\.[0-9]+"));

    // 基于权限验证的 router
    this.router.route("/dashboard/*").handler(authHandler);
    this.router.route("/chat-room/*").handler(authHandler);

    this.indexRouter();
    this.pingRouter();
    this.mainRouter();
    this.labelRouter();
    this.fileUploadRouter();
    this.chatRoomRouter();
    this.wikiPageRouter();
    this.dashboardRouter();
    this.globalRouter();

  }

  private void indexRouter() {

    // 首页
    this.router.route(HttpMethod.GET, "/").handler(routingContext -> routingContext.reroute("/index"));
    this.router.routeWithRegex(HttpMethod.GET, "/index" + DemoHandlers._SUPPORT_EXTS_PATTERN).handler(defaultHandlers::indexHandler);

  }

  private void pingRouter() {

    // curl -v -X POST http://localhost:8081/ping
    Route routePingPost =  this.router.route(HttpMethod.POST, "/ping");
    Route routePingGet =  this.router.routeWithRegex(HttpMethod.GET, "/ping|/");

    routePingGet.handler(routingContext -> routingContext.fail(406));
    routePingPost.handler(defaultHandlers::indexHandler);

  }

  private void mainRouter() {

    this.router.route(HttpMethod.GET, "/signout").handler(defaultHandlers::logout);
    this.router.routeWithRegex(HttpMethod.POST, "/login-auth").handler(CSRFHandler.create("csrf secret here"));
    this.router.route(HttpMethod.GET, "/login")
      .handler(CSRFHandler.create("csrf secret here"))
      .handler(defaultHandlers::login);

    // JWT access token
    // curl -v -X POST -F "username=keesh" -F "password=keesh" http://localhost:8081/access_token
    // curl -v -X POST -H "Content-Type:application/x-www-form-urlencoded" -d '{"username": "keesh", "password": "keesh"}' http://localhost:8081/access_token
    // curl -v -X POST -H "Content-Type:application/json" -d '{"username": "keesh", "password": "keesh"}' http://localhost:8081/access_token
    this.router.routeWithRegex(HttpMethod.POST, "/access_token" + DemoHandlers._SUPPORT_EXTS_PATTERN)
      .handler(defaultHandlers::accessToken);

    this.router.routeWithRegex(HttpMethod.POST,"/login-auth" + DemoHandlers._SUPPORT_EXTS_PATTERN)
      .handler(FormLoginHandler.create(AuthHandlerImpl.getAuthProvider(),
        "username", "password", "return_url", DemoVerticle._MOUNT_DIR + "/"));

  }

  private void labelRouter() {

    // 基于路径参数的 router
    //Route routeProduct = this.router.route(HttpMethod.GET, "/catalogue/products/:productType/:productId");
    this.router
      .routeWithRegex(HttpMethod.GET, "/catalogue/products/(?<productType>[^\\/.]+)/(?<productId>[^\\/.]+)" + DemoHandlers._SUPPORT_EXTS_PATTERN)
      .handler(defaultHandlers::catalogue);

    // 基于正则表达式的 router
    // Route routeArticle = this.router.routeWithRegex(HttpMethod.GET, "/articles/channel/(?<articleType>[^\\/]+)/(?<articleTag>[^\\/]+)(.json|.xml)?");
    this.router
      .routeWithRegex(HttpMethod.GET, "/articles/channel/(?<articleType>[^\\/.]+)/(?<articleTag>[^\\/.]+)" + DemoHandlers._SUPPORT_EXTS_PATTERN)
      .handler(defaultHandlers::articles);

  }

  private void fileUploadRouter() {
    /* curl \
    -F "userid=6887" \
    -F "filecomment=This is an image file" \
    -F "image=@/home/keesh/Desktop/timg.jpg" http://localhost:8081/file_uploads */
    this.router.routeWithRegex(HttpMethod.POST, "/file_uploads" + DemoHandlers._SUPPORT_EXTS_PATTERN)
      .handler(defaultHandlers::uploadFiles);
  }

  private void chatRoomRouter() {

    this.router.route("/chat-room").handler(defaultHandlers::chatRoom);

    this.router.route(HttpMethod.POST, "/chat-room/send-message")
      .consumes("*/json").handler(routingContext -> {
      /* JsonObject j1 = new JsonObject().put("username", "u1").put("message", "6ttt").put("dateTime", 1516180321963L);
      JsonObject j2 = new JsonObject().put("code", 200).put("message", "OK").put("data", j1);
      vertx.eventBus().publish("chat_room.1", j2); */
      defaultHandlers.sendMessage(routingContext);
    });

  }

  private void wikiPageRouter() {

    this.router.route("/wiki-page").handler(defaultHandlers::wikiPage);
  }

  private void dashboardRouter() {
    // curl -u keesh:keesh http://localhost:8081/dashboard
    this.router.route("/dashboard").handler(defaultHandlers::dashboard);
  }

  private void globalRouter() {

    // throw error purpose
    this.router.routeWithRegex(HttpMethod.GET, "/throw" + DemoHandlers._SUPPORT_EXTS_PATTERN).handler(routingContext -> {
      throw new PurposeException();
      //throw new PurposeException("Throw an exception purpose!");
    });

    // 404 notfound page router
    this.router.routeWithRegex(HttpMethod.GET, "/not-found" + DemoHandlers._SUPPORT_EXTS_PATTERN).handler(routingContext -> {
      routingContext.fail(404);
    });

    // 500 handler
    this.router.routeWithRegex(HttpMethod.GET, "/error" + DemoHandlers._SUPPORT_EXTS_PATTERN).handler(demoHandlers::errorPageHandler);

    // not have route matchs
    this.router.route("/*").handler(demoHandlers::notFoundHandler);
  }


  public void wrapRouterHandler(Vertx vertx, Router router) {

    //SessionStore sessionStore = LocalSessionStore.create(vertx, "myapp.sessionmap", 10000);
    SessionStore sessionStore = RedisSessionStore
      .create(vertx, "myapp.sessionmap", 10000)
      .host("127.0.0.1").port(6379);
    // Route: consumes and produces
    // routeInstance.consumes("text/html").consumes("text/plain").consumes("*/json");
    // routeInstance.produces("application/json");
    // etc..

    // vertxRouter.route().handler(CorsHandler.create("vertx\\.io").allowedMethod(HttpMethod.GET));
    router.route().handler(ResponseTimeHandler.create());
    // vertxRouter.route().handler(ResponseContentTypeHandler.create());
    router.route().handler(FaviconHandler.create());
    router.route().handler(CookieHandler.create());                      // Cookie cookie = routingContext.getCookie("cookie name here")
    router.route().handler(BodyHandler.create().setBodyLimit(50 * MB).setUploadsDirectory("uploads"));                        // file upload and body parameters parser
    router.route().handler(SessionHandler.create(sessionStore).setCookieHttpOnlyFlag(false));
    //.setCookieSecureFlag(true));         // Session session = routingContext.session(); session.put("foo", "bar");
    //vertxRouter.route().handler(CSRFHandler.create("not a good secret"));
    router.route().handler(UserSessionHandler.create(AuthHandlerImpl.getAuthProvider()));
    router.route().failureHandler(routingContext ->  {

      if (routingContext.failure() instanceof VertxException
        && routingContext.failure().getMessage().equals("Connection was closed")) {
        return;
      }

      demoHandlers.exceptionHandler(routingContext);

    });

    router.route().handler(routingContext -> {
      // middlewares here
      routingContext.next();
    });

    // default Serving
    router.route().handler(routingContext -> {
      demoHandlers.htmlRenderHandler(routingContext);
    });

    // Serving .html .json and .xml
    router.routeWithRegex(".+\\.html").handler(demoHandlers::htmlRenderHandler);
    router.routeWithRegex(".+\\.json").handler(demoHandlers::jsonSerializationHandler);
    router.routeWithRegex(".+\\.xml").handler(demoHandlers::xmlSerializationHandler);

    // not support exts
    router.route().handler(demoHandlers::notSupportExtensionMiddleware);

    // tester
    router.routeWithRegex(HttpMethod.GET, "/pip" + DemoHandlers._SUPPORT_EXTS_PATTERN)
      .handler(routingContext -> routingContext.put("count", 1).next())
      .handler(routingContext -> routingContext.response().end("pipline: " + ((int)routingContext.get("count") + 1)));

  }

  public Router getRouter() {
    return router;
  }

}

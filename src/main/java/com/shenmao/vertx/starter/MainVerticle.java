package com.shenmao.vertx.starter;

import com.shenmao.vertx.starter.exceptions.PurposeException;
import com.shenmao.vertx.starter.passport.AuthHandlerImpl;
import com.shenmao.vertx.starter.serialization.*;
import com.shenmao.vertx.starter.sessionstore.RedisSessionStore;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTOptions;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.*;
import io.vertx.ext.web.sstore.SessionStore;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Set;

public class MainVerticle extends AbstractVerticle {


  private static final String _SUPPORT_EXTS_PATTERN = "(.html|.json|.xml)?";

  public void notSupportExtensionMiddleware(RoutingContext routingContext) {

    String path = routingContext.normalisedPath().trim();

    if (path.indexOf(".") == -1 || path.endsWith(".html") || path.endsWith(".json") || path.endsWith(".xml")) {
      routingContext.next();
      return;
    }

    notSupportedExtHandler(routingContext);

  }

  public void htmlRenderHandler(RoutingContext routingContext) {
    SerializeOptions.create(routingContext, SerializeType.HTML);
    routingContext.next();
  }

  public void jsonSerializationHandler(RoutingContext routingContext) {
    SerializeOptions.create(routingContext, SerializeType.JSON);
    routingContext.next();
  }

  public void xmlSerializationHandler(RoutingContext routingContext) {
    SerializeOptions.create(routingContext, SerializeType.XML);
    routingContext.next();
  }

  public void exceptionHandler(RoutingContext routingContext) {

    // 401 handler
    if (routingContext.statusCode() == 401) {
      // This prompts the browser to show a log-in dialog and prompt the user to enter their username and password
      routingContext.response().setStatusCode(401);
      routingContext.response().end();
      return;
    }

    if (routingContext.statusCode() == 404) {
      notFoundHandler(routingContext);
      return;
    }

    if (routingContext.statusCode() == 406) {
      errorPageHandler(routingContext, new PurposeException(406, "Operation not supported"));
      return;
    }

    if (routingContext.statusCode() == 403) {
      routingContext.response().setStatusCode(302);
      routingContext.response().putHeader("Location", "/login");
      routingContext.response().end();
      return;
    }

    errorPageHandler(routingContext);

  }


  public void errorPageHandler(RoutingContext routingContext) {
    errorPageHandler(routingContext, null);
  }

  public void errorPageHandler(RoutingContext routingContext, PurposeException exception) {

    PurposeException purposeException = exception == null ? null : exception;

    if (purposeException == null && routingContext.failure() instanceof  PurposeException) {
      purposeException = (PurposeException) routingContext.failure();
    }

    String errorMessage = ( purposeException == null ? routingContext.failure().getMessage() : purposeException.getMessage());
    StringWriter errorsTrace = new StringWriter();

    if (purposeException != null) {
      purposeException.printStackTrace(new PrintWriter(errorsTrace));
    } else {
      routingContext.failure().printStackTrace(new PrintWriter(errorsTrace));
    }

    JsonObject jsonObject = new JsonObject()
                                .put("code", purposeException.getErrorCode())
                                .put("message", errorMessage)
                                .put("errorsTrace", errorsTrace.toString());

    ChainSerialization.create(routingContext)
      .setStatusCode(201)
      .setStatusRealCode(purposeException.getErrorCode())
      .putViewName("/500.html")
      .putContextData(jsonObject)
      .serialize();

  }

  public void notFoundHandler(RoutingContext routingContext) {
    ChainSerialization.create(routingContext)
      .setStatusCode(202)
      .setStatusRealCode(404)
      .putViewName("/404.html")
      .serialize();

  }

  public void notSupportedExtHandler(RoutingContext routingContext) {
//    throw new PurposeException(406, "Extension not supported");
    notFoundHandler(routingContext);
  }

  public void indexHandler(RoutingContext routingContext) {

    JsonObject user = new JsonObject()
        .put("isAuthenticated", routingContext.user() != null)
        .put("username", routingContext.user() != null ? routingContext.user().principal().getString("username") : null);

      ChainSerialization.create(routingContext)
      .putViewName("/index.html")
      .putViewUser(user)
      .putContextData(null)
      .serialize();

  }

  @Override
  public void start() {

    final HttpServer vertxServer = vertx.createHttpServer();
    final Router vertxRouter = Router.router(vertx);

//    SessionStore sessionStore = LocalSessionStore.create(vertx, "myapp.sessionmap", 10000);
    SessionStore sessionStore = RedisSessionStore
                                    .create(vertx, "myapp.sessionmap", 10000)
                                    .host("127.0.0.1").port(6379);
    // Route: consumes and produces
    // routeInstance.consumes("text/html").consumes("text/plain").consumes("*/json");
    // routeInstance.produces("application/json");
    // etc..


    AuthHandler authHandler = AuthHandlerImpl.create(vertx);

    // vertxRouter.route().handler(CorsHandler.create("vertx\\.io").allowedMethod(HttpMethod.GET));
    vertxRouter.route().handler(FaviconHandler.create());
    vertxRouter.route().handler(CookieHandler.create());                      // Cookie cookie = routingContext.getCookie("cookie name here")
    vertxRouter.route().handler(BodyHandler.create());                        // file upload and body parameters parser
    vertxRouter.route().handler(SessionHandler.create(sessionStore));         // Session session = routingContext.session(); session.put("foo", "bar");
    vertxRouter.route().handler(UserSessionHandler.create(AuthHandlerImpl.getAuthProvider()));
    vertxRouter.route().failureHandler(this::exceptionHandler);

    vertxRouter.route().handler(routingContext -> {
      // middlewares here
      routingContext.next();
    });

    // Serving static resources
    vertxRouter.route("/assets/*").handler(StaticHandler.create("static/assets").setCachingEnabled(false).setIncludeHidden(true).setDirectoryListing(true));
    vertxRouter.route("/images/*").handler(StaticHandler.create("static/images").setCachingEnabled(false).setIncludeHidden(true).setDirectoryListing(true));
    vertxRouter.route("/uploads/*").handler(StaticHandler.create("static/uploads").setCachingEnabled(false).setIncludeHidden(true).setDirectoryListing(true));


    /***** routers *****/

    // not support exts
    vertxRouter.route().handler(this::notSupportExtensionMiddleware);

    // default Serving
    vertxRouter.route().handler(this::htmlRenderHandler);

    // Serving .html .json and .xml
    vertxRouter.routeWithRegex(".+\\.html").handler(this::htmlRenderHandler);
    vertxRouter.routeWithRegex(".+\\.json").handler(this::jsonSerializationHandler);
    vertxRouter.routeWithRegex(".+\\.xml").handler(this::xmlSerializationHandler);

    // 首页
    vertxRouter.route(HttpMethod.GET, "/").handler(routingContext -> routingContext.reroute("/index"));
    vertxRouter.routeWithRegex(HttpMethod.GET, "/index" + _SUPPORT_EXTS_PATTERN).handler(this::indexHandler);

    // 基于权限验证的 router
    vertxRouter.route("/dashboard/*").handler(authHandler);

    vertxRouter.routeWithRegex(HttpMethod.GET, "/pip" + _SUPPORT_EXTS_PATTERN)
              .handler(routingContext -> routingContext.put("count", 1).next())
              .handler(routingContext -> routingContext.response().end("pipline: " + ((int)routingContext.get("count") + 1)));

    // curl -v -X POST http://localhost:8081/ping
    Route routePingPost =  vertxRouter.route(HttpMethod.POST, "/ping");
    Route routePingGet =  vertxRouter.routeWithRegex(HttpMethod.GET, "/ping|/");

    routePingGet.handler(routingContext -> routingContext.fail(406));
    routePingPost.handler(this::indexHandler);

    // 基于路径参数的 router
    //Route routeProduct = vertxRouter.route(HttpMethod.GET, "/catalogue/products/:productType/:productId");
    Route routeProduct = vertxRouter.routeWithRegex(HttpMethod.GET, "/catalogue/products/(?<productType>[^\\/.]+)/(?<productId>[^\\/.]+)" + _SUPPORT_EXTS_PATTERN);

    routeProduct.handler(routingContext -> {
      String productType = routingContext.request().getParam("param0");
      String productId = routingContext.request().getParam("param1");
      routingContext.response().putHeader("Content-Type", "text/html;charset=UTF-8");
      routingContext.response().end("productType: <b>"+productType+"</b> <br/>productId: <b>"+productId+"</b> ");
    });

    // 基于正则表达式的 router
    // Route routeArticle = vertxRouter.routeWithRegex(HttpMethod.GET, "/articles/channel/(?<articleType>[^\\/]+)/(?<articleTag>[^\\/]+)(.json|.xml)?");
    Route routeArticle = vertxRouter.routeWithRegex(
                      HttpMethod.GET, "/articles/channel/(?<articleType>[^\\/.]+)/(?<articleTag>[^\\/.]+)" + _SUPPORT_EXTS_PATTERN);

    routeArticle.handler(routingContext -> {

      String productType = routingContext.request().getParam("param0");
      String productId = routingContext.request().getParam("param1");

      JsonObject contextData = new JsonObject()
                              .put("productType", productType)
                              .put("productId", productId);

      ChainSerialization.create(routingContext)
        .putViewName("/articles/article_detail.html")
        .putContextData(contextData)
        .serialize();


    });

    /* curl \
    -F "userid=6887" \
    -F "filecomment=This is an image file" \
    -F "image=@/home/keesh/Desktop/timg.jpg" http://localhost:8081/file_uploads */
    vertxRouter.routeWithRegex(HttpMethod.POST, "/file_uploads" + _SUPPORT_EXTS_PATTERN).handler(routingContext -> {
      Set<FileUpload> uploads = routingContext.fileUploads();
      routingContext.response().end("<" + uploads.size() + "> files uploaded");
    });

    // curl -u keesh:keesh http://localhost:8081/private/admin
    vertxRouter.route("/dashboard").handler(routingContext -> {

      JsonObject contextData = new JsonObject();

      ChainSerialization.create(routingContext)
        .putViewName("/dashboard/dashboard_index.html")
        .putContextData(contextData)
        .serialize();

    });

    vertxRouter.routeWithRegex(HttpMethod.GET, "/signout" + _SUPPORT_EXTS_PATTERN).handler(routingContext -> {
      if (routingContext.user() != null) routingContext.clearUser();
      routingContext.response().setStatusCode(302).putHeader("Location", "/").end();
    });

    vertxRouter.routeWithRegex(HttpMethod.GET, "/login" + _SUPPORT_EXTS_PATTERN).handler(routingContext -> {


      JsonObject contextData = new JsonObject();

      ChainSerialization.create(routingContext)
        .putViewName("/login.html")
        .putContextData(contextData)
        .serialize();

    });

    // JWT access token
    // curl -v -X POST -F "username=keesh" -F "password=keesh" http://localhost:8081/access_token
    // curl -v -X POST -H "Content-Type:application/x-www-form-urlencoded" -d '{"username": "keesh", "password": "keesh"}' http://localhost:8081/access_token
    // curl -v -X POST -H "Content-Type:application/json" -d '{"username": "keesh", "password": "keesh"}' http://localhost:8081/access_token
    vertxRouter.routeWithRegex(HttpMethod.POST, "/access_token" + _SUPPORT_EXTS_PATTERN).handler(routingContext -> {

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

    });

    vertxRouter.routeWithRegex(HttpMethod.POST,"/login-auth" + _SUPPORT_EXTS_PATTERN)
      .handler(FormLoginHandler.create(AuthHandlerImpl.getAuthProvider(),
        "username", "password", "return_url", "/"));

    // throw error purpose
    vertxRouter.routeWithRegex(HttpMethod.GET, "/throw" + _SUPPORT_EXTS_PATTERN).handler(routingContext -> {
      throw new PurposeException();
      //throw new PurposeException("Throw an exception purpose!");
    });

    // 404 notfound page router
    vertxRouter.routeWithRegex(HttpMethod.GET, "/not-found" + _SUPPORT_EXTS_PATTERN).handler(routingContext -> {
      routingContext.fail(404);
    });

    // 500 handler
    vertxRouter.routeWithRegex(HttpMethod.GET, "/error" + _SUPPORT_EXTS_PATTERN).handler(this::errorPageHandler);

    // not have route matchs
    vertxRouter.route("/*").handler(routingContext -> routingContext.fail(404));

    vertxServer.requestHandler(vertxRouter::accept).listen(8081);

  }

}

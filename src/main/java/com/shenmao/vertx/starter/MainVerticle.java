package com.shenmao.vertx.starter;

import com.shenmao.vertx.starter.exceptions.PurposeException;
import com.shenmao.vertx.starter.passport.AuthHandlerImpl;
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
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;
import io.vertx.ext.web.templ.TemplateEngine;
import io.vertx.ext.web.templ.ThymeleafTemplateEngine;
import org.thymeleaf.exceptions.TemplateProcessingException;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainVerticle extends AbstractVerticle {


  TemplateEngine templateEngine = ThymeleafTemplateEngine.create();
  TemplateHandler htmlTemplateHandler = TemplateHandler.create(templateEngine);
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

    String path = routingContext.normalisedPath().trim();
    String viewName = "";

    if (path.trim().isEmpty() || path.trim().equals("/")) {
      viewName = "index.html";
    } else {
      viewName = path;
    }


    templateEngine.render(routingContext, "templates", viewName, ar -> {

      if (ar.succeeded()) {
        routingContext.response().putHeader("Content-Type", "text/html");
        routingContext.response().end(ar.result());
      } else {
        routingContext.fail(ar.cause());
      }

    });

  }

  public void jsonSerializationHandler(RoutingContext routingContext) {
    routingContext.request().headers().add("Content-Type", "*/json");
    routingContext.next();
  }

  public void xmlSerializationHandler(RoutingContext routingContext) {
    routingContext.request().headers().add("Content-Type", "*/xml");
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

    if (routingContext.statusCode() == 403) {
      routingContext.response().setStatusCode(302);
      routingContext.response().putHeader("Location", "/login");
      routingContext.response().end();
      return;
    }

    if ((routingContext.failure() instanceof PurposeException)) {
      errorPageHandler(routingContext);
      return;
    }

    if (routingContext.failure() instanceof TemplateProcessingException) {
      // notFoundHandler(routingContext);   // on production
      errorPageHandler(routingContext);     // on developer
      return;
    }


    throw new RuntimeException("");


  }

  public void errorPageHandler(RoutingContext routingContext) {

    System.out.println("err, " + routingContext.request().headers().get("Content-Type"));

    String errorMessage = ( routingContext.failure() != null ? routingContext.failure().getMessage() : "Server inner error");
    routingContext.response().setStatusCode(500)
      .putHeader("Content-Type", "text/html;charset=UTF-8")
      .end("<h2>500 -- " + errorMessage + "</h2>");
  }

  public void notFoundHandler(RoutingContext routingContext) {
    routingContext.response().setStatusCode(404)
      .putHeader("Content-Type", "text/html;charset=UTF-8")
      .end("<h2>404 -- Page not found</h2>");
  }

  public void notSupportedExtHandler(RoutingContext routingContext) {
    routingContext.response().setStatusCode(406)
      .putHeader("Content-Type", "text/html;charset=UTF-8")
      .end("<h2>406 -- Extension not supported</h2>");
  }

  public void indexHandler(RoutingContext routingContext) {

    routingContext.response().putHeader("Content-Type", "text/html;charset=UTF-8");

    StringBuilder sb =new StringBuilder();

    if (routingContext.user() == null) sb.append("<a href='/login'>Login</a> <br />");
    if (routingContext.user() != null) sb.append("<a href='/signout'>Logout (" + routingContext.user().principal().getString("username") + ")</a> <br />");

    sb.append("<a href='/'>Index (GET)</a> <br />");
    sb.append("<a href='/ping'>Ping (POST)</a> <br />");
    sb.append("<a href='/catalogue/products/电子/16075'>Catalogue (GET)</a> <br />");
    sb.append("<a href='/articles/channel/big-data/vagrant.html'>Articles (GET, .json, .xml, .html)</a> <br />");
    sb.append("<a href='/error'>Error Page (GET)</a> <br />");
    sb.append("<a href='/throw'>Throw an exception purpose</a> <br />");
    sb.append("<a href='/private/admin'>Private Page</a> <br />");
    sb.append("<a href='/access_token'>Access Token (POST)</a> <br />");

    routingContext.response().end(sb.toString());

  }

  @Override
  public void start() {

    HttpServer vertxServer = vertx.createHttpServer();
    Router vertxRouter = Router.router(vertx);
    SessionStore sessionStore = LocalSessionStore.create(vertx, "myapp.sessionmap", 10000);

    // Route: consumes and produces
    // routeInstance.consumes("text/html").consumes("text/plain").consumes("*/json");
    // routeInstance.produces("application/json");
    // etc..


    AuthHandler authHandler = AuthHandlerImpl.create(vertx);

    // vertxRouter.route().handler(CorsHandler.create("vertx\\.io").allowedMethod(HttpMethod.GET));
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
    vertxRouter.route("/images/*").handler(StaticHandler.create("static/images").setCachingEnabled(false).setIncludeHidden(true).setDirectoryListing(true));
    vertxRouter.route("/assets/*").handler(StaticHandler.create("static/assets").setCachingEnabled(false).setIncludeHidden(true).setDirectoryListing(true));

    /***** routers *****/

    vertxRouter.route(HttpMethod.GET, "/").handler(this::indexHandler);

    // not support exts
    vertxRouter.route().handler(this::notSupportExtensionMiddleware);

//    vertxRouter.routeWithRegex(".+\\.xml").handler(this::xmlSerializationHandler);
//    vertxRouter.routeWithRegex(".+\\.json").handler(this::jsonSerializationHandler);

    // Serving .html
    // vertxRouter.routeWithRegex(".+\\.html").handler(htmlTemplateHandler);
    vertxRouter.routeWithRegex(".+\\.html").handler(this::htmlRenderHandler);

    // 基于权限验证的 router
    vertxRouter.route("/private/*").handler(authHandler);

    vertxRouter.routeWithRegex(HttpMethod.GET, "/pip" + _SUPPORT_EXTS_PATTERN)
              .handler(routingContext -> routingContext.put("count", 1).next())
              .handler(routingContext -> routingContext.response().end("pipline: " + ((int)routingContext.get("count") + 1)));

    // curl -v -X POST http://localhost:8081/ping
    Route routePingPost =  vertxRouter.route(HttpMethod.POST, "/ping");
    Route routePingGet =  vertxRouter.routeWithRegex(HttpMethod.GET, "/ping|/");

    routePingGet.handler(routingContext -> routingContext.fail(404));
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
      routingContext.response().putHeader("Content-Type", "text/html;charset=UTF-8");
      routingContext.response().end("articleType: <b>"+productType+"</b> <br/>articleTag: <b>"+productId+"</b> ");
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
    vertxRouter.routeWithRegex("/private/admin" + _SUPPORT_EXTS_PATTERN).handler(routingContext -> {
      routingContext.response().end("private resource~");
    });

    vertxRouter.routeWithRegex(HttpMethod.GET, "/signout" + _SUPPORT_EXTS_PATTERN).handler(routingContext -> {
      if (routingContext.user() != null) routingContext.clearUser();
      routingContext.response().setStatusCode(302).putHeader("Location", "/").end();
    });

    vertxRouter.routeWithRegex(HttpMethod.GET, "/login" + _SUPPORT_EXTS_PATTERN).handler(routingContext -> {
      routingContext.response().putHeader("Content-Type", "text/html;charset=UTF-8");

      routingContext.response().sendFile("login.html");

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
      //throw new PurposeException();
      throw new PurposeException("Throw an exception purpose!");
    });

    // 404 notfound router
    vertxRouter.routeWithRegex(HttpMethod.GET, "/not-found" + _SUPPORT_EXTS_PATTERN).handler(this::notFoundHandler);

    // 500 handler
    vertxRouter.routeWithRegex(HttpMethod.GET, "/error" + _SUPPORT_EXTS_PATTERN).handler(this::errorPageHandler);

    // not have route matchs
    vertxRouter.route().handler(routingContext -> routingContext.fail(404));

    vertxServer.requestHandler(vertxRouter::accept).listen(8081);

  }

}

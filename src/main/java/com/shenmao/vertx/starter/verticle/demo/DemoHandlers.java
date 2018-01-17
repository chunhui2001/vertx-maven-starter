package com.shenmao.vertx.starter.verticle.demo;

import com.shenmao.vertx.starter.exceptions.PurposeException;
import com.shenmao.vertx.starter.passport.AuthHandlerImpl;
import com.shenmao.vertx.starter.serialization.ChainSerialization;
import com.shenmao.vertx.starter.serialization.SerializeOptions;
import com.shenmao.vertx.starter.serialization.SerializeType;
import com.shenmao.vertx.starter.sessionstore.RedisSessionStore;
import io.vertx.core.Vertx;
import io.vertx.core.VertxException;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.*;
import io.vertx.ext.web.sstore.SessionStore;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class DemoHandlers {

  private static final int KB = 1024;
  private static final int MB = 1024 * KB;

  public static final String _SUPPORT_EXTS_PATTERN = "(.html|.htm|.json|.xml)?";
  public static final String _SUPPORT_EXTS_PATTERN2 = "^.*(\\.html|\\.htm|\\.json|\\.xml)$";
  public static final String _SUPPORT_RESOURCE_EXTS_PATTERN = "^.*(\\.jpg|\\.jpeg|\\.png|\\.gif|\\.css|\\.js|\\.ppt|\\.pdf|\\.png)$";

  public static DemoHandlers create(String mountDir) {
    return new DemoHandlers(mountDir);
  }

  private String mountDir;

  public DemoHandlers(String mountDir) {
    this.mountDir = mountDir;
  }

  public void htmlRenderHandler(RoutingContext routingContext) {
    routingContext.put("mount_dir", mountDir);
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
    int errorCode = 500;

    if (purposeException != null) {
      purposeException.printStackTrace(new PrintWriter(errorsTrace));
      errorCode = purposeException.getErrorCode();
    } else {
      routingContext.failure().printStackTrace(new PrintWriter(errorsTrace));
    }

    ChainSerialization.create(routingContext)
      .setStatusCode(201)
      .setStatusRealCode(errorCode)
      .putViewName("/500.html")
      .putMessage(errorMessage)
      .putContextData(new JsonObject())
      .putErrorTrace(errorsTrace.toString())
      .serialize();

  }

  public void notFoundHandler(RoutingContext routingContext) {

    if (routingContext.normalisedPath().matches(_SUPPORT_RESOURCE_EXTS_PATTERN)) {
      routingContext.response().setStatusCode(404).end();
      return;
    }

    ChainSerialization.create(routingContext)
      .setStatusCode(202)
      .setStatusRealCode(404)
      .putViewName("/404.html")
      .serialize();

  }

  public void notSupportedExtHandler(RoutingContext routingContext) {
    throw new PurposeException(406, "Extension not supported");
    //notFoundHandler(routingContext);
  }

  public void indexHandler(RoutingContext routingContext) {
    ChainSerialization.create(routingContext)
      .putViewName("/index.html")
      .putContextData(null)
      .serialize();
  }


  public void notSupportExtensionMiddleware(RoutingContext routingContext) {

    String path = routingContext.normalisedPath().trim();

    if (path.indexOf(".") == -1 || path.matches(_SUPPORT_EXTS_PATTERN2)) {
      routingContext.next();
      return;
    }

    if (path.indexOf(".") == -1 || path.matches(_SUPPORT_RESOURCE_EXTS_PATTERN)) {
      routingContext.next();
      return;
    }

    this.notSupportedExtHandler(routingContext);

  }

  public void wrapHandler(Vertx vertx, Router router) {

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

      this.exceptionHandler(routingContext);

    });

    router.route().handler(routingContext -> {
      // middlewares here
      routingContext.next();
    });

    // default Serving
    router.route().handler(routingContext -> {
      this.htmlRenderHandler(routingContext);
    });

    // Serving .html .json and .xml
    router.routeWithRegex(".+\\.html").handler(this::htmlRenderHandler);
    router.routeWithRegex(".+\\.json").handler(this::jsonSerializationHandler);
    router.routeWithRegex(".+\\.xml").handler(this::xmlSerializationHandler);

    // not support exts
    router.route().handler(this::notSupportExtensionMiddleware);

    // tester
    router.routeWithRegex(HttpMethod.GET, "/pip" + DemoHandlers._SUPPORT_EXTS_PATTERN)
      .handler(routingContext -> routingContext.put("count", 1).next())
      .handler(routingContext -> routingContext.response().end("pipline: " + ((int)routingContext.get("count") + 1)));

  }


}

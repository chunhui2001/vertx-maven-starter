package com.shenmao.vertx.starter.serialization;

import com.shenmao.vertx.starter.exceptions.HbsTemplateParseException;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.templ.HandlebarsTemplateEngine;
import io.vertx.ext.web.templ.TemplateEngine;

public class HtmlSerialization {

  // http://jknack.github.io/handlebars.java/reuse.html
  public static final TemplateEngine templateEngine =
      HandlebarsTemplateEngine.create().setExtension("html");

  // public static final TemplateHandler htmlTemplateHandler = TemplateHandler.create(templateEngine);
  public static final String templateFolderName = "templates";


  public static void serialize(RoutingContext context, SerializeOptions options) {

    JsonObject user = new JsonObject()
      .put("isAuthenticated", context.user() != null)
      .put("username", context.user() != null ? context.user().principal().getString("username") : null);

    serialize(context,
      (String)options.config("viewName"),
      (JsonObject)options.config("contextData"),
      user);
  }

  public static void serialize(RoutingContext context, String viewName, JsonObject viewData, JsonObject viewUser) {

    context.response().putHeader("Content-Type", "text/html;charset=UTF-8");
    context.put("viewData", viewData);
    context.put("viewUser", viewUser);
    context.put("viewMessage", context.response().getStatusMessage());

    if (context.get("X-XSRF-TOKEN") != null) {
      context.put("xsrfToken", context.get("X-XSRF-TOKEN"));
    }

    templateEngine.render(context, templateFolderName, viewName, ar -> {

      if (ar.succeeded()) {
        context.response().end(ar.result());
      } else {
        throw new HbsTemplateParseException(ar.cause().getMessage());
      }

    });

  }

}

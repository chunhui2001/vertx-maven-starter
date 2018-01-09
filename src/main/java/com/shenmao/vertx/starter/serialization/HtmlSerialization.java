package com.shenmao.vertx.starter.serialization;

import com.shenmao.vertx.starter.exceptions.HbsTemplateNotFoundException;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.TemplateHandler;
import io.vertx.ext.web.templ.FreeMarkerTemplateEngine;
import io.vertx.ext.web.templ.HandlebarsTemplateEngine;
import io.vertx.ext.web.templ.TemplateEngine;
import io.vertx.ext.web.templ.ThymeleafTemplateEngine;

public class HtmlSerialization {

  // http://jknack.github.io/handlebars.java/reuse.html
  public static final TemplateEngine templateEngine = HandlebarsTemplateEngine.create().setExtension("html");

  // public static final TemplateHandler htmlTemplateHandler = TemplateHandler.create(templateEngine);
  public static final String templateFolderName = "templates";


  public static void serialize(RoutingContext context, SerializeOptions options) {
    serialize(context, (String)options.config("viewName"), (JsonObject)options.config("contextData"));
  }

  public static void serialize(RoutingContext context, String viewName, JsonObject viewData) {

    String viewRoot = HtmlSerialization.class.getClassLoader().getResource("").toString();

    context.response().putHeader("Content-Type", "text/html;charset=UTF-8");
    context.put("viewData", viewData);
    context.put("viewRoot", viewRoot);

    templateEngine.render(context, templateFolderName, viewName, ar -> {

      if (ar.succeeded()) {
        context.response().end(ar.result());
      } else {

        if (ar.cause().getMessage().indexOf("NoSuchFileException") != -1) {
          throw new HbsTemplateNotFoundException(ar.cause().getMessage());
        }

        context.fail(ar.cause());
      }

    });

  }

}

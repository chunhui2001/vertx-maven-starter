package com.shenmao.vertx.starter.actions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.templ.FreeMarkerTemplateEngine;

public class ContextResponse {

  public static String original(String uri) {
    if (uri.indexOf('?') == -1) return uri;
    return uri.substring(0, uri.indexOf('?'));
  }

  private static final FreeMarkerTemplateEngine templateEngine = FreeMarkerTemplateEngine.create();
  private static final String templateFolderName = "templates";


  public static void write(RoutingContext context, String view, Integer statusCode) {
    context.response().setStatusCode(statusCode == null ? 200 : statusCode);
    write(context, statusCode == 404 ? "/not-found.ftl" : view);
  }

  public static void write(RoutingContext context, String view) {

    if (original(context.request().uri()).endsWith(".json")) {

      context.response().putHeader("Content-Type", "application/json");

      try {
        context.response().end(new ObjectMapper().writeValueAsString(context.get("pages")));
      } catch (JsonProcessingException e) {
        e.printStackTrace();
      }

      return;

    }

    templateEngine.render(context, templateFolderName, view, ar -> {

      if (ar.succeeded()) {
        context.response().putHeader("Content-Type", "text/html");
        context.response().end(ar.result());
      } else {
        context.fail(ar.cause());
      }

    });

  }

  public static void write(RoutingContext context, String location, Object data, Integer statusCode) {

    context.response().setStatusCode(statusCode == null ? 301 : statusCode);
    context.response().putHeader("Location", location);
    context.response().end(data + "");

  }

}

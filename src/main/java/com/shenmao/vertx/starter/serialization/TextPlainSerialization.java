package com.shenmao.vertx.starter.serialization;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class TextPlainSerialization {

  public static void serialize(RoutingContext context, String plainString) {
    context.response().putHeader("Content-Type", "text/html;charset=UTF-8");
    context.response().end(plainString);
  }

}

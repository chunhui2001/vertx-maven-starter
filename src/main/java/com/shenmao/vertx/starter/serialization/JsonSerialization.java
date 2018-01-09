package com.shenmao.vertx.starter.serialization;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class JsonSerialization {

  public static void serialize(RoutingContext context, SerializeOptions options) {
    context.response().putHeader("Content-Type", "application/json;charset=UTF-8");
    JsonObject contextData = (JsonObject)options.config("contextData");
    context.response().end(contextData.encode());
  }

}

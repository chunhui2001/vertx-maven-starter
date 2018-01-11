package com.shenmao.vertx.starter.serialization;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class JsonSerialization {

  public static void serialize(RoutingContext context, SerializeOptions options) {
    context.response().putHeader("Content-Type", "application/json;charset=UTF-8");
    context.response().end(getData(context, options).encode());
  }

  public static JsonObject getData(RoutingContext context, SerializeOptions options) {
    JsonObject contextData = (JsonObject)options.config("contextData");
    int code = context.get("setStatusRealCode") != null ? (int)context.get("setStatusRealCode") : context.statusCode();
    JsonObject jsonObject = new JsonObject()
      .put("code", code == -1 ? 200 : code)
      .put("message", context.response().getStatusMessage())
      .put("data", contextData);
    return jsonObject;
  }

}

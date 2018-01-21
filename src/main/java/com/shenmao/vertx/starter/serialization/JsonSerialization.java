package com.shenmao.vertx.starter.serialization;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class JsonSerialization {

  public static void serialize(RoutingContext context, SerializeOptions options) {
    context.response().putHeader("Content-Type", "application/json;charset=UTF-8");
    context.response().end(getData(context, options).encode());
  }

  public static JsonObject getData(RoutingContext context, SerializeOptions options) {

    Object contextData = options.config("contextData");
    int code = context.get("statusRealCode") != null ? (int)context.get("statusRealCode") : context.statusCode();
    String errorTrace = context.get("errorTrace") != null ? context.get("errorTrace").toString() : null;

    JsonObject jsonObject = new JsonObject()
      .put("code", code == -1 ? 200 : code)
      .put("message", context.response().getStatusMessage())
      .put("data", contextData);

    if (errorTrace != null) {
      jsonObject.put("errorTrace", errorTrace);
    }

    return jsonObject;
  }

}

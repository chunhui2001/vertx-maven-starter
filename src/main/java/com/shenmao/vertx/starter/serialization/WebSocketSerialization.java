package com.shenmao.vertx.starter.serialization;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.io.IOException;

public class WebSocketSerialization {

  public static void serialize(RoutingContext context, SerializeOptions options){
    String _ws_endpoint = (String)options.config("ws_endpoint");
    JsonObject data = JsonSerialization.getData(context, options);
    context.vertx().eventBus().publish(_ws_endpoint, data);
    context.response().setStatusCode(data.getInteger("code") == null ? 200 : data.getInteger("code")).end();
  }

}

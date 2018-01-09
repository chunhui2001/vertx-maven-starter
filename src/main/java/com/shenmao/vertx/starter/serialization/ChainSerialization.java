package com.shenmao.vertx.starter.serialization;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class ChainSerialization {

  RoutingContext _context;

  private ChainSerialization(RoutingContext context) {
    this._context = context;
  }

  public static ChainSerialization create(RoutingContext context) {
    return new ChainSerialization(context);
  }

  public void serialize() {

    SerializeOptions serializeOptions = (SerializeOptions)this._context.data().get("serializeOptions");

    switch (serializeOptions.getType()) {
      case HTML:
        HtmlSerialization.serialize(this._context, serializeOptions);
        break;
      case JSON:
        JsonSerialization.serialize(this._context, serializeOptions);
        break;
      case XML:
        break;
      case TEXT:
        break;
      default:
          throw new IllegalArgumentException("Invalid shiro auth realm type: " + serializeOptions.getType());
    }

  }

  public ChainSerialization putViewName(RoutingContext context, String viewName) {
    SerializeOptions serializeOptions = context.get("serializeOptions");
    serializeOptions.putConfig("viewName", viewName);
    return this;
  }

  public ChainSerialization putContextData(RoutingContext context, JsonObject contextData) {
    SerializeOptions serializeOptions = context.get("serializeOptions");
    serializeOptions.putConfig("contextData", contextData);
    return this;
  }

}

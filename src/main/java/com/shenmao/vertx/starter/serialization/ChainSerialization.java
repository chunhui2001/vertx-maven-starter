package com.shenmao.vertx.starter.serialization;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class ChainSerialization {

  RoutingContext _context;

  private ChainSerialization(RoutingContext context) {
    this._context = context;
  }

  public static ChainSerialization create(RoutingContext context) {

    SerializeOptions serializeOptions = context.get("serializeOptions");

    if (serializeOptions == null) {
      SerializeOptions.create(context, SerializeType.HTML);
    }

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
        XmlSerialization.serialize(this._context, serializeOptions);
        break;
      case WS:
        WebSocketSerialization.serialize(this._context, serializeOptions);
        break;
      case TEXT:
        break;
      default:
          throw new IllegalArgumentException("Invalid shiro auth realm type: " + serializeOptions.getType());
    }

  }

  public ChainSerialization putViewName(String viewName) {
    SerializeOptions serializeOptions = this._context.get("serializeOptions");
    serializeOptions.putConfig("viewName", viewName);
    return this;
  }

  public ChainSerialization putViewUser(JsonObject user) {
    SerializeOptions serializeOptions = this._context.get("serializeOptions");
    serializeOptions.putConfig("viewUser", user);
    return this;
  }

  public ChainSerialization putContextData(JsonObject contextData) {
    SerializeOptions serializeOptions = this._context.get("serializeOptions");
    serializeOptions.putConfig("contextData", contextData);
    return this;
  }

  public ChainSerialization setSerializeType(SerializeType type) {
    SerializeOptions serializeOptions = this._context.get("serializeOptions");
    serializeOptions.setType(type);
    return this;
  }

  public ChainSerialization setStatusCode(int code) {
    this._context.response().setStatusCode(code);
    return this;
  }

  public ChainSerialization setStatusRealCode(int code) {
    this._context.put("statusRealCode", code);
    return this;
  }

  public ChainSerialization putMessage(String msg) {
    this._context.response().setStatusMessage(msg);
    return this;
  }

  public ChainSerialization putErrorTrace(String error) {
    this._context.put("errorTrace", error);
    return this;
  }

  public ChainSerialization setWsEndpoint(String endpoint) {
    SerializeOptions serializeOptions = this._context.get("serializeOptions");
    serializeOptions.putConfig("ws_endpoint", endpoint);
    return this;
  }

}

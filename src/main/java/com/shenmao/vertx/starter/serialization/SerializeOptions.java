package com.shenmao.vertx.starter.serialization;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class SerializeOptions {

  public static SerializeOptions create(RoutingContext routingContext, SerializeType type) {


    SerializeOptions serializeOptions = new SerializeOptions(routingContext, type);
    routingContext.put("serializeOptions", serializeOptions);
    return serializeOptions;

  }

  private RoutingContext _routingContext;
  private SerializeType _type;
  private JsonObject _config;

  private SerializeOptions(RoutingContext routingContext, SerializeType type) {
    this._routingContext = routingContext;
    this.setType(type);
    this.setConfig(new JsonObject());
  }

  public void setType(SerializeType type) {

    this._type = type;

    switch (type) {
      case HTML:
        _routingContext.request().headers().set("Accept", "text/html");
        break;
      case JSON:
        _routingContext.request().headers().set("Accept", "*/json");
        break;
      case XML:
        _routingContext.request().headers().set("Accept", "*/xml");
        break;
      case TEXT:
        break;
      default:
        throw new IllegalArgumentException("Invalid shiro auth realm type: " + type);
    }

  }

  public SerializeType getType() {
    return this._type;
  }

  public void setConfig(JsonObject config) {
    this._config = config;
  }

  public Object config(String key) {
    if (this._config.containsKey(key) && this._config.getValue(key) != null)
      return this._config.getValue(key);
    return new JsonObject();
  }

  public void putConfig(String key, Object val) {
    this._config.put(key, val);
  }

}

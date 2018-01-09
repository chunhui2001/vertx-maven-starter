package com.shenmao.vertx.starter.serialization;

import io.vertx.core.json.JsonObject;

public class SerializeOptions {

  private SerializeType _type;
  private JsonObject _config;

  public SerializeOptions(SerializeType type) {
    this._type = type;
    this._config = new JsonObject();
  }

  public void setType(SerializeType type) {
    this._type = type;
  }

  public SerializeType getType() {
    return this._type;
  }

  public void setConfig(JsonObject config) {
    this._config = config;
  }

  public Object config(String key) {
    return this._config.getValue(key);
  }

  public void putConfig(String key, Object val) {
    this._config.put(key, val);
  }

}

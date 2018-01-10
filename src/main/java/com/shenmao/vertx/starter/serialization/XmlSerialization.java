package com.shenmao.vertx.starter.serialization;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.io.IOException;

public class XmlSerialization {

  public static void serialize(RoutingContext context, SerializeOptions options){
    context.response().putHeader("Content-Type", "application/xml;charset=UTF-8");
    JsonObject contextData = (JsonObject)options.config("contextData");
    String xml = null;

    try {
      xml = JsonToXMLConverter.create().convertJsonToXml(contextData.encode());
    } catch (IOException e) {
      e.printStackTrace();
    }

    context.response().end(xml);
  }

}

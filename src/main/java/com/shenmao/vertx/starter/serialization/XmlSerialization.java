package com.shenmao.vertx.starter.serialization;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.io.IOException;

public class XmlSerialization {

  public static void serialize(RoutingContext context, SerializeOptions options){

    context.response().putHeader("Content-Type", "application/xml;charset=UTF-8");
    JsonObject data = JsonSerialization.getData(context, options);
    String xml = null;

    try {
      xml = JsonToXMLConverter.create().convertJsonToXml(data.encode());
    } catch (IOException e) {
      e.printStackTrace();
    }

    context.response().end(xml);
  }

}

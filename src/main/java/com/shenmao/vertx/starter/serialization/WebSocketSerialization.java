package com.shenmao.vertx.starter.serialization;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;
import io.vertx.kafka.client.producer.RecordMetadata;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.io.IOException;
import java.util.Properties;

public class WebSocketSerialization {

  static Properties kkConfig = new Properties();

  static {
    kkConfig.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
    kkConfig.put(ProducerConfig.ACKS_CONFIG, "1");
    kkConfig.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
    kkConfig.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
  }


  public static void serialize(RoutingContext context, SerializeOptions options){

    KafkaProducer<String, String> producer = KafkaProducer.createShared(context.vertx(), "the-producer", kkConfig);

    String _ws_endpoint = (String)options.config("ws_endpoint");
    JsonObject data = JsonSerialization.getData(context, options);
    data.put("_ws_endpoint", _ws_endpoint);

    KafkaProducerRecord<String, String> record = KafkaProducerRecord.create("VertxChatRoot", "chatKey", data.encodePrettily());

    producer.write(record, done -> {

      if (done.succeeded()) {
        // RecordMetadata recordMetadata = done.result();
        // context.vertx().eventBus().publish(_ws_endpoint, data);
        context.response().setStatusCode(data.getInteger("code") == null ? 200 : data.getInteger("code")).end();

      }

    });

    producer.close();

  }

}

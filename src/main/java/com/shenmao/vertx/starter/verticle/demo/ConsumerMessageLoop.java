package com.shenmao.vertx.starter.verticle.demo;

import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import io.vertx.rxjava.core.Vertx;
import org.apache.kafka.common.errors.WakeupException;

import java.util.HashMap;
import java.util.Map;

public class ConsumerMessageLoop extends Thread {

  private final Vertx vertx;
  private final KafkaConsumer<String, String> consumer;
  private final String topicName;

  public static ConsumerMessageLoop create (Vertx vertx, String topicName) {
    return new ConsumerMessageLoop(vertx, topicName);
  }

  public ConsumerMessageLoop(Vertx vertx, String topicName) {

    Map<String, String> config = new HashMap<>();
    config.put("bootstrap.servers", "localhost:9092");
    config.put("group.id", "my_kafka_group");
    config.put("auto.offset.reset", "earliest");
    config.put("enable.auto.commit", "false");

    this.vertx = vertx;
    this.consumer = KafkaConsumer.create(vertx.getDelegate(), config, String.class, String.class);
    this.topicName = topicName;

  }


  @Override
  public void run() {

    if (this.topicName == null || this.topicName.isEmpty()) return;

    consumer.subscribe(this.topicName);

    try {


      while (true) {

        consumer.handler( record -> {
          JsonObject data = new JsonObject(record.value());
          vertx.eventBus().publish(data.getString("_ws_endpoint"), data);
          System.out.println("Processing key=" + record.key() + ",value=" + record.value() +
            ",partition=" + record.partition() + ",offset=" + record.offset());
        });

        Thread.sleep(300);
      }

    } catch (WakeupException e) {

    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      if (consumer != null) consumer.close();;
    }

  }

  public void shutdown() {
    consumer.close();
  }

}

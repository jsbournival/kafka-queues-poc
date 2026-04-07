package com.example.kafkaqueues.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class FastConsumer extends ShareConsumerSupport {

  public FastConsumer(
      @Value("${app.kafka.consumer.fast-sleep-ms}") long fastSleepMs,
      ConsumptionReport report) {
    super(fastSleepMs, report);
  }

  @KafkaListener(
      topics = "${app.kafka.topic}",
      containerFactory = "shareKafkaListenerContainerFactory",
      groupId = "${app.kafka.share-group-id}")
  public void listen(ConsumerRecord<String, String> record) throws InterruptedException {
    process(record);
  }
}

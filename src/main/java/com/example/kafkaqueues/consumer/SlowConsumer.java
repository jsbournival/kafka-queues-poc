package com.example.kafkaqueues.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class SlowConsumer extends ShareConsumerSupport {

  public SlowConsumer(
      @Value("${app.kafka.consumer.slow-sleep-ms}") long slowSleepMs,
      ConsumptionReport report) {
    super(slowSleepMs, report);
  }

  @KafkaListener(
      topics = "${app.kafka.topic}",
      containerFactory = "shareKafkaListenerContainerFactory",
      groupId = "${app.kafka.share-group-id}")
  public void listen(ConsumerRecord<String, String> record) throws InterruptedException {
    process(record);
  }
}

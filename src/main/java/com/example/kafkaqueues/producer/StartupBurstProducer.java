package com.example.kafkaqueues.producer;

import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(Integer.MAX_VALUE)
public class StartupBurstProducer implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(StartupBurstProducer.class);

  private final KafkaTemplate<String, String> kafkaTemplate;

  @Value("${app.kafka.topic}")
  private String topic;

  @Value("${app.kafka.produce-on-startup}")
  private int produceOnStartup;

  @Value("${app.kafka.produce-delay-ms:8000}")
  private long produceDelayMs;

  public StartupBurstProducer(KafkaTemplate<String, String> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  @Override
  public void run(ApplicationArguments args) throws ExecutionException, InterruptedException {
    if (produceOnStartup <= 0) {
      return;
    }
    if (produceDelayMs > 0) {
      log.info("Waiting {} ms before publishing so share consumers can join", produceDelayMs);
      Thread.sleep(produceDelayMs);
    }
    log.info("Publishing {} records to {}", produceOnStartup, topic);
    for (int i = 0; i < produceOnStartup; i++) {
      String key = Integer.toString(i);
      String value = "msg-" + i;
      kafkaTemplate.send(topic, key, value).get();
    }
    log.info("Finished publishing {} records", produceOnStartup);
  }
}

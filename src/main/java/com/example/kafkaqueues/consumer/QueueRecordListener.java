package com.example.kafkaqueues.consumer;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class QueueRecordListener {

  private static final Logger log = LoggerFactory.getLogger(QueueRecordListener.class);

  private final double slowProbability;
  private final long slowSleepMs;
  private final long fastSleepMs;

  public QueueRecordListener(
      @Value("${app.kafka.consumer.slow-probability}") double slowProbability,
      @Value("${app.kafka.consumer.slow-sleep-ms}") long slowSleepMs,
      @Value("${app.kafka.consumer.fast-sleep-ms}") long fastSleepMs) {
    this.slowProbability = slowProbability;
    this.slowSleepMs = slowSleepMs;
    this.fastSleepMs = fastSleepMs;
  }

  @KafkaListener(
      topics = "${app.kafka.topic}",
      containerFactory = "shareKafkaListenerContainerFactory",
      groupId = "${app.kafka.share-group-id}",
      concurrency = "${app.kafka.consumer.concurrency}")
  public void listen(ConsumerRecord<String, String> record) throws InterruptedException {
    boolean slow = ThreadLocalRandom.current().nextDouble() < slowProbability;
    Instant started = Instant.now();
    log.info(
        "BEGIN partition={} offset={} key={} slowDraw={} thread={}",
        record.partition(),
        record.offset(),
        record.key(),
        slow,
        Thread.currentThread().getName());
    long sleepMs = slow ? slowSleepMs : fastSleepMs;
    if (sleepMs > 0) {
      Thread.sleep(sleepMs);
    }
    long durationMs = Duration.between(started, Instant.now()).toMillis();
    log.info(
        "END partition={} offset={} durationMs={} slow={} thread={}",
        record.partition(),
        record.offset(),
        durationMs,
        slow,
        Thread.currentThread().getName());
  }
}

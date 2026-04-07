package com.example.kafkaqueues.consumer;

import java.time.Duration;
import java.time.Instant;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

abstract class ShareConsumerSupport {

  static final String POISON_VALUE = "POISON";

  private static final Logger log = LoggerFactory.getLogger(ShareConsumerSupport.class);

  @Value("${app.kafka.consumer.poison-sleep-ms}")
  private long poisonSleepMs;

  private final long sleepMs;
  private final ConsumptionReport report;

  protected ShareConsumerSupport(long sleepMs, ConsumptionReport report) {
    this.sleepMs = sleepMs;
    this.report = report;
  }

  protected void process(ConsumerRecord<String, String> record) throws InterruptedException {
    if (POISON_VALUE.equals(record.value())) {
      processPoison(record);
      return;
    }

    Instant started = Instant.now();
    log.info(
        "BEGIN consumer={} partition={} offset={} key={} thread={}",
        getClass().getSimpleName(),
        record.partition(),
        record.offset(),
        record.key(),
        Thread.currentThread().getName());
    if (sleepMs > 0) {
      Thread.sleep(sleepMs);
    }
    long durationMs = Duration.between(started, Instant.now()).toMillis();
    log.info(
        "END consumer={} partition={} offset={} durationMs={} thread={}",
        getClass().getSimpleName(),
        record.partition(),
        record.offset(),
        durationMs,
        Thread.currentThread().getName());
    report.record(getClass().getSimpleName());
  }

  private void processPoison(ConsumerRecord<String, String> record) throws InterruptedException {
    int attempt = report.recordPoisonAttempt();
    log.warn(
        "POISON attempt={} consumer={} offset={} sleeping={}ms (past lock duration) thread={}",
        attempt,
        getClass().getSimpleName(),
        record.offset(),
        poisonSleepMs,
        Thread.currentThread().getName());
    Thread.sleep(poisonSleepMs);
    log.warn(
        "POISON attempt={} consumer={} offset={} woke up — lock already expired, broker will redeliver or drop",
        attempt,
        getClass().getSimpleName(),
        record.offset());
  }
}

package com.example.kafkaqueues.consumer;

import java.time.Duration;
import java.time.Instant;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class ShareConsumerSupport {

  private static final Logger log = LoggerFactory.getLogger(ShareConsumerSupport.class);

  private final long sleepMs;
  private final ConsumptionReport report;

  protected ShareConsumerSupport(long sleepMs, ConsumptionReport report) {
    this.sleepMs = sleepMs;
    this.report = report;
  }

  protected void process(ConsumerRecord<String, String> record) throws InterruptedException {
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
}

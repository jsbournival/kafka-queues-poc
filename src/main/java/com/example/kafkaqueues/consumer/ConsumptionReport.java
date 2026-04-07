package com.example.kafkaqueues.consumer;

import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class ConsumptionReport {

  private static final Logger log = LoggerFactory.getLogger(ConsumptionReport.class);

  private final int target;
  private final int maxDeliveryAttempts;
  private final AtomicInteger total = new AtomicInteger(0);
  private final AtomicInteger poisonAttempts = new AtomicInteger(0);
  private final ConcurrentHashMap<String, AtomicInteger> counts = new ConcurrentHashMap<>();

  ConsumptionReport(
      @Value("${app.kafka.produce-on-startup}") int produceOnStartup,
      @Value("${app.kafka.consumer.max-delivery-attempts}") int maxDeliveryAttempts) {
    this.target = produceOnStartup - 1;
    this.maxDeliveryAttempts = maxDeliveryAttempts;
  }

  void record(String consumerName) {
    counts.computeIfAbsent(consumerName, k -> new AtomicInteger()).incrementAndGet();
    if (total.incrementAndGet() == target) {
      printReport();
    }
  }

  int recordPoisonAttempt() {
    int attempt = poisonAttempts.incrementAndGet();
    if (attempt == maxDeliveryAttempts) {
      log.warn(
          "POISON attempt={}/{} — max delivery attempts reached, broker DROPS the record",
          attempt,
          maxDeliveryAttempts);
    }
    return attempt;
  }

  private void printReport() {
    log.info("=== CONSUMPTION REPORT ({} messages) ===", target);
    counts.entrySet().stream()
        .sorted(Comparator.comparing(e -> e.getKey()))
        .forEach(e -> log.info("  {}: {} messages", e.getKey(), e.getValue().get()));
    log.info(
        "  POISON: delivered {} time(s), will be dropped after {} attempts",
        poisonAttempts.get(),
        maxDeliveryAttempts);
    log.info("=========================================");
  }
}

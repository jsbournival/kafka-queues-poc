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
  private final AtomicInteger total = new AtomicInteger(0);
  private final ConcurrentHashMap<String, AtomicInteger> counts = new ConcurrentHashMap<>();

  ConsumptionReport(@Value("${app.kafka.produce-on-startup}") int target) {
    this.target = target;
  }

  void record(String consumerName) {
    counts.computeIfAbsent(consumerName, k -> new AtomicInteger()).incrementAndGet();
    if (total.incrementAndGet() == target) {
      printReport();
    }
  }

  private void printReport() {
    log.info("=== CONSUMPTION REPORT ({} messages) ===", target);
    counts.entrySet().stream()
        .sorted(Comparator.comparing(e -> e.getKey()))
        .forEach(e -> log.info("  {}: {} messages", e.getKey(), e.getValue().get()));
    log.info("=========================================");
  }
}

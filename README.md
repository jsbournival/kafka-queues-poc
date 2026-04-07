# Kafka Queues POC

A proof-of-concept demonstrating **Kafka Queues** (KIP-932 / Share Consumers) with Spring Boot 4.

## Goal

Traditional Kafka consumer groups assign partitions exclusively to a single consumer. If that consumer is slow or blocked, no other consumer can pick up messages from the same partition — the partition is effectively stalled.

**Kafka Queues** (introduced in KIP-932, available since Kafka 4.0) break this constraint by allowing multiple consumers to share delivery from the same partition within a *share group*. The broker tracks in-flight records per consumer and re-delivers unacknowledged ones after a lock timeout — similar to a message queue (SQS, RabbitMQ) but built natively on Kafka.

The question this POC answers: **can a fast consumer keep processing messages from a single-partition topic while a slow consumer is blocked on an earlier message?**

## Stack

| Component | Version |
|---|---|
| Java | 25 |
| Spring Boot | 4.0.x |
| Spring Kafka | 4.0.x |
| Apache Kafka | 4.2.0 |
| Gradle | 9.4.1 |

## Design

### Single partition

The topic `poc-queue-topic` is created with **1 partition**. In a classic consumer group this would make concurrency impossible — only one consumer thread could be active at a time. Using share groups, this becomes the cleanest possible proof: any concurrency observed is solely due to Kafka Queues semantics.

### Two consumers, one share group

Two independent `@KafkaListener` beans subscribe to the same topic under the same share group ID:

- **`FastConsumer`** — processes each record instantly (0 ms sleep)
- **`SlowConsumer`** — simulates slow processing (500 ms sleep per record)

Both listeners use a `ShareKafkaListenerContainerFactory` backed by a `DefaultShareConsumerFactory`. `max.poll.records=1` ensures each consumer fetches one record at a time, making the interleaving easy to observe in logs.

### Producer

On startup, `StartupBurstProducer` waits 8 seconds (to let both consumers connect) then publishes 400 messages to the topic.

### Reporting

`ConsumptionReport` maintains an atomic counter per consumer. When the combined total reaches 400, it prints a summary showing how many records each consumer processed.

## Running

**Prerequisites:** Docker, Java 25 (via [SDKMAN](https://sdkman.io/) or similar)

```bash
# Start Kafka
docker compose up -d

# Run the application
JAVA_HOME=~/.sdkman/candidates/java/25.0.1-tem ./gradlew bootRun
```

Wait for the report in the logs (~15 seconds after startup).

### Resetting between runs

The share group remembers its position. To get a clean run:

```bash
docker exec kafka-queues-broker /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 --delete --topic poc-queue-topic

sleep 2

docker exec kafka-queues-broker /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 --create --topic poc-queue-topic \
  --partitions 1 --replication-factor 1
```

## Findings

### Attempt 1 — two consumers, one was always slow (45 s sleep)

With a 45-second sleep, `SlowConsumer` locked one message and sat on it for the entire duration. `FastConsumer` consumed all remaining messages in seconds.

The logs showed the non-blocking behaviour clearly:

```
17:19:56.384  BEGIN consumer=SlowConsumer  partition=0 offset=801   ← locks offset 801
17:19:56.391  BEGIN consumer=FastConsumer  partition=0 offset=802   ← immediately picks up next
17:19:56.392  END   consumer=FastConsumer  partition=0 offset=802   durationMs=0
... FastConsumer races through offsets 803 → 1199 (397 more messages) ...
17:20:41.390  END   consumer=SlowConsumer  partition=0 offset=801   durationMs=45005
```

However, the consumption report was less compelling — `SlowConsumer` contributed 0 completed records within the 400-message window because its one in-flight record didn't finish until after the batch was done.

This exposed a second key behaviour: once `group.share.record.lock.duration.ms` elapses, the broker **reclaims the unacknowledged record from `SlowConsumer` and redelivers it to another available consumer** — in this case `FastConsumer`. The slow consumer doesn't just fall behind; the work it failed to finish in time is actively reassigned. In a classic consumer group this scenario (consumer holding a partition and timing out) would trigger a full rebalance and a flood of duplicate processing. Here the broker handles it surgically, at the record level.

### Attempt 2 — 500 ms sleep (final configuration)

Reducing the slow sleep to 500 ms let `SlowConsumer` actually complete records during the run, producing a clear split:

```
=== CONSUMPTION REPORT (400 messages) ===
  FastConsumer: 394 messages
  SlowConsumer:   6 messages
=========================================
```

Both consumers processed from the **same single partition** concurrently. The broker distributed records based on consumer availability — faster consumer, more records. No partition reassignment, no blocked partition, no starvation.

### Conclusion

Kafka Queues deliver true work-queue semantics on top of Kafka:

- Slower consumers don't block faster ones, even on a single partition
- Records that a consumer holds too long are **reclaimed by the broker and redelivered** to a faster peer — no manual intervention, no rebalance
- The throughput of the group is gated by the fastest available consumer rather than the slowest

This is a meaningful shift in how Kafka can be used for task-processing workloads where processing times are unpredictable.

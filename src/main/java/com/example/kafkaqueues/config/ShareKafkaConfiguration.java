package com.example.kafkaqueues.config;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ShareKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultShareConsumerFactory;
import org.springframework.kafka.core.ShareConsumerFactory;

@Configuration
@EnableKafka
public class ShareKafkaConfiguration {

  @Bean
  public ShareConsumerFactory<String, String> shareConsumerFactory(
      @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
      @Value("${app.kafka.share-group-id}") String groupId,
      @Value("${app.kafka.consumer.share-lock-duration-ms}") long shareLockDurationMs) {
    Map<String, Object> props = new HashMap<>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put("group.share.record.lock.duration.ms", shareLockDurationMs);
    return new DefaultShareConsumerFactory<>(props);
  }

  @Bean
  public ShareKafkaListenerContainerFactory<String, String> shareKafkaListenerContainerFactory(
      ShareConsumerFactory<String, String> shareConsumerFactory,
      @Value("${app.kafka.consumer.concurrency}") int concurrency) {
    ShareKafkaListenerContainerFactory<String, String> factory =
        new ShareKafkaListenerContainerFactory<>(shareConsumerFactory);
    factory.setConcurrency(concurrency);
    return factory;
  }
}

package com.example.kafkaqueues.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class TopicConfiguration {

  @Bean
  public org.apache.kafka.clients.admin.NewTopic pocQueueTopic(@Value("${app.kafka.topic}") String topic) {
    return TopicBuilder.name(topic).partitions(1).replicas(1).build();
  }
}

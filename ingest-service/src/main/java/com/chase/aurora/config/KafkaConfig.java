package com.chase.aurora.config;

import com.chase.aurora.model.Transaction;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonSerializer;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

  @Value("${spring.kafka.bootstrap-servers}")
  private String bootstrapServers;

  @Bean
  public ProducerFactory<String, Transaction> producerFactory() {
    Map<String, Object> props = new HashMap<>();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    
    // Performance optimizations for high throughput
    props.put(ProducerConfig.BATCH_SIZE_CONFIG, 32768);
    props.put(ProducerConfig.LINGER_MS_CONFIG, 5);
    props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");
    props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 67108864); // 64MB
    
    // Reliability settings
    props.put(ProducerConfig.ACKS_CONFIG, "all");
    props.put(ProducerConfig.RETRIES_CONFIG, 3);
    props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
    props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
    
    // JSON serializer settings
    props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
    
    return new DefaultKafkaProducerFactory<>(props);
  }

  @Bean
  public KafkaTemplate<String, Transaction> kafkaTemplate() {
    return new KafkaTemplate<>(producerFactory());
  }

  @Bean
  public NewTopic txnTopic() {
    return TopicBuilder.name("txn.v1")
        .partitions(48)  // High partition count for parallelism
        .replicas(3)     // Replication for fault tolerance
        .config("cleanup.policy", "delete")
        .config("retention.ms", "7776000000") // 90 days
        .config("compression.type", "lz4")
        .build();
  }
  
  @Bean
  public NewTopic txnDlqTopic() {
    return TopicBuilder.name("txn.v1.dlq")
        .partitions(12)
        .replicas(3)
        .config("cleanup.policy", "delete")
        .config("retention.ms", "2592000000") // 30 days for DLQ
        .build();
  }
} 
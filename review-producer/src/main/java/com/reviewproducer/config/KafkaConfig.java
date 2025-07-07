package com.reviewproducer.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {
    
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    
    @Value("${kafka.topic.reviews:reviews}")
    private String reviewsTopic;
    
    @Value("${kafka.topic.bad-reviews:bad_review_records}")
    private String badReviewsTopic;
    
    @Value("${kafka.topic.dlq:dlq}")
    private String dlqTopic;
    
    @Value("${kafka.dlq.retention.ms:604800000}") // 7 days default
    private long dlqRetentionMs;
    
    @Value("${kafka.dlq.retention.bytes:1073741824}") // 1GB default
    private long dlqRetentionBytes;
    
    @Value("${kafka.dlq.segment.ms:86400000}") // 1 day default
    private long dlqSegmentMs;
    
    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 1);
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        return new DefaultKafkaProducerFactory<>(configProps);
    }
    
    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
    
    @Bean
    public KafkaAdmin kafkaAdmin() {
        return new KafkaAdmin(Map.of("bootstrap.servers", bootstrapServers));
    }
    
    @Bean
    public org.apache.kafka.clients.admin.NewTopic reviewsTopic() {
        return TopicBuilder.name(reviewsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
    
    @Bean
    public org.apache.kafka.clients.admin.NewTopic badReviewsTopic() {
        return TopicBuilder.name(badReviewsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
    
    @Bean
    public org.apache.kafka.clients.admin.NewTopic dlqTopic() {
        return TopicBuilder.name(dlqTopic)
                .partitions(3)
                .replicas(1)
                .configs(Map.of(
                    "retention.ms", String.valueOf(dlqRetentionMs),
                    "retention.bytes", String.valueOf(dlqRetentionBytes),
                    "segment.ms", String.valueOf(dlqSegmentMs),
                    "cleanup.policy", "delete",
                    "delete.retention.ms", "1000", // Immediate deletion after retention
                    "min.compaction.lag.ms", "0"
                ))
                .build();
    }
} 
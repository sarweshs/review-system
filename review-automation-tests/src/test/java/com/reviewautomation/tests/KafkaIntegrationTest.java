package com.reviewautomation.tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reviewautomation.base.BaseIntegrationTest;
import com.reviewautomation.util.TestDataBuilder;
import com.reviewautomation.config.TestConfig;
import com.reviewcore.dto.ReviewMessage;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.kafka.bootstrap-servers=${kafka.bootstrap-servers}"
})
@DisplayName("Kafka Integration Tests")
public class KafkaIntegrationTest extends BaseIntegrationTest {

    private static final String GOOD_REVIEWS_TOPIC = "good_review_records";
    private static final String BAD_REVIEWS_TOPIC = "bad_review_records";
    private static final String TEST_CONSUMER_GROUP = "test-consumer-group";

    @Test
    @DisplayName("Should produce and consume good review message")
    void testGoodReviewMessageFlow() throws Exception {
        // Create test message
        ReviewMessage testMessage = TestDataBuilder.createTestReviewMessage();
        ObjectMapper objectMapper = new ObjectMapper();
        String messageJson = objectMapper.writeValueAsString(testMessage);

        // Create producer
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, TestConfig.getKafkaBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps);

        // Create consumer
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, TestConfig.getKafkaBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, TEST_CONSUMER_GROUP + "-" + System.currentTimeMillis());
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(Arrays.asList(GOOD_REVIEWS_TOPIC));

        try {
            // Produce message
            ProducerRecord<String, String> record = new ProducerRecord<>(
                GOOD_REVIEWS_TOPIC, 
                testMessage.getHotelId().toString(), 
                messageJson
            );
            
            producer.send(record).get(10, TimeUnit.SECONDS);
            log.info("Produced message to topic: {}", GOOD_REVIEWS_TOPIC);

            // Consume message
            await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
                    return !records.isEmpty();
                });

            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
            assertThat(records).isNotEmpty();

            for (ConsumerRecord<String, String> record1 : records) {
                log.info("Consumed message: key={}, value={}", record1.key(), record1.value());
                assertThat(record1.key()).isEqualTo(testMessage.getHotelId().toString());
                
                ReviewMessage consumedMessage = objectMapper.readValue(record1.value(), ReviewMessage.class);
                assertThat(consumedMessage.getHotelId()).isEqualTo(testMessage.getHotelId());
                assertThat(consumedMessage.getHotelName()).isEqualTo(testMessage.getHotelName());
                assertThat(consumedMessage.getPlatform()).isEqualTo(testMessage.getPlatform());
            }

        } finally {
            producer.close();
            consumer.close();
        }
    }

    @Test
    @DisplayName("Should produce and consume bad review message")
    void testBadReviewMessageFlow() throws Exception {
        // Create test message
        ReviewMessage testMessage = TestDataBuilder.createTestReviewMessage();
        testMessage.getComment().setRating(1.0);
        testMessage.getComment().setRatingText("Terrible");
        testMessage.getComment().setReviewTitle("Worst hotel experience");
        testMessage.getComment().setReviewComments("This hotel was absolutely terrible.");
        
        ObjectMapper objectMapper = new ObjectMapper();
        String messageJson = objectMapper.writeValueAsString(testMessage);

        // Create producer
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, TestConfig.getKafkaBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps);

        // Create consumer
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, TestConfig.getKafkaBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, TEST_CONSUMER_GROUP + "-bad-" + System.currentTimeMillis());
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(Arrays.asList(BAD_REVIEWS_TOPIC));

        try {
            // Produce message
            ProducerRecord<String, String> record = new ProducerRecord<>(
                BAD_REVIEWS_TOPIC, 
                testMessage.getHotelId().toString(), 
                messageJson
            );
            
            producer.send(record).get(10, TimeUnit.SECONDS);
            log.info("Produced bad review message to topic: {}", BAD_REVIEWS_TOPIC);

            // Consume message
            await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
                    return !records.isEmpty();
                });

            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
            assertThat(records).isNotEmpty();

            for (ConsumerRecord<String, String> record1 : records) {
                log.info("Consumed bad review message: key={}, value={}", record1.key(), record1.value());
                assertThat(record1.key()).isEqualTo(testMessage.getHotelId().toString());
                
                ReviewMessage consumedMessage = objectMapper.readValue(record1.value(), ReviewMessage.class);
                assertThat(consumedMessage.getHotelId()).isEqualTo(testMessage.getHotelId());
                assertThat(consumedMessage.getComment().getRating()).isEqualTo(1.0);
                assertThat(consumedMessage.getComment().getRatingText()).isEqualTo("Terrible");
            }

        } finally {
            producer.close();
            consumer.close();
        }
    }

    @Test
    @DisplayName("Should handle multiple messages in sequence")
    void testMultipleMessagesFlow() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // Create producer
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, TestConfig.getKafkaBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps);

        // Create consumer
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, TestConfig.getKafkaBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, TEST_CONSUMER_GROUP + "-multi-" + System.currentTimeMillis());
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(Arrays.asList(GOOD_REVIEWS_TOPIC));

        try {
            // Produce multiple messages
            for (int i = 1; i <= 3; i++) {
                ReviewMessage testMessage = TestDataBuilder.createTestReviewMessage();
                testMessage.setHotelId(1000L + i);
                testMessage.setHotelName("Test Hotel " + i);
                
                String messageJson = objectMapper.writeValueAsString(testMessage);
                ProducerRecord<String, String> record = new ProducerRecord<>(
                    GOOD_REVIEWS_TOPIC, 
                    testMessage.getHotelId().toString(), 
                    messageJson
                );
                
                producer.send(record).get(10, TimeUnit.SECONDS);
                log.info("Produced message {} to topic: {}", i, GOOD_REVIEWS_TOPIC);
            }

            // Consume messages
            await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
                    return records.count() >= 3;
                });

            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
            assertThat(records.count()).isGreaterThanOrEqualTo(3);

            for (ConsumerRecord<String, String> record : records) {
                log.info("Consumed message: key={}", record.key());
                ReviewMessage consumedMessage = objectMapper.readValue(record.value(), ReviewMessage.class);
                assertThat(consumedMessage.getHotelId()).isGreaterThanOrEqualTo(1001L);
                assertThat(consumedMessage.getHotelId()).isLessThanOrEqualTo(1003L);
            }

        } finally {
            producer.close();
            consumer.close();
        }
    }

    @Test
    @DisplayName("Should handle malformed JSON gracefully")
    void testMalformedJsonHandling() throws Exception {
        // Create producer
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, TestConfig.getKafkaBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps);

        // Create consumer
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, TestConfig.getKafkaBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, TEST_CONSUMER_GROUP + "-malformed-" + System.currentTimeMillis());
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(Arrays.asList(GOOD_REVIEWS_TOPIC));

        try {
            // Produce malformed JSON
            String malformedJson = "{ invalid json }";
            ProducerRecord<String, String> record = new ProducerRecord<>(
                GOOD_REVIEWS_TOPIC, 
                "999999", 
                malformedJson
            );
            
            producer.send(record).get(10, TimeUnit.SECONDS);
            log.info("Produced malformed JSON to topic: {}", GOOD_REVIEWS_TOPIC);

            // Consumer should still be able to receive the message (as string)
            await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
                    return !records.isEmpty();
                });

            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
            assertThat(records).isNotEmpty();

            for (ConsumerRecord<String, String> record1 : records) {
                log.info("Consumed malformed message: key={}, value={}", record1.key(), record1.value());
                assertThat(record1.key()).isEqualTo("999999");
                assertThat(record1.value()).isEqualTo(malformedJson);
            }

        } finally {
            producer.close();
            consumer.close();
        }
    }
} 
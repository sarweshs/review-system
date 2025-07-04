package com.reviewservice.kafka;

import com.reviewcore.model.Review;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@EmbeddedKafka(
        topics = {"reviews"},
        partitions = 1,
        brokerProperties = {
                "listeners=PLAINTEXT://localhost:9093",
                "port=9093",
                "log.dir=target/embedded-kafka-logs"
        }
)
@DirtiesContext
class ReviewProducerConsumerTest {

    @Autowired
    private KafkaTemplate<String, Review> kafkaTemplate;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    private Consumer<String, Review> consumer;

    @BeforeEach
    void setUp() {
        // Configure consumer properties
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("testGroup", "false", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.reviewcore.model");

        // Create consumer
        consumer = new org.apache.kafka.clients.consumer.KafkaConsumer<>(
                consumerProps,
                new StringDeserializer(),
                new JsonDeserializer<>(Review.class, false));

        // Subscribe to topic before producing messages
        consumer.subscribe(Collections.singleton("reviews"));
    }

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    void testSendAndReceive() throws Exception {
        // Set up a consumer to receive the message BEFORE sending
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("testGroup", "true", embeddedKafkaBroker);
        org.apache.kafka.clients.consumer.KafkaConsumer<String, Review> consumer =
                new org.apache.kafka.clients.consumer.KafkaConsumer<>(consumerProps, new StringDeserializer(), new JsonDeserializer<>(Review.class, false));
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, "reviews");

        // Now send the message
        Review review = new Review();
        review.setReviewId("kafka-test");
        kafkaTemplate.send("reviews", review.getReviewId(), review).get();
        kafkaTemplate.flush();

        // Wait for the message
        ConsumerRecord<String, Review> record = KafkaTestUtils.getSingleRecord(consumer, "reviews", java.time.Duration.ofSeconds(10));
        assertThat(record.value().getReviewId()).isEqualTo("kafka-test");
        consumer.close();
    }


    @Bean
    @Primary // Overrides the default KafkaTemplate
    public KafkaTemplate<String, Review> testKafkaTemplate(
            ProducerFactory<String, Review> producerFactory) {
        KafkaTemplate<String, Review> template = new KafkaTemplate<>(producerFactory);
        template.setAllowNonTransactional(true);
        return template;
    }
}
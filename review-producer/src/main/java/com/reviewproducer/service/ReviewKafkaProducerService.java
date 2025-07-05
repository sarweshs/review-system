package com.reviewproducer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewKafkaProducerService {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${kafka.topic.reviews:reviews}")
    private String reviewsTopic;
    
    @Value("${kafka.topic.bad-reviews:bad-reviews}")
    private String badReviewsTopic;
    
    /**
     * Send a valid review to Kafka
     */
    public void sendValidReview(String reviewJson) {
        try {
            kafkaTemplate.send(reviewsTopic, reviewJson);
            log.debug("Sent valid review to Kafka topic: {}", reviewsTopic);
        } catch (Exception e) {
            log.error("Failed to send valid review to Kafka: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send valid review to Kafka", e);
        }
    }
    
    /**
     * Send a bad review record to Kafka
     */
    public void sendBadReview(String badReviewJson) {
        try {
            kafkaTemplate.send(badReviewsTopic, badReviewJson);
            log.debug("Sent bad review to Kafka topic: {}", badReviewsTopic);
        } catch (Exception e) {
            log.error("Failed to send bad review to Kafka: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send bad review to Kafka", e);
        }
    }
    
    /**
     * Send a review with validation - if valid, send to reviews topic; if invalid, send to bad-reviews topic
     */
    public void sendReviewWithValidation(String reviewJson, String platform) {
        try {
            // Validate JSON format
            objectMapper.readTree(reviewJson);
            
            // If validation passes, send to valid reviews topic
            sendValidReview(reviewJson);
            log.info("Valid review sent to Kafka for platform: {}", platform);
            
        } catch (Exception e) {
            log.warn("Invalid review detected for platform: {}, reason: {}", platform, e.getMessage());
            
            // Create bad review record and send to bad reviews topic
            try {
                String badReviewRecord = createBadReviewRecord(reviewJson, platform, e.getMessage());
                sendBadReview(badReviewRecord);
                log.info("Bad review record sent to Kafka for platform: {}", platform);
            } catch (Exception badReviewException) {
                log.error("Failed to send bad review record to Kafka: {}", badReviewException.getMessage(), badReviewException);
            }
        }
    }
    
    /**
     * Create a bad review record JSON
     */
    private String createBadReviewRecord(String originalJson, String platform, String reason) {
        try {
            BadReviewRecord badRecord = new BadReviewRecord(originalJson, platform, reason);
            return objectMapper.writeValueAsString(badRecord);
        } catch (Exception e) {
            log.error("Failed to create bad review record JSON: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create bad review record", e);
        }
    }
    
    /**
     * Bad review record DTO for internal use
     */
    private static class BadReviewRecord {
        private final String jsonData;
        private final String platform;
        private final String reason;
        private final String createdAt;
        
        public BadReviewRecord(String jsonData, String platform, String reason) {
            this.jsonData = jsonData;
            this.platform = platform;
            this.reason = reason;
            this.createdAt = java.time.LocalDateTime.now().toString();
        }
        
        // Getters
        public String getJsonData() { return jsonData; }
        public String getPlatform() { return platform; }
        public String getReason() { return reason; }
        public String getCreatedAt() { return createdAt; }
    }
} 
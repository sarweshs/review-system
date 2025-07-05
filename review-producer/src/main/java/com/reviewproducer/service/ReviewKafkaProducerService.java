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
    private final ReviewValidationService validationService;
    
    @Value("${kafka.topic.reviews:reviews}")
    private String reviewsTopic;
    
    @Value("${kafka.topic.bad-reviews:bad_review_records}")
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
     * Process a single review line with validation
     */
    public void processReviewLine(String reviewJson) {
        try {
            // Validate the review
            ReviewValidationService.ValidationResult validationResult = validationService.validateReview(reviewJson);
            
            if (validationResult.isValid()) {
                // Send valid review to Kafka
                sendValidReview(reviewJson);
                log.debug("Valid review sent to Kafka");
            } else {
                // Extract platform for bad record
                String platform = validationService.extractPlatform(reviewJson);
                
                // Create and send bad review record
                String badReviewRecord = createBadReviewRecord(reviewJson, platform, validationResult.getReason());
                sendBadReview(badReviewRecord);
                
                // Log the bad record as requested
                log.warn("Bad review record detected - Platform: {}, Reason: {}, Record: {}", 
                        platform, validationResult.getReason(), reviewJson);
            }
            
        } catch (Exception e) {
            log.error("Failed to process review line: {}", e.getMessage(), e);
            
            // Create bad record for processing error
            String platform = validationService.extractPlatform(reviewJson);
            String badReviewRecord = createBadReviewRecord(reviewJson, platform, "PROCESSING_ERROR: " + e.getMessage());
            sendBadReview(badReviewRecord);
        }
    }
    
    /**
     * Create a bad review record JSON in the specified format
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
     * Bad review record DTO matching the database schema
     */
    private static class BadReviewRecord {
        private final String jsonData;
        private final String platform;
        private final String reason;
        
        public BadReviewRecord(String jsonData, String platform, String reason) {
            this.jsonData = jsonData;
            this.platform = platform;
            this.reason = reason;
        }
        
        // Getters
        public String getJsonData() { return jsonData; }
        public String getPlatform() { return platform; }
        public String getReason() { return reason; }
    }
} 
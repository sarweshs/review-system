package com.reviewproducer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
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
    
    @Value("${kafka.topic.dlq:dlq}")
    private String dlqTopic;
    
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
     * Send a record to Dead Letter Queue (DLQ)
     */
    public void sendToDLQ(String dlqRecordJson) {
        try {
            kafkaTemplate.send(dlqTopic, dlqRecordJson);
            log.debug("Sent record to DLQ topic: {}", dlqTopic);
        } catch (Exception e) {
            log.error("Failed to send record to DLQ: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send record to DLQ", e);
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
            } else if (validationResult.shouldSendToDLQ()) {
                // Extract platform and review ID info for DLQ record
                String platform = validationService.extractPlatform(reviewJson);
                ReviewValidationService.ReviewIdInfo reviewIdInfo = validationService.extractReviewIdInfo(reviewJson);
                
                // Create and send DLQ record
                String dlqRecord = createDLQRecord(reviewJson, platform, validationResult.getReason(), reviewIdInfo);
                sendToDLQ(dlqRecord);
                
                // Log the DLQ record
                log.warn("Record sent to DLQ - Platform: {}, Reason: {}, ReviewId: {}, ProviderId: {}", 
                        platform, validationResult.getReason(), 
                        reviewIdInfo.getReviewId(), reviewIdInfo.getProviderId());
            } else {
                // Extract platform for bad record
                String platform = validationService.extractPlatform(reviewJson);
                
                // Create and send bad review record
                String badReviewRecord = createBadReviewRecord(reviewJson, platform, validationResult.getReason());
                sendBadReview(badReviewRecord);
                
                // Log the bad record
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
     * Create a DLQ record JSON for records with missing critical fields
     */
    private String createDLQRecord(String originalJson, String platform, String reason, ReviewValidationService.ReviewIdInfo reviewIdInfo) {
        try {
            DLQRecord dlqRecord = new DLQRecord(
                reviewIdInfo.getReviewId(),
                reviewIdInfo.getProviderId(),
                originalJson,
                platform,
                reason,
                System.currentTimeMillis()
            );
            return objectMapper.writeValueAsString(dlqRecord);
        } catch (Exception e) {
            log.error("Failed to create DLQ record JSON: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create DLQ record", e);
        }
    }
    
    /**
     * Create a bad review record JSON in the specified format
     */
    private String createBadReviewRecord(String originalJson, String platform, String reason) {
        try {
            // Extract reviewId and providerId from the original JSON
            ReviewValidationService.ReviewIdInfo reviewIdInfo = validationService.extractReviewIdInfo(originalJson);
            BadReviewRecord badRecord = new BadReviewRecord(
                reviewIdInfo.getReviewId(), 
                reviewIdInfo.getProviderId(), 
                originalJson, 
                platform, 
                reason
            );
            return objectMapper.writeValueAsString(badRecord);
        } catch (Exception e) {
            log.error("Failed to create bad review record JSON: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create bad review record", e);
        }
    }
    
    /**
     * DLQ record DTO for records with missing critical fields
     */
    private static class DLQRecord {
        private final Long reviewId;
        private final Integer providerId;
        private final String originalJson;
        private final String platform;
        private final String reason;
        private final Long timestamp;
        
        public DLQRecord(Long reviewId, Integer providerId, String originalJson, String platform, String reason, Long timestamp) {
            this.reviewId = reviewId;
            this.providerId = providerId;
            this.originalJson = originalJson;
            this.platform = platform;
            this.reason = reason;
            this.timestamp = timestamp;
        }
        
        // Getters
        public Long getReviewId() { return reviewId; }
        public Integer getProviderId() { return providerId; }
        public String getOriginalJson() { return originalJson; }
        public String getPlatform() { return platform; }
        public String getReason() { return reason; }
        public Long getTimestamp() { return timestamp; }
    }
    
    /**
     * Bad review record DTO matching the database schema
     */
    private static class BadReviewRecord {
        private final Long reviewId;
        private final Integer providerId;
        private final String jsonData;
        private final String platform;
        private final String reason;
        
        public BadReviewRecord(Long reviewId, Integer providerId, String jsonData, String platform, String reason) {
            this.reviewId = reviewId;
            this.providerId = providerId;
            this.jsonData = jsonData;
            this.platform = platform;
            this.reason = reason;
        }
        
        // Getters
        public Long getReviewId() { return reviewId; }
        public Integer getProviderId() { return providerId; }
        public String getJsonData() { return jsonData; }
        public String getPlatform() { return platform; }
        public String getReason() { return reason; }
    }
} 
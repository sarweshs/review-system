package com.reviewproducer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewValidationService {
    
    private final ObjectMapper objectMapper;
    
    /**
     * Validation result containing validation status and reason if invalid
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String reason;
        private final boolean shouldSendToDLQ;
        
        public ValidationResult(boolean valid, String reason, boolean shouldSendToDLQ) {
            this.valid = valid;
            this.reason = reason;
            this.shouldSendToDLQ = shouldSendToDLQ;
        }
        
        public boolean isValid() { return valid; }
        public String getReason() { return reason; }
        public boolean shouldSendToDLQ() { return shouldSendToDLQ; }
        
        public static ValidationResult valid() {
            return new ValidationResult(true, null, false);
        }
        
        public static ValidationResult invalid(String reason) {
            return new ValidationResult(false, reason, false);
        }
        
        public static ValidationResult invalidWithDLQ(String reason) {
            return new ValidationResult(false, reason, true);
        }
    }
    
    /**
     * Validate a single review JSON line
     */
    public ValidationResult validateReview(String reviewJson) {
        try {
            // Parse JSON to check if it's valid JSON format
            JsonNode reviewNode = objectMapper.readTree(reviewJson);
            
            // Check if hotelId is null
            JsonNode hotelIdNode = reviewNode.get("hotelId");
            if (hotelIdNode == null || hotelIdNode.isNull()) {
                return ValidationResult.invalid("HOTEL_ID_NULL");
            }
            
            // Check if hotelName is null
            JsonNode hotelNameNode = reviewNode.get("hotelName");
            if (hotelNameNode == null || hotelNameNode.isNull()) {
                return ValidationResult.invalid("HOTEL_NAME_NULL");
            }
            
            // Check for comment section
            JsonNode commentNode = reviewNode.get("comment");
            if (commentNode == null || commentNode.isNull()) {
                return ValidationResult.invalidWithDLQ("COMMENT_SECTION_MISSING");
            }
            
            // Check for review_id (hotelReviewId) - CRITICAL FIELD
            JsonNode reviewIdNode = commentNode.get("hotelReviewId");
            if (reviewIdNode == null || reviewIdNode.isNull()) {
                return ValidationResult.invalidWithDLQ("REVIEW_ID_MISSING");
            }
            
            // Check for provider_id - CRITICAL FIELD
            JsonNode providerIdNode = commentNode.get("providerId");
            if (providerIdNode == null || providerIdNode.isNull()) {
                return ValidationResult.invalidWithDLQ("PROVIDER_ID_MISSING");
            }
            
            // Validate that review_id and provider_id are valid numbers
            try {
                if (reviewIdNode.asLong() <= 0) {
                    return ValidationResult.invalidWithDLQ("REVIEW_ID_INVALID_VALUE");
                }
            } catch (Exception e) {
                return ValidationResult.invalidWithDLQ("REVIEW_ID_NOT_NUMBER");
            }
            
            try {
                if (providerIdNode.asInt() <= 0) {
                    return ValidationResult.invalidWithDLQ("PROVIDER_ID_INVALID_VALUE");
                }
            } catch (Exception e) {
                return ValidationResult.invalidWithDLQ("PROVIDER_ID_NOT_NUMBER");
            }
            
            // If all validations pass
            return ValidationResult.valid();
            
        } catch (Exception e) {
            log.warn("Failed to parse or validate review JSON: {}", e.getMessage());
            return ValidationResult.invalid("INVALID_JSON: " + e.getMessage());
        }
    }
    
    /**
     * Extract platform from review JSON
     */
    public String extractPlatform(String reviewJson) {
        try {
            JsonNode reviewNode = objectMapper.readTree(reviewJson);
            JsonNode platformNode = reviewNode.get("platform");
            return platformNode != null && !platformNode.isNull() ? platformNode.asText() : "unknown";
        } catch (Exception e) {
            log.warn("Failed to extract platform from review JSON: {}", e.getMessage());
            return "unknown";
        }
    }
    
    /**
     * Extract review_id and provider_id from review JSON
     * Returns null values if fields are missing
     */
    public ReviewIdInfo extractReviewIdInfo(String reviewJson) {
        try {
            JsonNode reviewNode = objectMapper.readTree(reviewJson);
            JsonNode commentNode = reviewNode.get("comment");
            
            Long reviewId = null;
            Integer providerId = null;
            
            if (commentNode != null && !commentNode.isNull()) {
                JsonNode reviewIdNode = commentNode.get("hotelReviewId");
                if (reviewIdNode != null && !reviewIdNode.isNull()) {
                    try {
                        reviewId = reviewIdNode.asLong();
                    } catch (Exception e) {
                        log.warn("Could not parse review_id as Long: {}", reviewIdNode.asText());
                    }
                }
                
                JsonNode providerIdNode = commentNode.get("providerId");
                if (providerIdNode != null && !providerIdNode.isNull()) {
                    try {
                        providerId = providerIdNode.asInt();
                    } catch (Exception e) {
                        log.warn("Could not parse provider_id as Integer: {}", providerIdNode.asText());
                    }
                }
            }
            
            return new ReviewIdInfo(reviewId, providerId);
        } catch (Exception e) {
            log.warn("Failed to extract review_id and provider_id from review JSON: {}", e.getMessage());
            return new ReviewIdInfo(null, null);
        }
    }
    
    /**
     * Helper class to hold review_id and provider_id information
     */
    public static class ReviewIdInfo {
        private final Long reviewId;
        private final Integer providerId;
        
        public ReviewIdInfo(Long reviewId, Integer providerId) {
            this.reviewId = reviewId;
            this.providerId = providerId;
        }
        
        public Long getReviewId() { return reviewId; }
        public Integer getProviderId() { return providerId; }
        
        public boolean hasValidIds() {
            return reviewId != null && reviewId > 0 && providerId != null && providerId > 0;
        }
    }
} 
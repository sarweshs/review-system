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
        
        public ValidationResult(boolean valid, String reason) {
            this.valid = valid;
            this.reason = reason;
        }
        
        public boolean isValid() { return valid; }
        public String getReason() { return reason; }
        
        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }
        
        public static ValidationResult invalid(String reason) {
            return new ValidationResult(false, reason);
        }
    }
    
    /**
     * Validate a single review JSON line
     */
    public ValidationResult validateReview(String reviewJson) {
        try {
            // Parse JSON to check if it's valid JSON format
            JsonNode reviewNode = objectMapper.readTree(reviewJson);
            
            // Check if hotel_id is null
            JsonNode hotelIdNode = reviewNode.get("hotel_id");
            if (hotelIdNode == null || hotelIdNode.isNull()) {
                return ValidationResult.invalid("HOTEL_ID_NULL");
            }
            
            // Check if hotel_name is null
            JsonNode hotelNameNode = reviewNode.get("hotel_name");
            if (hotelNameNode == null || hotelNameNode.isNull()) {
                return ValidationResult.invalid("HOTEL_NAME_NULL");
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
} 
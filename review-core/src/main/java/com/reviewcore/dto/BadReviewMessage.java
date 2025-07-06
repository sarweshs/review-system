package com.reviewcore.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * DTO representing a bad review message from Kafka
 * This matches the BadReviewRecord structure sent by the producer
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BadReviewMessage {
    
    @JsonProperty("jsonData")
    private String jsonData;
    
    @JsonProperty("platform")
    private String platform;
    
    @JsonProperty("reason")
    private String reason;
} 
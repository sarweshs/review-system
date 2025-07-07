package com.reviewproducer.controller;

import com.reviewproducer.service.ReviewKafkaProducerService;
import com.reviewproducer.service.MetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/producer")
@RequiredArgsConstructor
public class ReviewProducerController {
    
    private final ReviewKafkaProducerService kafkaProducerService;
    private final MetricsService metricsService;
    
    /**
     * Send a review to Kafka with validation
     */
    @PostMapping("/review")
    public ResponseEntity<String> sendReview(@RequestBody ReviewRequest request) {
        log.info("Received review for platform: {}", request.getPlatform());
        
        try {
            kafkaProducerService.processReviewLine(request.getReviewJson());
            return ResponseEntity.ok("Review processed successfully");
        } catch (Exception e) {
            log.error("Failed to process review: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to process review: " + e.getMessage());
        }
    }
    
    /**
     * Send a valid review directly to Kafka
     */
    @PostMapping("/review/valid")
    public ResponseEntity<String> sendValidReview(@RequestBody ReviewRequest request) {
        log.info("Sending valid review for platform: {}", request.getPlatform());
        
        try {
            kafkaProducerService.sendValidReview(request.getReviewJson());
            return ResponseEntity.ok("Valid review sent successfully");
        } catch (Exception e) {
            log.error("Failed to send valid review: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to send valid review: " + e.getMessage());
        }
    }
    
    /**
     * Send a bad review record to Kafka
     */
    @PostMapping("/review/bad")
    public ResponseEntity<String> sendBadReview(@RequestBody ReviewRequest request) {
        log.info("Sending bad review for platform: {}", request.getPlatform());
        
        try {
            kafkaProducerService.sendBadReview(request.getReviewJson());
            return ResponseEntity.ok("Bad review sent successfully");
        } catch (Exception e) {
            log.error("Failed to send bad review: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to send bad review: " + e.getMessage());
        }
    }
    
    /**
     * Send a record to Dead Letter Queue (DLQ)
     */
    @PostMapping("/review/dlq")
    public ResponseEntity<String> sendToDLQ(@RequestBody ReviewRequest request) {
        log.info("Sending record to DLQ for platform: {}", request.getPlatform());
        
        try {
            kafkaProducerService.sendToDLQ(request.getReviewJson());
            return ResponseEntity.ok("Record sent to DLQ successfully");
        } catch (Exception e) {
            log.error("Failed to send record to DLQ: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to send record to DLQ: " + e.getMessage());
        }
    }
    
    /**
     * Get metrics
     */
    @GetMapping("/metrics")
    public ResponseEntity<Object> getMetrics() {
        return ResponseEntity.ok(metricsService.getMetricsSummary());
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Review Producer is running");
    }
    
    /**
     * Request DTO for review operations
     */
    public static class ReviewRequest {
        private String reviewJson;
        private String platform;
        
        // Getters and setters
        public String getReviewJson() { return reviewJson; }
        public void setReviewJson(String reviewJson) { this.reviewJson = reviewJson; }
        public String getPlatform() { return platform; }
        public void setPlatform(String platform) { this.platform = platform; }
    }
} 
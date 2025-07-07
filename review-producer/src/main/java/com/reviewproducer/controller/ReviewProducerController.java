package com.reviewproducer.controller;

import com.reviewproducer.service.ReviewKafkaProducerService;
import com.reviewproducer.service.MetricsService;
import com.reviewproducer.service.MinIOEventService;
import com.reviewproducer.service.StorageEventService;
import com.reviewproducer.model.MinIOEvent;
import com.reviewproducer.model.StorageEvent;
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
    private final MinIOEventService minIOEventService;
    private final StorageEventService storageEventService;
    
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
     * Handle MinIO events for uploaded .jl files
     */
    @PostMapping("/storage/event/minio")
    public ResponseEntity<String> handleMinIOEvent(@RequestBody MinIOEvent event) {
        log.info("Received MinIO event");
        
        try {
            storageEventService.processMinIOEvent(event);
            return ResponseEntity.ok("MinIO event processed successfully");
        } catch (Exception e) {
            log.error("Failed to process MinIO event: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to process MinIO event: " + e.getMessage());
        }
    }
    
    /**
     * Handle generic storage events (for different storage providers)
     */
    @PostMapping("/storage/event")
    public ResponseEntity<String> handleStorageEvent(@RequestBody StorageEvent event) {
        log.info("Received generic storage event from provider: {}", event.getProvider());
        
        try {
            storageEventService.processStorageEvent(event);
            return ResponseEntity.ok("Storage event processed successfully");
        } catch (Exception e) {
            log.error("Failed to process storage event: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to process storage event: " + e.getMessage());
        }
    }
    
    /**
     * Handle raw storage events (for manual processing)
     */
    @PostMapping("/storage/event/raw")
    public ResponseEntity<String> handleRawStorageEvent(@RequestBody String eventPayload) {
        log.info("Received raw storage event");
        
        try {
            // For now, we'll just log the event payload
            // In the future, this could be extended to handle different storage providers
            log.info("Raw storage event payload: {}", eventPayload);
            return ResponseEntity.ok("Raw storage event received");
        } catch (Exception e) {
            log.error("Failed to process raw storage event: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to process raw storage event: " + e.getMessage());
        }
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
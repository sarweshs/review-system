package com.reviewconsumer.controller;

import com.reviewconsumer.service.MetricsService;
import com.reviewconsumer.service.ReviewConsumerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class MetricsController {
    
    private static final Logger logger = LoggerFactory.getLogger(MetricsController.class);
    
    private final MetricsService metricsService;
    private final ReviewConsumerService reviewConsumerService;
    
    @Autowired
    public MetricsController(MetricsService metricsService, ReviewConsumerService reviewConsumerService) {
        this.metricsService = metricsService;
        this.reviewConsumerService = reviewConsumerService;
    }
    
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        logger.info("Retrieving consumer metrics");
        
        Map<String, Object> metrics = new HashMap<>();
        
        // Basic counts
        metrics.put("processedReviews", metricsService.getProcessedReviews());
        metrics.put("badReviews", metricsService.getBadReviews());
        metrics.put("errorCount", metricsService.getErrorCount());
        
        // Processing statistics
        metrics.put("totalProcessingTimeMs", metricsService.getTotalProcessingTime());
        metrics.put("averageProcessingTimeMs", metricsService.getAverageProcessingTime());
        
        // Rates and performance
        metrics.put("processingRatePerSecond", metricsService.getProcessingRate());
        metrics.put("errorRate", metricsService.getErrorRate());
        
        // Uptime
        metrics.put("startTime", metricsService.getStartTime());
        metrics.put("uptimeSeconds", metricsService.getUptimeSeconds());
        
        // Service-specific metrics
        metrics.put("serviceProcessedCount", reviewConsumerService.getProcessedCount());
        metrics.put("serviceErrorCount", reviewConsumerService.getErrorCount());
        
        // Timestamp
        metrics.put("timestamp", Instant.now());
        
        logger.debug("Returning metrics: {}", metrics);
        
        return ResponseEntity.ok(metrics);
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "review-consumer");
        health.put("timestamp", Instant.now());
        health.put("uptimeSeconds", metricsService.getUptimeSeconds());
        
        return ResponseEntity.ok(health);
    }
} 
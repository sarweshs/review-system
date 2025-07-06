package com.reviewproducer.service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MetricsServiceTest {
    
    private MetricsService metricsService;
    private SimpleMeterRegistry meterRegistry;
    
    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metricsService = new MetricsService(meterRegistry);
    }
    
    @Test
    void testRecordFileProcessing() {
        // Record file processing metrics
        metricsService.recordFileProcessing("test-source", "test-file.jl", 100, 80, 15, 5, 1500);
        
        // Get metrics summary
        MetricsService.MetricsSummary summary = metricsService.getMetricsSummary();
        
        assertEquals(1, summary.getTotalFilesProcessed());
        assertEquals(100, summary.getTotalLinesProcessed());
        assertEquals(80, summary.getTotalValidReviews());
        assertEquals(15, summary.getTotalInvalidReviews());
        assertEquals(0, summary.getTotalProcessingErrors());
    }
    
    @Test
    void testRecordSourceProcessing() {
        // Record source processing metrics
        metricsService.recordSourceProcessing("test-source", 10, 5, 3, 2000);
        
        // Get metrics summary
        MetricsService.MetricsSummary summary = metricsService.getMetricsSummary();
        
        assertEquals(0, summary.getTotalFilesProcessed()); // Source processing doesn't increment file count
        assertEquals(0, summary.getTotalLinesProcessed());
        assertEquals(0, summary.getTotalValidReviews());
        assertEquals(0, summary.getTotalInvalidReviews());
        assertEquals(0, summary.getTotalProcessingErrors());
    }
    
    @Test
    void testRecordJobExecution() {
        // Record job execution metrics
        metricsService.recordJobExecution(3, 2, 1, 20, 10, 8, 5000);
        
        // Get metrics summary
        MetricsService.MetricsSummary summary = metricsService.getMetricsSummary();
        
        assertEquals(0, summary.getTotalFilesProcessed());
        assertEquals(0, summary.getTotalLinesProcessed());
        assertEquals(0, summary.getTotalValidReviews());
        assertEquals(0, summary.getTotalInvalidReviews());
        assertEquals(1, summary.getTotalProcessingErrors()); // 1 failure
    }
    
    @Test
    void testRecordQueueMetrics() {
        // Record queue metrics
        metricsService.recordQueueMetrics(2, 4, 5, 10);
        
        // Get metrics summary
        MetricsService.MetricsSummary summary = metricsService.getMetricsSummary();
        
        assertEquals(0, summary.getTotalFilesProcessed());
        assertEquals(0, summary.getTotalLinesProcessed());
        assertEquals(0, summary.getTotalValidReviews());
        assertEquals(0, summary.getTotalInvalidReviews());
        assertEquals(0, summary.getTotalProcessingErrors());
    }
    
    @Test
    void testMetricsSummaryToString() {
        MetricsService.MetricsSummary summary = new MetricsService.MetricsSummary(10, 1000, 800, 150, 5, 
                java.time.Instant.parse("2025-07-05T13:00:00Z"));
        
        String summaryString = summary.toString();
        assertTrue(summaryString.contains("files=10"));
        assertTrue(summaryString.contains("lines=1000"));
        assertTrue(summaryString.contains("valid=800"));
        assertTrue(summaryString.contains("invalid=150"));
        assertTrue(summaryString.contains("errors=5"));
        assertTrue(summaryString.contains("timestamp=2025-07-05T13:00:00Z"));
    }
} 
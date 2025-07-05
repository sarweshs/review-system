package com.reviewconsumer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MetricsServiceTest {

    private MetricsService metricsService;

    @BeforeEach
    void setUp() {
        metricsService = new MetricsService();
    }

    @Test
    void testInitialMetrics() {
        assertEquals(0, metricsService.getProcessedReviews());
        assertEquals(0, metricsService.getBadReviews());
        assertEquals(0, metricsService.getErrorCount());
        assertEquals(0, metricsService.getTotalProcessingTime());
        assertEquals(0.0, metricsService.getAverageProcessingTime());
        assertEquals(0.0, metricsService.getProcessingRate());
        assertEquals(0.0, metricsService.getErrorRate());
    }

    @Test
    void testIncrementProcessedReviews() {
        metricsService.incrementProcessedReviews();
        assertEquals(1, metricsService.getProcessedReviews());
        
        metricsService.incrementProcessedReviews();
        assertEquals(2, metricsService.getProcessedReviews());
    }

    @Test
    void testIncrementBadReviews() {
        metricsService.incrementBadReviews();
        assertEquals(1, metricsService.getBadReviews());
        
        metricsService.incrementBadReviews();
        assertEquals(2, metricsService.getBadReviews());
    }

    @Test
    void testIncrementErrorCount() {
        metricsService.incrementErrorCount();
        assertEquals(1, metricsService.getErrorCount());
        
        metricsService.incrementErrorCount();
        assertEquals(2, metricsService.getErrorCount());
    }

    @Test
    void testAddProcessingTime() {
        metricsService.addProcessingTime(100);
        assertEquals(100, metricsService.getTotalProcessingTime());
        
        metricsService.addProcessingTime(50);
        assertEquals(150, metricsService.getTotalProcessingTime());
    }

    @Test
    void testAverageProcessingTime() {
        metricsService.incrementProcessedReviews();
        metricsService.addProcessingTime(100);
        assertEquals(100.0, metricsService.getAverageProcessingTime());
        
        metricsService.incrementProcessedReviews();
        metricsService.addProcessingTime(50);
        assertEquals(75.0, metricsService.getAverageProcessingTime());
    }

    @Test
    void testErrorRate() {
        // No reviews processed yet
        assertEquals(0.0, metricsService.getErrorRate());
        
        // Add some processed reviews
        metricsService.incrementProcessedReviews();
        metricsService.incrementProcessedReviews();
        assertEquals(0.0, metricsService.getErrorRate());
        
        // Add an error
        metricsService.incrementErrorCount();
        assertEquals(0.5, metricsService.getErrorRate()); // 1 error / 2 total = 0.5
        
        // Add bad reviews
        metricsService.incrementBadReviews();
        assertEquals(0.33, metricsService.getErrorRate(), 0.01); // 1 error / 3 total ≈ 0.33
    }

    @Test
    void testUptime() {
        assertNotNull(metricsService.getStartTime());
        assertTrue(metricsService.getUptimeSeconds() >= 0);
    }
} 
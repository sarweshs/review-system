package com.reviewconsumer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class MetricsService {
    
    private static final Logger logger = LoggerFactory.getLogger(MetricsService.class);
    
    private final AtomicLong processedReviews = new AtomicLong(0);
    private final AtomicLong badReviews = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    private final AtomicLong totalProcessingTime = new AtomicLong(0);
    
    private final Instant startTime = Instant.now();
    
    public void incrementProcessedReviews() {
        long count = processedReviews.incrementAndGet();
        logger.debug("Incremented processed reviews count to: {}", count);
    }
    
    public void incrementBadReviews() {
        long count = badReviews.incrementAndGet();
        logger.debug("Incremented bad reviews count to: {}", count);
    }
    
    public void incrementErrorCount() {
        long count = errorCount.incrementAndGet();
        logger.debug("Incremented error count to: {}", count);
    }
    
    public void addProcessingTime(long processingTimeMs) {
        long total = totalProcessingTime.addAndGet(processingTimeMs);
        logger.debug("Added processing time: {}ms, total: {}ms", processingTimeMs, total);
    }
    
    public long getProcessedReviews() {
        return processedReviews.get();
    }
    
    public long getBadReviews() {
        return badReviews.get();
    }
    
    public long getErrorCount() {
        return errorCount.get();
    }
    
    public long getTotalProcessingTime() {
        return totalProcessingTime.get();
    }
    
    public double getAverageProcessingTime() {
        long processed = processedReviews.get();
        if (processed == 0) {
            return 0.0;
        }
        return (double) totalProcessingTime.get() / processed;
    }
    
    public Instant getStartTime() {
        return startTime;
    }
    
    public long getUptimeSeconds() {
        return Instant.now().getEpochSecond() - startTime.getEpochSecond();
    }
    
    public double getProcessingRate() {
        long uptime = getUptimeSeconds();
        if (uptime == 0) {
            return 0.0;
        }
        return (double) processedReviews.get() / uptime;
    }
    
    public double getErrorRate() {
        long total = processedReviews.get() + badReviews.get();
        if (total == 0) {
            return 0.0;
        }
        return (double) errorCount.get() / total;
    }
} 
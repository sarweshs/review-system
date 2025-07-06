package com.reviewconsumer.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class MetricsService {
    
    private static final Logger logger = LoggerFactory.getLogger(MetricsService.class);
    
    private final Counter processedReviewsCounter;
    private final Counter badReviewsCounter;
    private final Counter errorCounter;
    private final Timer processingTimer;
    private final AtomicLong totalProcessingTime = new AtomicLong(0);
    
    private final Instant startTime = Instant.now();
    
    public MetricsService(MeterRegistry meterRegistry) {
        this.processedReviewsCounter = Counter.builder("review_consumer_processed_reviews_total")
            .description("Total number of reviews processed successfully")
            .register(meterRegistry);
            
        this.badReviewsCounter = Counter.builder("review_consumer_bad_reviews_total")
            .description("Total number of bad reviews processed")
            .register(meterRegistry);
            
        this.errorCounter = Counter.builder("review_consumer_errors_total")
            .description("Total number of processing errors")
            .register(meterRegistry);
            
        this.processingTimer = Timer.builder("review_consumer_processing_duration")
            .description("Time taken to process reviews")
            .register(meterRegistry);
    }
    
    public void incrementProcessedReviews() {
        processedReviewsCounter.increment();
        logger.debug("Incremented processed reviews counter");
    }
    
    public void incrementBadReviews() {
        badReviewsCounter.increment();
        logger.debug("Incremented bad reviews counter");
    }
    
    public void incrementErrorCount() {
        errorCounter.increment();
        logger.debug("Incremented error counter");
    }
    
    public Timer.Sample startProcessingTimer() {
        return Timer.start();
    }
    
    public void stopProcessingTimer(Timer.Sample sample) {
        sample.stop(processingTimer);
    }
    
    public void addProcessingTime(long processingTimeMs) {
        totalProcessingTime.addAndGet(processingTimeMs);
        logger.debug("Added processing time: {}ms, total: {}ms", processingTimeMs, totalProcessingTime.get());
    }
    
    public long getProcessedReviews() {
        return (long) processedReviewsCounter.count();
    }
    
    public long getBadReviews() {
        return (long) badReviewsCounter.count();
    }
    
    public long getErrorCount() {
        return (long) errorCounter.count();
    }
    
    public long getTotalProcessingTime() {
        return totalProcessingTime.get();
    }
    
    public double getAverageProcessingTime() {
        long processed = getProcessedReviews();
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
        return (double) getProcessedReviews() / uptime;
    }
    
    public double getErrorRate() {
        long total = getProcessedReviews() + getBadReviews();
        if (total == 0) {
            return 0.0;
        }
        return (double) getErrorCount() / total;
    }
} 
package com.reviewproducer.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Gauge;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class MetricsService {
    
    // Micrometer counters and timers
    private final Counter filesProcessedCounter;
    private final Counter linesProcessedCounter;
    private final Counter validReviewsCounter;
    private final Counter invalidReviewsCounter;
    private final Counter processingErrorsCounter;
    private final Counter sourcesProcessedCounter;
    private final Counter filesFoundCounter;
    private final Counter filesQueuedCounter;
    private final Counter jobExecutionsCounter;
    private final Counter sourcesSuccessCounter;
    private final Counter sourcesFailureCounter;
    
    private final Timer fileProcessingTimer;
    private final Timer sourceProcessingTimer;
    private final Timer jobExecutionTimer;
    
    // Gauges for current state
    private final AtomicLong activeThreads = new AtomicLong(0);
    private final AtomicLong queueDepth = new AtomicLong(0);
    
    // Legacy counters for backward compatibility
    private final AtomicLong totalFilesProcessed = new AtomicLong(0);
    private final AtomicLong totalLinesProcessed = new AtomicLong(0);
    private final AtomicLong totalValidReviews = new AtomicLong(0);
    private final AtomicLong totalInvalidReviews = new AtomicLong(0);
    private final AtomicLong totalProcessingErrors = new AtomicLong(0);
    
    public MetricsService(MeterRegistry meterRegistry) {
        // Initialize counters
        this.filesProcessedCounter = Counter.builder("review_producer_files_processed_total")
            .description("Total number of files processed")
            .register(meterRegistry);
            
        this.linesProcessedCounter = Counter.builder("review_producer_lines_processed_total")
            .description("Total number of lines processed")
            .register(meterRegistry);
            
        this.validReviewsCounter = Counter.builder("review_producer_valid_reviews_total")
            .description("Total number of valid reviews processed")
            .register(meterRegistry);
            
        this.invalidReviewsCounter = Counter.builder("review_producer_invalid_reviews_total")
            .description("Total number of invalid reviews processed")
            .register(meterRegistry);
            
        this.processingErrorsCounter = Counter.builder("review_producer_processing_errors_total")
            .description("Total number of processing errors")
            .register(meterRegistry);
            
        this.sourcesProcessedCounter = Counter.builder("review_producer_sources_processed_total")
            .description("Total number of sources processed")
            .register(meterRegistry);
            
        this.filesFoundCounter = Counter.builder("review_producer_files_found_total")
            .description("Total number of files found")
            .register(meterRegistry);
            
        this.filesQueuedCounter = Counter.builder("review_producer_files_queued_total")
            .description("Total number of files queued for processing")
            .register(meterRegistry);
            
        this.jobExecutionsCounter = Counter.builder("review_producer_job_executions_total")
            .description("Total number of job executions")
            .register(meterRegistry);
            
        this.sourcesSuccessCounter = Counter.builder("review_producer_sources_success_total")
            .description("Total number of successful source processing")
            .register(meterRegistry);
            
        this.sourcesFailureCounter = Counter.builder("review_producer_sources_failure_total")
            .description("Total number of failed source processing")
            .register(meterRegistry);
        
        // Initialize timers
        this.fileProcessingTimer = Timer.builder("review_producer_file_processing_duration")
            .description("Time taken to process individual files")
            .register(meterRegistry);
            
        this.sourceProcessingTimer = Timer.builder("review_producer_source_processing_duration")
            .description("Time taken to process sources")
            .register(meterRegistry);
            
        this.jobExecutionTimer = Timer.builder("review_producer_job_execution_duration")
            .description("Time taken for complete job execution")
            .register(meterRegistry);
        
        // Initialize gauges
        Gauge.builder("review_producer_active_threads", activeThreads, AtomicLong::get)
            .description("Number of active processing threads")
            .register(meterRegistry);
            
        Gauge.builder("review_producer_queue_depth", queueDepth, AtomicLong::get)
            .description("Current depth of the processing queue")
            .register(meterRegistry);
    }
    
    /**
     * Record file processing metrics
     */
    public void recordFileProcessing(String sourceName, String fileName, int totalLines, int validLines, 
                                   int invalidLines, int emptyLines, long processingDurationMs) {
        
        // Increment counters
        filesProcessedCounter.increment();
        linesProcessedCounter.increment(totalLines);
        validReviewsCounter.increment(validLines);
        invalidReviewsCounter.increment(invalidLines);
        
        // Update legacy counters
        totalFilesProcessed.incrementAndGet();
        totalLinesProcessed.addAndGet(totalLines);
        totalValidReviews.addAndGet(validLines);
        totalInvalidReviews.addAndGet(invalidLines);
        
        // Record timing
        fileProcessingTimer.record(processingDurationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        
        log.info("Metrics recorded - File: {} from source: {}, Lines: {}/{}/{}/{}, Duration: {}ms", 
                fileName, sourceName, totalLines, validLines, invalidLines, emptyLines, processingDurationMs);
    }
    
    /**
     * Record source processing metrics
     */
    public void recordSourceProcessing(String sourceName, int totalFilesFound, int filesToProcess, 
                                     int filesQueued, long processingDurationMs) {
        
        // Increment counters
        sourcesProcessedCounter.increment();
        filesFoundCounter.increment(totalFilesFound);
        filesQueuedCounter.increment(filesQueued);
        
        // Record timing
        sourceProcessingTimer.record(processingDurationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        
        log.info("Source metrics recorded - Source: {}, Files: {}/{}/{}, Duration: {}ms", 
                sourceName, totalFilesFound, filesToProcess, filesQueued, processingDurationMs);
    }
    
    /**
     * Record job execution metrics
     */
    public void recordJobExecution(int totalSources, int successCount, int failureCount, 
                                 int totalFilesFound, int totalFilesToProcess, int totalFilesQueued, 
                                 long jobDurationMs) {
        
        // Increment counters
        jobExecutionsCounter.increment();
        sourcesSuccessCounter.increment(successCount);
        sourcesFailureCounter.increment(failureCount);
        processingErrorsCounter.increment(failureCount);
        
        // Update legacy counter
        totalProcessingErrors.addAndGet(failureCount);
        
        // Record timing
        jobExecutionTimer.record(jobDurationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        
        log.info("Job metrics recorded - Sources: {}/{}, Files: {}/{}/{}, Duration: {}ms", 
                successCount, totalSources, totalFilesFound, totalFilesToProcess, totalFilesQueued, jobDurationMs);
    }
    
    /**
     * Record queue and thread metrics
     */
    public void recordQueueMetrics(int activeThreads, int maxThreads, int queueDepth, int queueCapacity) {
        
        // Update gauges
        this.activeThreads.set(activeThreads);
        this.queueDepth.set(queueDepth);
        
        log.info("Queue metrics recorded - Threads: {}/{}, Queue: {}/{}", 
                activeThreads, maxThreads, queueDepth, queueCapacity);
    }
    
    /**
     * Get current metrics summary
     */
    public MetricsSummary getMetricsSummary() {
        return new MetricsSummary(
                totalFilesProcessed.get(),
                totalLinesProcessed.get(),
                totalValidReviews.get(),
                totalInvalidReviews.get(),
                totalProcessingErrors.get(),
                Instant.now()
        );
    }
    
    /**
     * Metrics summary for monitoring
     */
    public static class MetricsSummary {
        private final long totalFilesProcessed;
        private final long totalLinesProcessed;
        private final long totalValidReviews;
        private final long totalInvalidReviews;
        private final long totalProcessingErrors;
        private final Instant timestamp;
        
        public MetricsSummary(long totalFilesProcessed, long totalLinesProcessed, long totalValidReviews,
                            long totalInvalidReviews, long totalProcessingErrors, Instant timestamp) {
            this.totalFilesProcessed = totalFilesProcessed;
            this.totalLinesProcessed = totalLinesProcessed;
            this.totalValidReviews = totalValidReviews;
            this.totalInvalidReviews = totalInvalidReviews;
            this.totalProcessingErrors = totalProcessingErrors;
            this.timestamp = timestamp;
        }
        
        // Getters
        public long getTotalFilesProcessed() { return totalFilesProcessed; }
        public long getTotalLinesProcessed() { return totalLinesProcessed; }
        public long getTotalValidReviews() { return totalValidReviews; }
        public long getTotalInvalidReviews() { return totalInvalidReviews; }
        public long getTotalProcessingErrors() { return totalProcessingErrors; }
        public Instant getTimestamp() { return timestamp; }
        
        @Override
        public String toString() {
            return String.format("MetricsSummary{files=%d, lines=%d, valid=%d, invalid=%d, errors=%d, timestamp=%s}",
                    totalFilesProcessed, totalLinesProcessed, totalValidReviews, totalInvalidReviews, 
                    totalProcessingErrors, timestamp);
        }
    }
} 
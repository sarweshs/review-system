package com.reviewproducer.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class MetricsService {
    
    // Counters for tracking metrics
    private final AtomicLong totalFilesProcessed = new AtomicLong(0);
    private final AtomicLong totalLinesProcessed = new AtomicLong(0);
    private final AtomicLong totalValidReviews = new AtomicLong(0);
    private final AtomicLong totalInvalidReviews = new AtomicLong(0);
    private final AtomicLong totalProcessingErrors = new AtomicLong(0);
    
    /**
     * Record file processing metrics
     */
    public void recordFileProcessing(String sourceName, String fileName, int totalLines, int validLines, 
                                   int invalidLines, int emptyLines, long processingDurationMs) {
        
        totalFilesProcessed.incrementAndGet();
        totalLinesProcessed.addAndGet(totalLines);
        totalValidReviews.addAndGet(validLines);
        totalInvalidReviews.addAndGet(invalidLines);
        
        log.info("Metrics recorded - File: {} from source: {}, Lines: {}/{}/{}/{}, Duration: {}ms", 
                fileName, sourceName, totalLines, validLines, invalidLines, emptyLines, processingDurationMs);
        
        // TODO: Send to Prometheus/Grafana/other monitoring system
        // Example Prometheus metrics:
        // - review_files_processed_total{source="sourceName"} += 1
        // - review_lines_processed_total{source="sourceName", type="valid"} += validLines
        // - review_lines_processed_total{source="sourceName", type="invalid"} += invalidLines
        // - review_processing_duration_seconds{source="sourceName", file="fileName"} = processingDurationMs / 1000.0
    }
    
    /**
     * Record source processing metrics
     */
    public void recordSourceProcessing(String sourceName, int totalFilesFound, int filesToProcess, 
                                     int filesQueued, long processingDurationMs) {
        
        log.info("Source metrics recorded - Source: {}, Files: {}/{}/{}, Duration: {}ms", 
                sourceName, totalFilesFound, filesToProcess, filesQueued, processingDurationMs);
        
        // TODO: Send to monitoring system
        // - review_sources_processed_total{source="sourceName"} += 1
        // - review_files_found_total{source="sourceName"} += totalFilesFound
        // - review_files_queued_total{source="sourceName"} += filesQueued
        // - review_source_processing_duration_seconds{source="sourceName"} = processingDurationMs / 1000.0
    }
    
    /**
     * Record job execution metrics
     */
    public void recordJobExecution(int totalSources, int successCount, int failureCount, 
                                 int totalFilesFound, int totalFilesToProcess, int totalFilesQueued, 
                                 long jobDurationMs) {
        
        totalProcessingErrors.addAndGet(failureCount);
        
        log.info("Job metrics recorded - Sources: {}/{}, Files: {}/{}/{}, Duration: {}ms", 
                successCount, totalSources, totalFilesFound, totalFilesToProcess, totalFilesQueued, jobDurationMs);
        
        // TODO: Send to monitoring system
        // - review_job_executions_total += 1
        // - review_job_duration_seconds = jobDurationMs / 1000.0
        // - review_sources_success_total += successCount
        // - review_sources_failure_total += failureCount
        // - review_files_found_total += totalFilesFound
        // - review_files_processed_total += totalFilesToProcess
        // - review_files_queued_total += totalFilesQueued
    }
    
    /**
     * Record queue and thread metrics
     */
    public void recordQueueMetrics(int activeThreads, int maxThreads, int queueDepth, int queueCapacity) {
        
        log.info("Queue metrics recorded - Threads: {}/{}, Queue: {}/{}", 
                activeThreads, maxThreads, queueDepth, queueCapacity);
        
        // TODO: Send to monitoring system
        // - review_active_threads{thread_pool="file_processing"} = activeThreads
        // - review_thread_pool_capacity{thread_pool="file_processing"} = maxThreads
        // - review_queue_depth{queue="file_processing"} = queueDepth
        // - review_queue_capacity{queue="file_processing"} = queueCapacity
        // - review_queue_utilization{queue="file_processing"} = (double) queueDepth / queueCapacity
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
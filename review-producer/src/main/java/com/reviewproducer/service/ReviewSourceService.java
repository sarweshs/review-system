package com.reviewproducer.service;

import com.reviewcore.model.ReviewSource;
import com.reviewcore.model.Credential;
import com.reviewproducer.model.FileMetadata;
import com.reviewproducer.repository.ReviewSourceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class ReviewSourceService {
    private final ReviewSourceRepository reviewSourceRepository;
    private final CredentialService credentialService;
    private final StorageServiceFactory storageServiceFactory;
    private final ReviewKafkaProducerService kafkaProducerService;
    private final MetricsService metricsService;
    
    @Value("${review.producer.thread.pool.size:2}")
    private int threadPoolSize;
    
    @Value("${review.producer.queue.capacity:100}")
    private int queueCapacity;
    
    private BlockingQueue<FileProcessingTask> fileQueue;
    private ExecutorService executorService;
    private final AtomicInteger activeThreads = new AtomicInteger(0);

    @PostConstruct
    public void initQueueAndExecutor() {
        this.fileQueue = new LinkedBlockingQueue<>(queueCapacity);
        this.executorService = Executors.newFixedThreadPool(threadPoolSize);
        log.info("Initialized ReviewSourceService with {} threads and queue capacity {}", threadPoolSize, queueCapacity);
    }

    public ReviewSourceService(ReviewSourceRepository reviewSourceRepository, 
                             CredentialService credentialService,
                             StorageServiceFactory storageServiceFactory,
                             ReviewKafkaProducerService kafkaProducerService,
                             MetricsService metricsService) {
        this.reviewSourceRepository = reviewSourceRepository;
        this.credentialService = credentialService;
        this.storageServiceFactory = storageServiceFactory;
        this.kafkaProducerService = kafkaProducerService;
        this.metricsService = metricsService;
    }

    public List<ReviewSource> getActiveSources() {
        try {
            return reviewSourceRepository.findAllActive();
        } catch (Exception e) {
            log.error("Failed to fetch active review sources from database", e);
            return List.of();
        }
    }

    @Scheduled(fixedDelayString = "${review.producer.schedule.interval:300000}") // 5 minutes default
    public void processReviewSources() {
        log.info("Starting scheduled review source processing job");
        
        long jobStartTime = System.currentTimeMillis();
        int totalSources = 0;
        int successCount = 0;
        int failureCount = 0;
        int totalFilesFound = 0;
        int totalFilesToProcess = 0;
        int totalFilesQueued = 0;
        
        try {
            List<ReviewSource> activeSources = reviewSourceRepository.findAllActive();
            totalSources = activeSources.size();
            log.info("Found {} active review sources", totalSources);
            
            for (ReviewSource source : activeSources) {
                try {
                    ProcessingMetrics metrics = processReviewSource(source);
                    successCount++;
                    totalFilesFound += metrics.getTotalFilesFound();
                    totalFilesToProcess += metrics.getFilesToProcess();
                    totalFilesQueued += metrics.getFilesQueued();
                } catch (Exception e) {
                    log.error("Failed to process review source: {} - {}", source.getName(), e.getMessage(), e);
                    failureCount++;
                }
            }
            
            long jobDuration = System.currentTimeMillis() - jobStartTime;
            log.info("Review source processing completed in {} ms - Sources: {}/{}, Files Found: {}, Files to Process: {}, Files Queued: {}", 
                    jobDuration, successCount, totalSources, totalFilesFound, totalFilesToProcess, totalFilesQueued);
            
            // Record job execution metrics
            metricsService.recordJobExecution(totalSources, successCount, failureCount, 
                                            totalFilesFound, totalFilesToProcess, totalFilesQueued, jobDuration);
            
        } catch (Exception e) {
            log.error("Error in scheduled review source processing: {}", e.getMessage(), e);
        }
    }

    private ProcessingMetrics processReviewSource(ReviewSource source) {
        log.info("Processing review source: {} (URI: {})", source.getName(), source.getUri());
        
        long sourceStartTime = System.currentTimeMillis();
        ProcessingMetrics metrics = new ProcessingMetrics();
        
        Credential credentials = null;
        try {
            credentials = credentialService.decryptCredential(source.getCredentialJson());
        } catch (Exception e) {
            log.error("Failed to decrypt credentials for source: {} - {}", source.getName(), e.getMessage(), e);
            return metrics;
        }
        if (credentials == null) {
            log.error("Failed to decrypt credentials for source: {} (null returned)", source.getName());
            return metrics;
        }
        log.info("Successfully decrypted credentials for source: {}", source.getName());
        
        try {
            // Create storage service
            var storageService = storageServiceFactory.createStorageService(source.getUri(), credentials);
            
            // Extract prefix from URI
            String prefix = extractPrefixFromUri(source.getUri());
            log.info("Using prefix: {} for source: {}", prefix, source.getName());
            
            // List files with metadata
            List<FileMetadata> allFiles = storageService.listReviewFilesWithMetadata(prefix);
            metrics.setTotalFilesFound(allFiles.size());
            log.info("Found {} total .jl files with metadata in prefix: {} for source: {}", 
                    allFiles.size(), prefix, source.getName());
            
            if (allFiles.isEmpty()) {
                log.info("No .jl files found for source: {}", source.getName());
                return metrics;
            }
            
            // Log file details for metrics
            logFileMetrics(allFiles, source.getName(), "ALL_FILES");
            
            // Filter files based on lastProcessedTimestamp
            List<FileMetadata> filesToProcess = filterFilesByTimestamp(allFiles, 
                source.getLastProcessedTimestamp() != null ? 
                source.getLastProcessedTimestamp().atZone(java.time.ZoneOffset.UTC).toInstant() : null);
            
            metrics.setFilesToProcess(filesToProcess.size());
            log.info("Found {} files to process (after filtering by timestamp) for source: {} out of {} total files", 
                    filesToProcess.size(), source.getName(), allFiles.size());
            
            // Log filtered file details for metrics
            logFileMetrics(filesToProcess, source.getName(), "FILES_TO_PROCESS");
            
            if (filesToProcess.isEmpty()) {
                log.info("No new files to process for source: {}", source.getName());
                return metrics;
            }
            
            // Add files to processing queue
            int queuedFiles = 0;
            for (FileMetadata file : filesToProcess) {
                FileProcessingTask task = new FileProcessingTask(source, file, storageService);
                try {
                    if (fileQueue.offer(task, 30, TimeUnit.SECONDS)) {
                        log.debug("Added file to processing queue: {}", file.getName());
                        queuedFiles++;
                    } else {
                        log.warn("Failed to add file to processing queue (timeout): {}", file.getName());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Interrupted while adding file to queue: {}", file.getName());
                    break;
                }
            }
            
            metrics.setFilesQueued(queuedFiles);
            log.info("Successfully queued {} out of {} files for processing for source: {}", 
                    queuedFiles, filesToProcess.size(), source.getName());
            
            // Start processing threads if not already running
            startProcessingThreads();
            
        } catch (Exception e) {
            log.error("Error processing review source: {} - {}", source.getName(), e.getMessage(), e);
        }
        
        // Record source processing metrics
        long sourceDuration = System.currentTimeMillis() - sourceStartTime;
        metricsService.recordSourceProcessing(source.getName(), metrics.getTotalFilesFound(), 
                                            metrics.getFilesToProcess(), metrics.getFilesQueued(), sourceDuration);
        
        return metrics;
    }
    
    private List<FileMetadata> filterFilesByTimestamp(List<FileMetadata> files, Instant lastProcessedTimestamp) {
        if (lastProcessedTimestamp == null) {
            log.info("No last processed timestamp found, processing all {} files", files.size());
            return files;
        }
        
        List<FileMetadata> filteredFiles = files.stream()
                .filter(file -> file.getCreated().isAfter(lastProcessedTimestamp))
                .toList();
        
        log.info("Filtered {} files created after {} UTC (last processed timestamp)", 
                filteredFiles.size(), lastProcessedTimestamp);
        
        return filteredFiles;
    }
    
    private void startProcessingThreads() {
        int currentActive = activeThreads.get();
        if (currentActive < threadPoolSize) {
            int threadsToStart = threadPoolSize - currentActive;
            log.info("Starting {} processing threads (current active: {})", threadsToStart, currentActive);
            
            for (int i = 0; i < threadsToStart; i++) {
                executorService.submit(new FileProcessor());
            }
        }
        
        // Log queue and thread metrics
        log.info("Processing metrics - Active threads: {}/{}, Queue depth: {}/{}", 
                activeThreads.get(), threadPoolSize, fileQueue.size(), queueCapacity);
        
        // Record queue and thread metrics
        metricsService.recordQueueMetrics(activeThreads.get(), threadPoolSize, fileQueue.size(), queueCapacity);
    }

    private String extractPrefixFromUri(String uri) {
        try {
            java.net.URI parsedUri = new java.net.URI(uri);
            String path = parsedUri.getPath();
            
            // Remove leading slash
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            
            // Return empty string to search entire bucket
            return "";
        } catch (Exception e) {
            log.error("Error extracting prefix from URI: {} - {}", uri, e.getMessage(), e);
            return null;
        }
    }
    
    // Inner class for file processing task
    private static class FileProcessingTask {
        private final ReviewSource source;
        private final FileMetadata file;
        private final StorageService storageService;
        
        public FileProcessingTask(ReviewSource source, FileMetadata file, StorageService storageService) {
            this.source = source;
            this.file = file;
            this.storageService = storageService;
        }
        
        public ReviewSource getSource() { return source; }
        public FileMetadata getFile() { return file; }
        public StorageService getStorageService() { return storageService; }
    }
    
    // Inner class for file processor thread
    private class FileProcessor implements Runnable {
        @Override
        public void run() {
            activeThreads.incrementAndGet();
            log.info("File processor thread started. Active threads: {}", activeThreads.get());
            
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        // Wait for task with timeout
                        FileProcessingTask task = fileQueue.poll(60, TimeUnit.SECONDS);
                        
                        if (task == null) {
                            log.debug("No tasks in queue, continuing to wait...");
                            continue;
                        }
                        
                        processFile(task);
                        
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.info("File processor thread interrupted");
                        break;
                    } catch (Exception e) {
                        log.error("Error in file processor thread: {}", e.getMessage(), e);
                    }
                }
            } finally {
                activeThreads.decrementAndGet();
                log.info("File processor thread stopped. Active threads: {}", activeThreads.get());
            }
        }
        
        private void processFile(FileProcessingTask task) {
            ReviewSource source = task.getSource();
            FileMetadata file = task.getFile();
            StorageService storageService = task.getStorageService();
            
            log.info("Processing file: {} from source: {}", file.getName(), source.getName());
            
            try {
                // Download and process the file
                String fileContent = storageService.downloadFile(file.getKey());
                log.info("Successfully downloaded file: {} ({} bytes)", file.getName(), fileContent.length());
                
                // Process each line in the JSONL file
                processJsonlContent(fileContent, source.getName());
                
                // Update last processed timestamp
                source.setLastProcessedTimestamp(file.getCreated().atZone(java.time.ZoneOffset.UTC).toLocalDateTime());
                reviewSourceRepository.save(source);
                log.info("Updated last processed timestamp for source: {} to {} UTC", 
                        source.getName(), file.getCreated());
                
            } catch (Exception e) {
                log.error("Failed to process file: {} from source: {} - {}", 
                        file.getName(), source.getName(), e.getMessage(), e);
            }
        }
        
        /**
         * Process JSONL content line by line
         */
        private void processJsonlContent(String fileContent, String sourceName) {
            String[] lines = fileContent.split("\n");
            int totalLines = lines.length;
            int processedLines = 0;
            int validLines = 0;
            int invalidLines = 0;
            int emptyLines = 0;
            long processingStartTime = System.currentTimeMillis();
            
            log.info("Processing {} lines from file in source: {}", totalLines, sourceName);
            
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) {
                    emptyLines++;
                    continue;
                }
                
                try {
                    // Process each review line with validation
                    kafkaProducerService.processReviewLine(line);
                    processedLines++;
                    validLines++; // We'll count valid ones, invalid ones are logged separately
                    
                } catch (Exception e) {
                    log.error("Failed to process line in file from source {}: {}", sourceName, e.getMessage());
                    invalidLines++;
                }
            }
            
            long processingDuration = System.currentTimeMillis() - processingStartTime;
            log.info("File processing completed for source: {} - Total: {}, Processed: {}, Valid: {}, Invalid: {}, Empty: {}, Duration: {} ms", 
                    sourceName, totalLines, processedLines, validLines, invalidLines, emptyLines, processingDuration);
            
            // Record file processing metrics
            metricsService.recordFileProcessing(sourceName, "unknown", totalLines, validLines, invalidLines, emptyLines, processingDuration);
        }
    }
    
    /**
     * Log detailed file metrics for monitoring and future metric collection
     */
    private void logFileMetrics(List<FileMetadata> files, String sourceName, String metricType) {
        if (files.isEmpty()) {
            log.info("File metrics for {} - {}: 0 files", sourceName, metricType);
            return;
        }
        
        // Calculate metrics
        long totalSize = files.stream().mapToLong(FileMetadata::getSize).sum();
        Instant oldestFile = files.stream().map(FileMetadata::getCreated).min(Instant::compareTo).orElse(null);
        Instant newestFile = files.stream().map(FileMetadata::getCreated).max(Instant::compareTo).orElse(null);
        
        // Log comprehensive metrics
        log.info("File metrics for {} - {}: {} files, total size: {} bytes, size range: {} - {} bytes, " +
                "date range: {} - {}", 
                sourceName, metricType, files.size(), totalSize,
                files.stream().mapToLong(FileMetadata::getSize).min().orElse(0),
                files.stream().mapToLong(FileMetadata::getSize).max().orElse(0),
                oldestFile != null ? oldestFile.toString() : "N/A",
                newestFile != null ? newestFile.toString() : "N/A");
        
        // Log individual file details at DEBUG level
        if (log.isDebugEnabled()) {
            files.forEach(file -> log.debug("File: {}, Size: {} bytes, Created: {}, Key: {}", 
                    file.getName(), file.getSize(), file.getCreated(), file.getKey()));
        }
        
        // TODO: Send metrics to monitoring system (Prometheus, etc.)
        // This is where you would add metric collection for:
        // - Total files found per source
        // - Files to be processed per source
        // - File size distribution
        // - Processing queue depth
        // - Processing success/failure rates
    }
    
    /**
     * Metrics class for tracking processing statistics
     */
    private static class ProcessingMetrics {
        private int totalFilesFound = 0;
        private int filesToProcess = 0;
        private int filesQueued = 0;
        
        public int getTotalFilesFound() { return totalFilesFound; }
        public void setTotalFilesFound(int totalFilesFound) { this.totalFilesFound = totalFilesFound; }
        
        public int getFilesToProcess() { return filesToProcess; }
        public void setFilesToProcess(int filesToProcess) { this.filesToProcess = filesToProcess; }
        
        public int getFilesQueued() { return filesQueued; }
        public void setFilesQueued(int filesQueued) { this.filesQueued = filesQueued; }
    }
} 
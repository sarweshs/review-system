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
                             StorageServiceFactory storageServiceFactory) {
        this.reviewSourceRepository = reviewSourceRepository;
        this.credentialService = credentialService;
        this.storageServiceFactory = storageServiceFactory;
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
        
        try {
            List<ReviewSource> activeSources = reviewSourceRepository.findAllActive();
            log.info("Found {} active review sources", activeSources.size());
            
            int successCount = 0;
            int failureCount = 0;
            
            for (ReviewSource source : activeSources) {
                try {
                    processReviewSource(source);
                    successCount++;
                } catch (Exception e) {
                    log.error("Failed to process review source: {} - {}", source.getName(), e.getMessage(), e);
                    failureCount++;
                }
            }
            
            log.info("Review source processing completed. Success: {}, Failures: {}", successCount, failureCount);
            
        } catch (Exception e) {
            log.error("Error in scheduled review source processing: {}", e.getMessage(), e);
        }
    }

    private void processReviewSource(ReviewSource source) {
        log.info("Processing review source: {} (URI: {})", source.getName(), source.getUri());
        
        Credential credentials = null;
        try {
            credentials = credentialService.decryptCredential(source.getCredentialJson());
        } catch (Exception e) {
            log.error("Failed to decrypt credentials for source: {} - {}", source.getName(), e.getMessage(), e);
            return;
        }
        if (credentials == null) {
            log.error("Failed to decrypt credentials for source: {} (null returned)", source.getName());
            return;
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
            log.info("Found {} .jl files with metadata in prefix: {}", allFiles.size(), prefix);
            
            if (allFiles.isEmpty()) {
                log.info("No .jl files found for source: {}", source.getName());
                return;
            }
            
            // Filter files based on lastProcessedTimestamp
            List<FileMetadata> filesToProcess = filterFilesByTimestamp(allFiles, 
                source.getLastProcessedTimestamp() != null ? 
                source.getLastProcessedTimestamp().atZone(java.time.ZoneId.systemDefault()).toInstant() : null);
            log.info("Found {} files to process (after filtering by timestamp) for source: {}", 
                    filesToProcess.size(), source.getName());
            
            if (filesToProcess.isEmpty()) {
                log.info("No new files to process for source: {}", source.getName());
                return;
            }
            
            // Add files to processing queue
            for (FileMetadata file : filesToProcess) {
                FileProcessingTask task = new FileProcessingTask(source, file, storageService);
                try {
                    if (fileQueue.offer(task, 30, TimeUnit.SECONDS)) {
                        log.debug("Added file to processing queue: {}", file.getName());
                    } else {
                        log.warn("Failed to add file to processing queue (timeout): {}", file.getName());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Interrupted while adding file to queue: {}", file.getName());
                    break;
                }
            }
            
            // Start processing threads if not already running
            startProcessingThreads();
            
        } catch (Exception e) {
            log.error("Error processing review source: {} - {}", source.getName(), e.getMessage(), e);
        }
    }
    
    private List<FileMetadata> filterFilesByTimestamp(List<FileMetadata> files, Instant lastProcessedTimestamp) {
        if (lastProcessedTimestamp == null) {
            log.info("No last processed timestamp found, processing all {} files", files.size());
            return files;
        }
        
        List<FileMetadata> filteredFiles = files.stream()
                .filter(file -> file.getCreated().isAfter(lastProcessedTimestamp))
                .toList();
        
        log.info("Filtered {} files created after {} (last processed timestamp)", 
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
                
                // TODO: Parse JSONL content and send to Kafka
                log.info("Successfully downloaded file: {} ({} bytes)", file.getName(), fileContent.length());
                
                // Update last processed timestamp
                source.setLastProcessedTimestamp(file.getCreated().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
                reviewSourceRepository.save(source);
                log.info("Updated last processed timestamp for source: {} to {}", 
                        source.getName(), file.getCreated());
                
            } catch (Exception e) {
                log.error("Failed to process file: {} from source: {} - {}", 
                        file.getName(), source.getName(), e.getMessage(), e);
            }
        }
    }
} 
package com.reviewproducer.service;

import com.reviewcore.model.ReviewSource;
import com.reviewcore.model.Credential;
import com.reviewproducer.repository.ReviewSourceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class ReviewSourceService {
    private final ReviewSourceRepository reviewSourceRepository;
    private final CredentialService credentialService;
    private final StorageServiceFactory storageServiceFactory;

    @Value("${review-source-job.enabled:true}")
    private boolean jobEnabled;

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

    @Scheduled(fixedDelayString = "${review-source-job.fixed-delay-ms:60000}")
    public void scheduledPrintActiveSources() {
        if (!jobEnabled) {
            log.debug("Review source job is disabled");
            return;
        }
        
        log.info("Starting scheduled review source processing job");
        
        try {
            List<ReviewSource> sources = getActiveSources();
            
            if (sources.isEmpty()) {
                log.info("No active review sources found");
                return;
            }
            
            log.info("Found {} active review sources", sources.size());
            
            int successCount = 0;
            int failureCount = 0;
            
            for (ReviewSource source : sources) {
                try {
                    boolean success = processReviewSource(source);
                    if (success) {
                        successCount++;
                    } else {
                        failureCount++;
                    }
                } catch (Exception e) {
                    log.error("Unexpected error processing review source: {}", source.getName(), e);
                    failureCount++;
                }
            }
            
            log.info("Review source processing completed. Success: {}, Failures: {}", successCount, failureCount);
            
        } catch (Exception e) {
            log.error("Critical error in scheduled review source processing job", e);
        }
    }
    
    private boolean processReviewSource(ReviewSource source) {
        log.info("Processing review source: {} (URI: {})", source.getName(), source.getUri());
        
        try {
            // Validate URI format
            if (source.getUri() == null || source.getUri().trim().isEmpty()) {
                log.error("Invalid URI for source: {} - URI is null or empty", source.getName());
                return false;
            }
            
            // Decrypt credentials if present
            Credential credential = null;
            if (source.getCredentialJson() != null && !source.getCredentialJson().trim().isEmpty()) {
                try {
                    credential = credentialService.decryptCredential(source.getCredentialJson());
                    log.info("Successfully decrypted credentials for source: {}", source.getName());
                } catch (Exception e) {
                    log.error("Failed to decrypt credentials for source: {} - {}", source.getName(), e.getMessage(), e);
                    return false;
                }
            } else {
                log.info("No credentials found for source: {} - proceeding with anonymous access", source.getName());
            }
            
            // Create storage service
            StorageService storageService;
            try {
                storageService = storageServiceFactory.createStorageService(source.getUri(), credential);
                if (storageService == null) {
                    log.error("Failed to create storage service for source: {} - unsupported URI scheme or invalid configuration", source.getName());
                    return false;
                }
            } catch (Exception e) {
                log.error("Error creating storage service for source: {} - {}", source.getName(), e.getMessage(), e);
                return false;
            }
            
            // Extract prefix from URI path
            String prefix = extractPrefixFromUri(source.getUri());
            if (prefix == null) {
                log.error("Failed to extract prefix from URI for source: {}", source.getName());
                return false;
            }
            
            log.info("Using prefix: {} for source: {}", prefix, source.getName());
            
            // List files with timeout and retry logic
            List<String> files = listFilesWithRetry(storageService, prefix, source.getName());
            
            if (files == null) {
                // Error occurred during file listing
                return false;
            }
            
            if (files.isEmpty()) {
                log.info("No .jl files found for source: {}", source.getName());
            } else {
                log.info("Found {} .jl files for source: {}", files.size(), source.getName());
                
                // Log file names at DEBUG level
                for (String file : files) {
                    log.debug("File to process: {}", file);
                }
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("Error processing review source: {} - {}", source.getName(), e.getMessage(), e);
            return false;
        }
    }
    
    private List<String> listFilesWithRetry(StorageService storageService, String prefix, String sourceName) {
        int maxRetries = 3;
        int retryDelayMs = 1000;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return storageService.listReviewFiles(prefix);
            } catch (Exception e) {
                log.warn("Attempt {} failed to list files for source: {} - {}", attempt, sourceName, e.getMessage());
                
                if (attempt == maxRetries) {
                    log.error("All {} attempts failed to list files for source: {} - {}", maxRetries, sourceName, e.getMessage(), e);
                    return null;
                }
                
                try {
                    Thread.sleep(retryDelayMs * attempt); // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error("Interrupted while waiting for retry for source: {}", sourceName);
                    return null;
                }
            }
        }
        
        return null;
    }
    
    private String extractPrefixFromUri(String uri) {
        try {
            java.net.URI parsedUri = new java.net.URI(uri);
            String path = parsedUri.getPath();
            
            // Remove leading slash and return as prefix
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            
            // If path ends with slash, keep it as prefix
            if (!path.endsWith("/")) {
                path += "/";
            }
            
            return path;
        } catch (Exception e) {
            log.error("Error extracting prefix from URI: {} - {}", uri, e.getMessage(), e);
            return null;
        }
    }
} 
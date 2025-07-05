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
        return reviewSourceRepository.findAllActive();
    }

    @Scheduled(fixedDelayString = "${review-source-job.fixed-delay-ms:60000}")
    public void scheduledPrintActiveSources() {
        if (!jobEnabled) {
            log.debug("Review source job is disabled");
            return;
        }
        
        log.info("Starting scheduled review source processing job");
        List<ReviewSource> sources = getActiveSources();
        
        if (sources.isEmpty()) {
            log.info("No active review sources found");
            System.out.println("No active review sources found");
            return;
        }
        
        log.info("Found {} active review sources", sources.size());
        System.out.println("Found " + sources.size() + " active review sources");
        
        for (ReviewSource source : sources) {
            processReviewSource(source);
        }
    }
    
    private void processReviewSource(ReviewSource source) {
        log.info("Processing review source: {} (URI: {})", source.getName(), source.getUri());
        System.out.println("Processing review source: " + source.getName() + " (URI: " + source.getUri() + ")");
        
        try {
            // Decrypt credentials if present
            Credential credential = null;
            if (source.getCredentialJson() != null && !source.getCredentialJson().trim().isEmpty()) {
                try {
                    credential = credentialService.decryptCredential(source.getCredentialJson());
                    log.info("Successfully decrypted credentials for source: {}", source.getName());
                    System.out.println("Successfully decrypted credentials for source: " + source.getName());
                } catch (Exception e) {
                    log.error("Failed to decrypt credentials for source: {}", source.getName(), e);
                    System.out.println("Failed to decrypt credentials for source: " + source.getName() + " - " + e.getMessage());
                    return;
                }
            } else {
                log.info("No credentials found for source: {}", source.getName());
                System.out.println("No credentials found for source: " + source.getName());
            }
            
            // Create storage service
            StorageService storageService = storageServiceFactory.createStorageService(source.getUri(), credential);
            if (storageService == null) {
                log.error("Failed to create storage service for source: {}", source.getName());
                System.out.println("Failed to create storage service for source: " + source.getName());
                return;
            }
            
            // Extract prefix from URI path
            String prefix = extractPrefixFromUri(source.getUri());
            log.info("Using prefix: {} for source: {}", prefix, source.getName());
            System.out.println("Using prefix: " + prefix + " for source: " + source.getName());
            
            // List files
            List<String> files = storageService.listReviewFiles(prefix);
            
            if (files.isEmpty()) {
                log.info("No .jl files found for source: {}", source.getName());
                System.out.println("No .jl files found for source: " + source.getName());
            } else {
                log.info("Found {} .jl files for source: {}", files.size(), source.getName());
                System.out.println("Found " + files.size() + " .jl files for source: " + source.getName());
                
                // Print file names
                for (String file : files) {
                    log.info("File to process: {}", file);
                    System.out.println("  - " + file);
                }
            }
            
        } catch (Exception e) {
            log.error("Error processing review source: {}", source.getName(), e);
            System.out.println("Error processing review source: " + source.getName() + " - " + e.getMessage());
        }
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
            log.error("Error extracting prefix from URI: {}", uri, e);
            return "";
        }
    }
} 
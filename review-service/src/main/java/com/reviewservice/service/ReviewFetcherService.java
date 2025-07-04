package com.reviewservice.service;

import com.reviewservice.kafka.ReviewProducer;
import com.reviewservice.parser.ReviewParser;
import com.reviewservice.storage.StorageService;
import com.reviewservice.repository.ReviewSourceRepository;
import com.reviewcore.model.ReviewSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ReviewFetcherService {
    @Autowired
    private Map<String, StorageService> storageServices;
    @Autowired
    private ReviewProducer reviewProducer;
    @Autowired
    private ReviewParser reviewParser;
    @Autowired
    private ReviewSourceRepository reviewSourceRepository;
    @Autowired
    private CredentialService credentialService;

    @Scheduled(fixedDelayString = "${fetcher.poll-interval-ms:60000}")
    public void fetchAndProcessReviews() {
        List<ReviewSource> sources = reviewSourceRepository.findAll();
        for (ReviewSource source : sources) {
            // Determine backend from URI
            String backend = determineBackendFromUri(source.getUri());
            StorageService storageService = storageServices.get(backend + "StorageService");
            if (storageService == null) {
                // Log or handle missing storage service
                continue;
            }
            // --- Credential handling logic ---
            // If no credential, fetch directly
            if (source.getCredentialJson() == null || source.getCredentialJson().isEmpty()) {
                // Download from source directly, no auth headers
                // Example: storageService.getFile(key) or similar
            } else {
                try {
                    // Decrypt credential and get headers
                    var credential = credentialService.decryptCredential(source.getCredentialJson());
                    var headers = credentialService.getAuthHeaders(credential);
                    // Use headers in your storageService implementation if needed
                } catch (Exception e) {
                    // Log error and skip this source
                    continue;
                }
            }
            // TODO: Implement logic to process files for this source using storageService
        }
    }
    
    /**
     * Determines the backend type from the URI
     */
    private String determineBackendFromUri(String uri) {
        if (uri == null || uri.trim().isEmpty()) {
            return "s3"; // default to S3
        }
        
        String lowerUri = uri.toLowerCase();
        if (lowerUri.startsWith("s3://")) {
            return "s3";
        } else if (lowerUri.startsWith("minio://")) {
            return "minio";
        } else if (lowerUri.startsWith("gcs://") || lowerUri.startsWith("gs://")) {
            return "gcs";
        } else {
            // Default to S3 if no protocol specified
            return "s3";
        }
    }
} 
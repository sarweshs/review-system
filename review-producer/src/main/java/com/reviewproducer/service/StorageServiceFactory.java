package com.reviewproducer.service;

import com.reviewcore.model.Credential;
import com.reviewcore.model.AwsCredential;
import com.reviewcore.model.BasicCredential;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
@Component
public class StorageServiceFactory {
    
    /**
     * Creates and configures a storage service based on the URI and credentials
     */
    public StorageService createStorageService(String uri, Credential credential) {
        if (uri == null || uri.trim().isEmpty()) {
            log.error("Cannot create storage service: URI is null or empty");
            return null;
        }
        
        try {
            URI parsedUri = new URI(uri);
            String scheme = parsedUri.getScheme();
            String endpoint = scheme + "://" + parsedUri.getHost();
            if (parsedUri.getPort() != -1) {
                endpoint += ":" + parsedUri.getPort();
            }
            String bucket = parsedUri.getPath().substring(1); // Remove leading slash
            
            log.info("Creating storage service for scheme: {}, endpoint: {}, bucket: {}", scheme, endpoint, bucket);
            
            if ("https".equals(scheme) || "s3".equals(scheme)) {
                return createS3StorageService(endpoint, bucket, credential);
            } else if ("http".equals(scheme) || "minio".equals(scheme)) {
                return createMinIOStorageService(endpoint, bucket, credential);
            } else {
                log.error("Unsupported storage scheme: {}", scheme);
                return null;
            }
            
        } catch (URISyntaxException e) {
            log.error("Invalid URI format: {} - {}", uri, e.getMessage(), e);
            return null;
        } catch (Exception e) {
            log.error("Error creating storage service for URI: {} - {}", uri, e.getMessage(), e);
            return null;
        }
    }
    
    private StorageService createS3StorageService(String endpoint, String bucket, Credential credential) {
        if (!(credential instanceof AwsCredential)) {
            log.error("Invalid credential type for S3 storage. Expected AwsCredential, got: {}", 
                    credential != null ? credential.getClass().getSimpleName() : "null");
            return null;
        }
        
        try {
            AwsCredential awsCredential = (AwsCredential) credential;
            S3StorageService s3Service = new S3StorageService();
            s3Service.initialize(endpoint, bucket, awsCredential);
            return s3Service;
            
        } catch (Exception e) {
            log.error("Failed to create S3 storage service for endpoint: {} and bucket: {} - {}", 
                    endpoint, bucket, e.getMessage(), e);
            return null;
        }
    }
    
    private StorageService createMinIOStorageService(String endpoint, String bucket, Credential credential) {
        if (!(credential instanceof BasicCredential)) {
            log.error("Invalid credential type for MinIO storage. Expected BasicCredential, got: {}", 
                    credential != null ? credential.getClass().getSimpleName() : "null");
            return null;
        }
        
        try {
            BasicCredential basicCredential = (BasicCredential) credential;
            MinIOStorageService minioService = new MinIOStorageService();
            minioService.initialize(endpoint, bucket, basicCredential);
            return minioService;
            
        } catch (Exception e) {
            log.error("Failed to create MinIO storage service for endpoint: {} and bucket: {} - {}", 
                    endpoint, bucket, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Determines the storage type from URI
     */
    public String getStorageType(String uri) {
        if (uri == null || uri.trim().isEmpty()) {
            log.error("Cannot determine storage type: URI is null or empty");
            return "unknown";
        }
        
        try {
            URI parsedUri = new URI(uri);
            String scheme = parsedUri.getScheme();
            
            if (scheme == null || scheme.trim().isEmpty()) {
                log.error("Cannot determine storage type: URI scheme is missing - {}", uri);
                return "unknown";
            }
            
            switch (scheme.toLowerCase()) {
                case "s3":
                case "https":
                    return "s3";
                case "minio":
                case "http":
                    return "minio";
                default:
                    log.warn("Unknown storage scheme: {} for URI: {}", scheme, uri);
                    return "unknown";
            }
        } catch (URISyntaxException e) {
            log.error("Invalid URI format: {} - {}", uri, e.getMessage(), e);
            return "unknown";
        } catch (Exception e) {
            log.error("Unexpected error determining storage type for URI: {} - {}", uri, e.getMessage(), e);
            return "unknown";
        }
    }
} 
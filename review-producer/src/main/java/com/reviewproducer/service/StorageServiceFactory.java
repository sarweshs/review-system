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
            String host = parsedUri.getHost();
            int port = parsedUri.getPort();
            String path = parsedUri.getPath();
            
            // Validate URI components
            if (scheme == null || scheme.trim().isEmpty()) {
                log.error("Cannot create storage service: URI scheme is missing - {}", uri);
                return null;
            }
            
            if (host == null || host.trim().isEmpty()) {
                log.error("Cannot create storage service: URI host is missing - {}", uri);
                return null;
            }
            
            if (path == null || path.trim().isEmpty()) {
                log.error("Cannot create storage service: URI path is missing - {}", uri);
                return null;
            }
            
            // Extract bucket name from path (remove leading slash)
            String bucketName = path.startsWith("/") ? path.substring(1) : path;
            if (bucketName.endsWith("/")) {
                bucketName = bucketName.substring(0, bucketName.length() - 1);
            }
            
            if (bucketName.trim().isEmpty()) {
                log.error("Cannot create storage service: bucket name is empty - {}", uri);
                return null;
            }
            
            // Build endpoint URL
            String endpoint = scheme + "://" + host;
            if (port > 0) {
                endpoint += ":" + port;
            }
            
            log.info("Creating storage service for scheme: {}, endpoint: {}, bucket: {}", scheme, endpoint, bucketName);
            
            switch (scheme.toLowerCase()) {
                case "s3":
                case "https":
                    // Handle S3/Storj
                    if (credential == null) {
                        log.error("AWS credentials required for S3/Storj access but none provided");
                        return null;
                    }
                    
                    if (!(credential instanceof AwsCredential)) {
                        log.error("Invalid credential type for S3/Storj access. Expected AwsCredential, got: {}", 
                                credential.getClass().getSimpleName());
                        return null;
                    }
                    
                    AwsCredential awsCredential = (AwsCredential) credential;
                    if (awsCredential.getAccessKeyId() == null || awsCredential.getAccessKeyId().trim().isEmpty()) {
                        log.error("AWS access key ID is missing or empty");
                        return null;
                    }
                    
                    if (awsCredential.getSecretAccessKey() == null || awsCredential.getSecretAccessKey().trim().isEmpty()) {
                        log.error("AWS secret access key is missing or empty");
                        return null;
                    }
                    
                    try {
                        S3StorageService s3Service = new S3StorageService();
                        s3Service.initialize(awsCredential, endpoint, bucketName);
                        return s3Service;
                    } catch (Exception e) {
                        log.error("Failed to initialize S3 storage service for endpoint: {} and bucket: {} - {}", 
                                endpoint, bucketName, e.getMessage(), e);
                        return null;
                    }
                    
                case "minio":
                case "http":
                    // Handle MinIO
                    if (credential == null) {
                        log.error("Basic credentials required for MinIO access but none provided");
                        return null;
                    }
                    
                    if (!(credential instanceof BasicCredential)) {
                        log.error("Invalid credential type for MinIO access. Expected BasicCredential, got: {}", 
                                credential.getClass().getSimpleName());
                        return null;
                    }
                    
                    BasicCredential basicCredential = (BasicCredential) credential;
                    if (basicCredential.getUsername() == null || basicCredential.getUsername().trim().isEmpty()) {
                        log.error("MinIO username is missing or empty");
                        return null;
                    }
                    
                    if (basicCredential.getPassword() == null || basicCredential.getPassword().trim().isEmpty()) {
                        log.error("MinIO password is missing or empty");
                        return null;
                    }
                    
                    try {
                        MinIOStorageService minioService = new MinIOStorageService();
                        minioService.initialize(basicCredential, endpoint, bucketName);
                        return minioService;
                    } catch (Exception e) {
                        log.error("Failed to initialize MinIO storage service for endpoint: {} and bucket: {} - {}", 
                                endpoint, bucketName, e.getMessage(), e);
                        return null;
                    }
                    
                default:
                    log.error("Unsupported storage scheme: {} for URI: {}", scheme, uri);
                    return null;
            }
            
        } catch (URISyntaxException e) {
            log.error("Invalid URI format: {} - {}", uri, e.getMessage(), e);
            return null;
        } catch (Exception e) {
            log.error("Unexpected error creating storage service for URI: {} - {}", uri, e.getMessage(), e);
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
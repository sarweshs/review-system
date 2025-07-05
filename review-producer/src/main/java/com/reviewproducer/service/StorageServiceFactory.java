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
        try {
            URI parsedUri = new URI(uri);
            String scheme = parsedUri.getScheme();
            String host = parsedUri.getHost();
            int port = parsedUri.getPort();
            String path = parsedUri.getPath();
            
            // Extract bucket name from path (remove leading slash)
            String bucketName = path.startsWith("/") ? path.substring(1) : path;
            if (bucketName.endsWith("/")) {
                bucketName = bucketName.substring(0, bucketName.length() - 1);
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
                    if (credential instanceof AwsCredential) {
                        S3StorageService s3Service = new S3StorageService();
                        s3Service.initialize((AwsCredential) credential, endpoint, bucketName);
                        return s3Service;
                    } else {
                        log.error("AWS credentials required for S3/Storj access");
                        return null;
                    }
                    
                case "minio":
                case "http":
                    // Handle MinIO
                    if (credential instanceof BasicCredential) {
                        MinIOStorageService minioService = new MinIOStorageService();
                        minioService.initialize((BasicCredential) credential, endpoint, bucketName);
                        return minioService;
                    } else {
                        log.error("Basic credentials required for MinIO access");
                        return null;
                    }
                    
                default:
                    log.error("Unsupported storage scheme: {}", scheme);
                    return null;
            }
            
        } catch (URISyntaxException e) {
            log.error("Invalid URI format: {}", uri, e);
            return null;
        } catch (Exception e) {
            log.error("Error creating storage service for URI: {}", uri, e);
            return null;
        }
    }
    
    /**
     * Determines the storage type from URI
     */
    public String getStorageType(String uri) {
        try {
            URI parsedUri = new URI(uri);
            String scheme = parsedUri.getScheme();
            
            switch (scheme.toLowerCase()) {
                case "s3":
                case "https":
                    return "s3";
                case "minio":
                case "http":
                    return "minio";
                default:
                    return "unknown";
            }
        } catch (URISyntaxException e) {
            log.error("Invalid URI format: {}", uri, e);
            return "unknown";
        }
    }
} 
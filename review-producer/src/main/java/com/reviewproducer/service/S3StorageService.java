package com.reviewproducer.service;

import com.reviewcore.model.AwsCredential;
import com.reviewproducer.model.FileMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class S3StorageService implements StorageService {
    
    private S3Client s3Client;
    private String bucketName;
    private String endpoint;
    
    public S3StorageService() {}
    
    public void initialize(String endpoint, String bucketName, AwsCredential credential) {
        this.endpoint = endpoint;
        this.bucketName = bucketName;
        
        try {
            log.info("Initializing S3 client for endpoint: {} and bucket: {}", endpoint, bucketName);
            
            this.s3Client = S3Client.builder()
                    .endpointOverride(URI.create(endpoint))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(credential.getAccessKeyId(), credential.getSecretAccessKey())))
                    .region(Region.US_EAST_1)
                    .overrideConfiguration(config -> config
                            .apiCallTimeout(Duration.ofSeconds(30))
                            .apiCallAttemptTimeout(Duration.ofSeconds(10)))
                    .build();
            
            // Test connection
            HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build();
            s3Client.headBucket(headBucketRequest);
            
            log.info("Successfully connected to S3 bucket: {}", bucketName);
            
        } catch (SdkException e) {
            log.error("Failed to initialize S3 client for endpoint: {} and bucket: {} - {}", 
                    endpoint, bucketName, e.getMessage(), e);
            throw new RuntimeException("Failed to initialize S3 client", e);
        }
    }
    
    @Override
    public List<String> listReviewFiles(String prefix) {
        try {
            log.debug("Listing S3 objects in bucket: {} with prefix: {}", bucketName, prefix);
            
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .build();
            
            ListObjectsV2Response response = s3Client.listObjectsV2(request);
            
            List<String> files = response.contents().stream()
                    .filter(obj -> obj.key().endsWith(".jl"))
                    .map(S3Object::key)
                    .collect(Collectors.toList());
            
            log.info("Found {} .jl files in prefix: {}", files.size(), prefix);
            return files;
            
        } catch (SdkException e) {
            log.error("Error listing S3 files in bucket: {} with prefix: {} - {}", 
                    bucketName, prefix, e.getMessage(), e);
            throw new RuntimeException("Failed to list S3 files", e);
        }
    }
    
    @Override
    public List<String> listReviewFilesRecursive(String prefix) {
        try {
            log.debug("Listing S3 objects recursively in bucket: {} with prefix: {}", bucketName, prefix);
            
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .build();
            
            ListObjectsV2Response response = s3Client.listObjectsV2(request);
            
            List<String> files = response.contents().stream()
                    .filter(obj -> obj.key().endsWith(".jl"))
                    .map(S3Object::key)
                    .collect(Collectors.toList());
            
            log.info("Found {} .jl files recursively in prefix: {}", files.size(), prefix);
            
            // Log file names at DEBUG level
            for (String file : files) {
                log.debug("Found file: {}", file);
            }
            
            return files;
            
        } catch (SdkException e) {
            log.error("Error listing S3 files recursively in bucket: {} with prefix: {} - {}", 
                    bucketName, prefix, e.getMessage(), e);
            throw new RuntimeException("Failed to list S3 files recursively", e);
        }
    }
    
    @Override
    public List<FileMetadata> listReviewFilesWithMetadata(String prefix) {
        try {
            log.info("Listing S3 objects with metadata recursively in bucket: {} with prefix: {}", bucketName, prefix);
            
            // Try listing without prefix first to see what's in the bucket
            ListObjectsV2Request requestWithoutPrefix = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .build();
            
            ListObjectsV2Response responseWithoutPrefix = s3Client.listObjectsV2(requestWithoutPrefix);
            log.info("Total objects found in bucket {} (no prefix): {}", bucketName, responseWithoutPrefix.contents().size());
            
            // Print first few objects to understand bucket structure
            int count = 0;
            for (S3Object obj : responseWithoutPrefix.contents()) {
                if (count < 10) { // Only log first 10 objects
                    log.info("Found object (no prefix): {} (size: {} bytes, modified: {})", 
                            obj.key(), obj.size(), obj.lastModified());
                    count++;
                } else {
                    break;
                }
            }
            
            // Now try with the actual prefix
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .build();
            
            ListObjectsV2Response response = s3Client.listObjectsV2(request);
            
            // First, let's print ALL objects found to understand the structure
            log.info("Total objects found in bucket {} with prefix {}: {}", bucketName, prefix, response.contents().size());
            
            if (response.contents().isEmpty()) {
                log.warn("No objects found in bucket {} with prefix {}", bucketName, prefix);
                return new ArrayList<>();
            }
            
            // Print all objects for debugging
            for (S3Object obj : response.contents()) {
                log.info("Found object: {} (size: {} bytes, modified: {})", 
                        obj.key(), obj.size(), obj.lastModified());
            }
            
            List<FileMetadata> files = new ArrayList<>();
            
            for (S3Object obj : response.contents()) {
                if (obj.key().endsWith(".jl")) {
                    String fileName = obj.key().substring(obj.key().lastIndexOf('/') + 1);
                    
                    FileMetadata metadata = new FileMetadata(
                            fileName,
                            obj.key(),
                            obj.size(),
                            obj.lastModified(),
                            obj.lastModified(), // S3 doesn't provide creation time, using lastModified
                            obj.eTag(),
                            "application/jsonl"
                    );
                    
                    files.add(metadata);
                    log.info("Found .jl file: {} (size: {} bytes, modified: {})", 
                            obj.key(), obj.size(), obj.lastModified());
                }
            }
            
            log.info("Found {} .jl files with metadata recursively in prefix: {}", files.size(), prefix);
            
            // Log subfolder structure
            Set<String> subfolders = response.contents().stream()
                    .map(obj -> {
                        String key = obj.key();
                        int lastSlash = key.lastIndexOf('/');
                        return lastSlash > 0 ? key.substring(0, lastSlash) : "";
                    })
                    .filter(folder -> !folder.isEmpty())
                    .collect(Collectors.toSet());
            
            log.info("Found {} subfolders:", subfolders.size());
            for (String subfolder : subfolders) {
                log.info("  - Subfolder: {}", subfolder);
            }
            
            return files;
            
        } catch (SdkException e) {
            log.error("Error listing S3 files with metadata in bucket: {} with prefix: {} - {}", 
                    bucketName, prefix, e.getMessage(), e);
            throw new RuntimeException("Failed to list S3 files with metadata", e);
        }
    }
    
    @Override
    public byte[] getFile(String key) {
        try {
            log.debug("Getting S3 file: {} from bucket: {}", key, bucketName);
            
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            
            byte[] content = s3Client.getObjectAsBytes(request).asByteArray();
            
            log.debug("Successfully retrieved S3 file: {} ({} bytes)", key, content.length);
            return content;
            
        } catch (SdkException e) {
            log.error("Error getting S3 file {} from bucket: {} - {}", key, bucketName, e.getMessage(), e);
            throw new RuntimeException("Failed to get S3 file", e);
        }
    }
    
    @Override
    public String downloadFile(String key) {
        try {
            log.debug("Downloading S3 file: {} from bucket: {}", key, bucketName);
            
            byte[] fileBytes = getFile(key);
            String content = new String(fileBytes, java.nio.charset.StandardCharsets.UTF_8);
            
            log.debug("Successfully downloaded S3 file: {} ({} characters)", key, content.length());
            return content;
            
        } catch (Exception e) {
            log.error("Error downloading S3 file {} from bucket: {} - {}", key, bucketName, e.getMessage(), e);
            throw new RuntimeException("Failed to download S3 file: " + key, e);
        }
    }
    
    @Override
    public FileMetadata getFileMetadata(String key) {
        try {
            log.debug("Getting metadata for S3 file: {} from bucket: {}", key, bucketName);
            
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            
            HeadObjectResponse response = s3Client.headObject(request);
            
            String fileName = key.substring(key.lastIndexOf('/') + 1);
            
            FileMetadata metadata = new FileMetadata(
                    fileName,
                    key,
                    response.contentLength(),
                    response.lastModified(),
                    response.lastModified(), // S3 doesn't provide creation time
                    response.eTag(),
                    response.contentType()
            );
            
            log.debug("Retrieved metadata for file: {} (size: {} bytes, modified: {})", 
                    key, response.contentLength(), response.lastModified());
            
            return metadata;
            
        } catch (SdkException e) {
            log.error("Error getting metadata for S3 file {} from bucket: {} - {}", 
                    key, bucketName, e.getMessage(), e);
            throw new RuntimeException("Failed to get S3 file metadata", e);
        }
    }
    
    @Override
    public boolean fileExists(String key) {
        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            
            s3Client.headObject(request);
            return true;
            
        } catch (NoSuchKeyException e) {
            return false;
        } catch (SdkException e) {
            log.error("Error checking if S3 file exists: {} in bucket: {} - {}", 
                    key, bucketName, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public String getStorageType() {
        return "s3";
    }
} 
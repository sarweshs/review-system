package com.reviewproducer.service;

import com.reviewcore.model.AwsCredential;
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
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class S3StorageService implements StorageService {
    
    private S3Client s3Client;
    private String bucketName;
    private String endpoint;
    
    public S3StorageService() {
        // Default constructor for Spring
    }
    
    public void initialize(AwsCredential credential, String endpoint, String bucketName) {
        if (credential == null) {
            throw new IllegalArgumentException("AWS credential cannot be null");
        }
        
        if (endpoint == null || endpoint.trim().isEmpty()) {
            throw new IllegalArgumentException("Endpoint cannot be null or empty");
        }
        
        if (bucketName == null || bucketName.trim().isEmpty()) {
            throw new IllegalArgumentException("Bucket name cannot be null or empty");
        }
        
        this.endpoint = endpoint;
        this.bucketName = bucketName;
        
        try {
            this.s3Client = S3Client.builder()
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(credential.getAccessKeyId(), credential.getSecretAccessKey())))
                    .endpointOverride(URI.create(endpoint))
                    .region(Region.US_EAST_1) // required but not used by Storj
                    .overrideConfiguration(builder -> builder
                            .apiCallTimeout(Duration.ofSeconds(30))
                            .apiCallAttemptTimeout(Duration.ofSeconds(10)))
                    .build();
            
            log.info("Initialized S3 client for endpoint: {} and bucket: {}", endpoint, bucketName);
            
            // Test the connection by checking if bucket exists
            testConnection();
            
        } catch (Exception e) {
            log.error("Failed to initialize S3 client for endpoint: {} and bucket: {} - {}", 
                    endpoint, bucketName, e.getMessage(), e);
            throw new RuntimeException("Failed to initialize S3 client", e);
        }
    }
    
    private void testConnection() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
            log.info("Successfully connected to S3 bucket: {}", bucketName);
        } catch (NoSuchBucketException e) {
            log.error("S3 bucket does not exist: {} - {}", bucketName, e.getMessage());
            throw new RuntimeException("S3 bucket does not exist: " + bucketName, e);
        } catch (S3Exception e) {
            if (e.statusCode() == 403) {
                log.error("Access denied to S3 bucket: {} - check credentials and permissions - {}", 
                        bucketName, e.getMessage());
                throw new RuntimeException("Access denied to S3 bucket: " + bucketName, e);
            } else {
                log.error("S3 error testing connection to bucket: {} - {}", bucketName, e.getMessage(), e);
                throw new RuntimeException("S3 error testing connection to bucket: " + bucketName, e);
            }
        } catch (SdkException e) {
            log.error("SDK error testing connection to S3 bucket: {} - {}", bucketName, e.getMessage(), e);
            throw new RuntimeException("SDK error testing connection to S3 bucket: " + bucketName, e);
        } catch (Exception e) {
            log.error("Unexpected error testing connection to S3 bucket: {} - {}", bucketName, e.getMessage(), e);
            throw new RuntimeException("Unexpected error testing connection to S3 bucket: " + bucketName, e);
        }
    }
    
    @Override
    public List<String> listReviewFiles(String prefix) {
        if (s3Client == null) {
            log.error("S3 client is not initialized");
            throw new IllegalStateException("S3 client is not initialized");
        }
        
        if (prefix == null) {
            prefix = "";
        }
        
        try {
            log.debug("Listing S3 objects in bucket: {} with prefix: {}", bucketName, prefix);
            
            ListObjectsV2Response response = s3Client.listObjectsV2(
                ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .maxKeys(1000) // Limit results to avoid timeouts
                    .build()
            );
            
            List<String> files = response.contents().stream()
                    .map(S3Object::key)
                    .filter(key -> key.endsWith(".jl"))
                    .collect(Collectors.toList());
            
            log.info("Found {} .jl files in prefix: {}", files.size(), prefix);
            return files;
            
        } catch (S3Exception e) {
            if (e.statusCode() == 403) {
                log.error("Access denied listing S3 objects in bucket: {} with prefix: {} - {}", 
                        bucketName, prefix, e.getMessage());
                throw new RuntimeException("Access denied listing S3 objects in bucket: " + bucketName, e);
            } else if (e.statusCode() == 404) {
                log.error("S3 bucket not found: {} - {}", bucketName, e.getMessage());
                throw new RuntimeException("S3 bucket not found: " + bucketName, e);
            } else {
                log.error("S3 error listing objects in bucket: {} with prefix: {} - {}", 
                        bucketName, prefix, e.getMessage(), e);
                throw new RuntimeException("S3 error listing objects in bucket: " + bucketName, e);
            }
        } catch (SdkException e) {
            log.error("SDK error listing S3 objects in bucket: {} with prefix: {} - {}", 
                    bucketName, prefix, e.getMessage(), e);
            throw new RuntimeException("SDK error listing S3 objects in bucket: " + bucketName, e);
        } catch (Exception e) {
            log.error("Unexpected error listing S3 objects in bucket: {} with prefix: {} - {}", 
                    bucketName, prefix, e.getMessage(), e);
            throw new RuntimeException("Unexpected error listing S3 objects in bucket: " + bucketName, e);
        }
    }

    @Override
    public byte[] getFile(String key) {
        if (s3Client == null) {
            log.error("S3 client is not initialized");
            throw new IllegalStateException("S3 client is not initialized");
        }
        
        if (key == null || key.trim().isEmpty()) {
            log.error("S3 object key cannot be null or empty");
            throw new IllegalArgumentException("S3 object key cannot be null or empty");
        }
        
        try {
            log.debug("Downloading S3 object: {} from bucket: {}", key, bucketName);
            
            byte[] content = s3Client.getObjectAsBytes(
                GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build()
            ).asByteArray();
            
            log.info("Downloaded file: {} ({} bytes) from bucket: {}", key, content.length, bucketName);
            return content;
            
        } catch (NoSuchKeyException e) {
            log.error("S3 object not found: {} in bucket: {} - {}", key, bucketName, e.getMessage());
            throw new RuntimeException("S3 object not found: " + key + " in bucket: " + bucketName, e);
        } catch (S3Exception e) {
            if (e.statusCode() == 403) {
                log.error("Access denied downloading S3 object: {} from bucket: {} - {}", 
                        key, bucketName, e.getMessage());
                throw new RuntimeException("Access denied downloading S3 object: " + key + " from bucket: " + bucketName, e);
            } else {
                log.error("S3 error downloading object: {} from bucket: {} - {}", 
                        key, bucketName, e.getMessage(), e);
                throw new RuntimeException("S3 error downloading object: " + key + " from bucket: " + bucketName, e);
            }
        } catch (SdkException e) {
            log.error("SDK error downloading S3 object: {} from bucket: {} - {}", 
                    key, bucketName, e.getMessage(), e);
            throw new RuntimeException("SDK error downloading S3 object: " + key + " from bucket: " + bucketName, e);
        } catch (Exception e) {
            log.error("Unexpected error downloading S3 object: {} from bucket: {} - {}", 
                    key, bucketName, e.getMessage(), e);
            throw new RuntimeException("Unexpected error downloading S3 object: " + key + " from bucket: " + bucketName, e);
        }
    }

    @Override
    public boolean fileExists(String key) {
        if (s3Client == null) {
            log.error("S3 client is not initialized");
            return false;
        }
        
        if (key == null || key.trim().isEmpty()) {
            log.error("S3 object key cannot be null or empty");
            return false;
        }
        
        try {
            s3Client.headObject(
                HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build()
            );
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            if (e.statusCode() == 403) {
                log.error("Access denied checking existence of S3 object: {} in bucket: {} - {}", 
                        key, bucketName, e.getMessage());
            } else {
                log.error("S3 error checking existence of object: {} in bucket: {} - {}", 
                        key, bucketName, e.getMessage());
            }
            return false;
        } catch (Exception e) {
            log.error("Unexpected error checking existence of S3 object: {} in bucket: {} - {}", 
                    key, bucketName, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String getStorageType() {
        return "s3";
    }
} 
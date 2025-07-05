package com.reviewservice.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class S3StorageService implements StorageService {
    
    @Value("${AWS_ACCESS_KEY_ID}")
    private String accessKey;
    
    @Value("${AWS_SECRET_ACCESS_KEY}")
    private String secretKey;
    
    @Value("${AWS_ENDPOINT:https://gateway.storjshare.io}")
    private String endpoint;
    
    @Value("${AWS_BUCKET:review-data}")
    private String bucket;
    
    private S3Client s3Client;
    
    private S3Client getS3Client() {
        if (s3Client == null) {
            log.debug("Initializing S3 client for endpoint: {} and bucket: {}", endpoint, bucket);
            s3Client = S3Client.builder()
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)))
                    .endpointOverride(URI.create(endpoint))
                    .region(Region.US_EAST_1) // required but not used by Storj
                    .build();
            log.info("S3 client initialized successfully");
        }
        return s3Client;
    }
    
    @Override
    public List<String> listReviewFiles(String prefix) {
        try {
            log.debug("Listing S3 files in bucket: {} with prefix: {}", bucket, prefix);
            
            ListObjectsV2Response response = getS3Client().listObjectsV2(
                ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .prefix(prefix)
                    .build()
            );
            
            List<String> files = response.contents().stream()
                    .map(S3Object::key)
                    .collect(Collectors.toList());
            
            log.debug("Found {} files in S3 bucket with prefix: {}", files.size(), prefix);
            return files;
        } catch (Exception e) {
            log.error("Error listing S3 files: {}", e.getMessage(), e);
            return List.of();
        }
    }

    @Override
    public byte[] getFile(String key) {
        try {
            log.debug("Getting S3 file: {} from bucket: {}", key, bucket);
            
            byte[] content = getS3Client().getObjectAsBytes(
                GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build()
            ).asByteArray();
            
            log.debug("Successfully retrieved S3 file: {} ({} bytes)", key, content.length);
            return content;
        } catch (Exception e) {
            log.error("Error getting S3 file {}: {}", key, e.getMessage(), e);
            return new byte[0];
        }
    }

    @Override
    public void markFileProcessed(String key) {
        // TODO: Implement processed file tracking
        // Could use a separate bucket or database to track processed files
        log.info("Marking file as processed: {}", key);
    }

    @Override
    public boolean isFileProcessed(String key) {
        // TODO: Implement processed file check
        // Could check against a separate bucket or database
        log.debug("Checking if file is processed: {} (not implemented, returning false)", key);
        return false;
    }
} 
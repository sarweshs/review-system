package com.reviewservice.storage;

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
            s3Client = S3Client.builder()
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)))
                    .endpointOverride(URI.create(endpoint))
                    .region(Region.US_EAST_1) // required but not used by Storj
                    .build();
        }
        return s3Client;
    }
    
    @Override
    public List<String> listReviewFiles(String prefix) {
        try {
            ListObjectsV2Response response = getS3Client().listObjectsV2(
                ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .prefix(prefix)
                    .build()
            );
            
            return response.contents().stream()
                    .map(S3Object::key)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            // Log error and return empty list
            System.err.println("Error listing S3 files: " + e.getMessage());
            return List.of();
        }
    }

    @Override
    public byte[] getFile(String key) {
        try {
            return getS3Client().getObjectAsBytes(
                GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build()
            ).asByteArray();
        } catch (Exception e) {
            // Log error and return empty array
            System.err.println("Error getting S3 file " + key + ": " + e.getMessage());
            return new byte[0];
        }
    }

    @Override
    public void markFileProcessed(String key) {
        // TODO: Implement processed file tracking
        // Could use a separate bucket or database to track processed files
        System.out.println("Marking file as processed: " + key);
    }

    @Override
    public boolean isFileProcessed(String key) {
        // TODO: Implement processed file check
        // Could check against a separate bucket or database
        return false;
    }
} 
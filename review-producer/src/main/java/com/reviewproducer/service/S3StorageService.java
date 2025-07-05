package com.reviewproducer.service;

import com.reviewcore.model.AwsCredential;
import lombok.extern.slf4j.Slf4j;
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
    
    private S3Client s3Client;
    private String bucketName;
    private String endpoint;
    
    public S3StorageService() {
        // Default constructor for Spring
    }
    
    public void initialize(AwsCredential credential, String endpoint, String bucketName) {
        this.endpoint = endpoint;
        this.bucketName = bucketName;
        
        this.s3Client = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(credential.getAccessKeyId(), credential.getSecretAccessKey())))
                .endpointOverride(URI.create(endpoint))
                .region(Region.US_EAST_1) // required but not used by Storj
                .build();
        
        log.info("Initialized S3 client for endpoint: {} and bucket: {}", endpoint, bucketName);
    }
    
    @Override
    public List<String> listReviewFiles(String prefix) {
        try {
            ListObjectsV2Response response = s3Client.listObjectsV2(
                ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .build()
            );
            
            List<String> files = response.contents().stream()
                    .map(S3Object::key)
                    .filter(key -> key.endsWith(".jl"))
                    .collect(Collectors.toList());
            
            log.info("Found {} .jl files in prefix: {}", files.size(), prefix);
            return files;
            
        } catch (Exception e) {
            log.error("Error listing S3 files in prefix: {}", prefix, e);
            return List.of();
        }
    }

    @Override
    public byte[] getFile(String key) {
        try {
            byte[] content = s3Client.getObjectAsBytes(
                GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build()
            ).asByteArray();
            
            log.info("Downloaded file: {} ({} bytes)", key, content.length);
            return content;
            
        } catch (Exception e) {
            log.error("Error getting S3 file: {}", key, e);
            return new byte[0];
        }
    }

    @Override
    public boolean fileExists(String key) {
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
        } catch (Exception e) {
            log.error("Error checking if file exists: {}", key, e);
            return false;
        }
    }

    @Override
    public String getStorageType() {
        return "s3";
    }
} 
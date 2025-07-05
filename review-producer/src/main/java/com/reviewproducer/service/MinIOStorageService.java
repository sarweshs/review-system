package com.reviewproducer.service;

import com.reviewcore.model.BasicCredential;
import io.minio.MinioClient;
import io.minio.ListObjectsArgs;
import io.minio.GetObjectArgs;
import io.minio.Result;
import io.minio.errors.*;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class MinIOStorageService implements StorageService {
    
    private MinioClient minioClient;
    private String bucketName;
    private String endpoint;
    
    public MinIOStorageService() {
        // Default constructor for Spring
    }
    
    public void initialize(BasicCredential credential, String endpoint, String bucketName) {
        if (credential == null) {
            throw new IllegalArgumentException("Basic credential cannot be null");
        }
        
        if (endpoint == null || endpoint.trim().isEmpty()) {
            throw new IllegalArgumentException("Endpoint cannot be null or empty");
        }
        
        if (bucketName == null || bucketName.trim().isEmpty()) {
            throw new IllegalArgumentException("Bucket name cannot be null or empty");
        }
        
        if (credential.getUsername() == null || credential.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("MinIO username cannot be null or empty");
        }
        
        if (credential.getPassword() == null || credential.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("MinIO password cannot be null or empty");
        }
        
        this.endpoint = endpoint;
        this.bucketName = bucketName;
        
        try {
            this.minioClient = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(credential.getUsername(), credential.getPassword())
                    .build();
            
            log.info("Initialized MinIO client for endpoint: {} and bucket: {}", endpoint, bucketName);
            
            // Test the connection by checking if bucket exists
            testConnection();
            
        } catch (Exception e) {
            log.error("Failed to initialize MinIO client for endpoint: {} and bucket: {} - {}", 
                    endpoint, bucketName, e.getMessage(), e);
            throw new RuntimeException("Failed to initialize MinIO client", e);
        }
    }
    
    private void testConnection() {
        try {
            boolean bucketExists = minioClient.bucketExists(io.minio.BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build());
            
            if (!bucketExists) {
                log.error("MinIO bucket does not exist: {}", bucketName);
                throw new RuntimeException("MinIO bucket does not exist: " + bucketName);
            }
            
            log.info("Successfully connected to MinIO bucket: {}", bucketName);
            
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("AccessDenied")) {
                log.error("Access denied to MinIO bucket: {} - check credentials and permissions - {}", 
                        bucketName, e.getMessage());
                throw new RuntimeException("Access denied to MinIO bucket: " + bucketName, e);
            } else {
                log.error("MinIO error testing connection to bucket: {} - {}", bucketName, e.getMessage(), e);
                throw new RuntimeException("MinIO error testing connection to bucket: " + bucketName, e);
            }
        } catch (InsufficientDataException | InternalException | InvalidKeyException | 
                 InvalidResponseException | NoSuchAlgorithmException | ServerException | 
                 XmlParserException e) {
            log.error("MinIO SDK error testing connection to bucket: {} - {}", bucketName, e.getMessage(), e);
            throw new RuntimeException("MinIO SDK error testing connection to bucket: " + bucketName, e);
        } catch (Exception e) {
            log.error("Unexpected error testing connection to MinIO bucket: {} - {}", bucketName, e.getMessage(), e);
            throw new RuntimeException("Unexpected error testing connection to MinIO bucket: " + bucketName, e);
        }
    }
    
    @Override
    public List<String> listReviewFiles(String prefix) {
        if (minioClient == null) {
            log.error("MinIO client is not initialized");
            throw new IllegalStateException("MinIO client is not initialized");
        }
        
        if (prefix == null) {
            prefix = "";
        }
        
        try {
            log.debug("Listing MinIO objects in bucket: {} with prefix: {}", bucketName, prefix);
            
            Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .maxKeys(1000) // Limit results to avoid timeouts
                    .build()
            );
            
            List<String> files = new ArrayList<>();
            for (Result<Item> result : results) {
                Item item = result.get();
                if (item.objectName().endsWith(".jl")) {
                    files.add(item.objectName());
                }
            }
            
            log.info("Found {} .jl files in prefix: {}", files.size(), prefix);
            return files;
            
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("AccessDenied")) {
                log.error("Access denied listing MinIO objects in bucket: {} with prefix: {} - {}", 
                        bucketName, prefix, e.getMessage());
                throw new RuntimeException("Access denied listing MinIO objects in bucket: " + bucketName, e);
            } else if (e.errorResponse().code().equals("NoSuchBucket")) {
                log.error("MinIO bucket not found: {} - {}", bucketName, e.getMessage());
                throw new RuntimeException("MinIO bucket not found: " + bucketName, e);
            } else {
                log.error("MinIO error listing objects in bucket: {} with prefix: {} - {}", 
                        bucketName, prefix, e.getMessage(), e);
                throw new RuntimeException("MinIO error listing objects in bucket: " + bucketName, e);
            }
        } catch (InsufficientDataException | InternalException | InvalidKeyException | 
                 InvalidResponseException | NoSuchAlgorithmException | ServerException | 
                 XmlParserException e) {
            log.error("MinIO SDK error listing objects in bucket: {} with prefix: {} - {}", 
                    bucketName, prefix, e.getMessage(), e);
            throw new RuntimeException("MinIO SDK error listing objects in bucket: " + bucketName, e);
        } catch (Exception e) {
            log.error("Unexpected error listing MinIO objects in bucket: {} with prefix: {} - {}", 
                    bucketName, prefix, e.getMessage(), e);
            throw new RuntimeException("Unexpected error listing MinIO objects in bucket: " + bucketName, e);
        }
    }

    @Override
    public byte[] getFile(String key) {
        if (minioClient == null) {
            log.error("MinIO client is not initialized");
            throw new IllegalStateException("MinIO client is not initialized");
        }
        
        if (key == null || key.trim().isEmpty()) {
            log.error("MinIO object key cannot be null or empty");
            throw new IllegalArgumentException("MinIO object key cannot be null or empty");
        }
        
        try {
            log.debug("Downloading MinIO object: {} from bucket: {}", key, bucketName);
            
            InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(key)
                    .build()
            );
            
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[1024];
            while ((nRead = stream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            
            byte[] content = buffer.toByteArray();
            log.info("Downloaded file: {} ({} bytes) from bucket: {}", key, content.length, bucketName);
            return content;
            
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("NoSuchKey")) {
                log.error("MinIO object not found: {} in bucket: {} - {}", key, bucketName, e.getMessage());
                throw new RuntimeException("MinIO object not found: " + key + " in bucket: " + bucketName, e);
            } else if (e.errorResponse().code().equals("AccessDenied")) {
                log.error("Access denied downloading MinIO object: {} from bucket: {} - {}", 
                        key, bucketName, e.getMessage());
                throw new RuntimeException("Access denied downloading MinIO object: " + key + " from bucket: " + bucketName, e);
            } else {
                log.error("MinIO error downloading object: {} from bucket: {} - {}", 
                        key, bucketName, e.getMessage(), e);
                throw new RuntimeException("MinIO error downloading object: " + key + " from bucket: " + bucketName, e);
            }
        } catch (InsufficientDataException | InternalException | InvalidKeyException | 
                 InvalidResponseException | NoSuchAlgorithmException | ServerException | 
                 XmlParserException e) {
            log.error("MinIO SDK error downloading object: {} from bucket: {} - {}", 
                    key, bucketName, e.getMessage(), e);
            throw new RuntimeException("MinIO SDK error downloading object: " + key + " from bucket: " + bucketName, e);
        } catch (Exception e) {
            log.error("Unexpected error downloading MinIO object: {} from bucket: {} - {}", 
                    key, bucketName, e.getMessage(), e);
            throw new RuntimeException("Unexpected error downloading MinIO object: " + key + " from bucket: " + bucketName, e);
        }
    }

    @Override
    public boolean fileExists(String key) {
        if (minioClient == null) {
            log.error("MinIO client is not initialized");
            return false;
        }
        
        if (key == null || key.trim().isEmpty()) {
            log.error("MinIO object key cannot be null or empty");
            return false;
        }
        
        try {
            minioClient.statObject(
                io.minio.StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(key)
                    .build()
            );
            return true;
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("NoSuchKey")) {
                return false;
            } else if (e.errorResponse().code().equals("AccessDenied")) {
                log.error("Access denied checking existence of MinIO object: {} in bucket: {} - {}", 
                        key, bucketName, e.getMessage());
            } else {
                log.error("MinIO error checking existence of object: {} in bucket: {} - {}", 
                        key, bucketName, e.getMessage());
            }
            return false;
        } catch (InsufficientDataException | InternalException | InvalidKeyException | 
                 InvalidResponseException | NoSuchAlgorithmException | ServerException | 
                 XmlParserException e) {
            log.error("MinIO SDK error checking existence of object: {} in bucket: {} - {}", 
                    key, bucketName, e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("Unexpected error checking existence of MinIO object: {} in bucket: {} - {}", 
                    key, bucketName, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String getStorageType() {
        return "minio";
    }
} 
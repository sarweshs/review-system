package com.reviewproducer.service;

import com.reviewcore.model.BasicCredential;
import com.reviewproducer.model.FileMetadata;
import io.minio.MinioClient;
import io.minio.ListObjectsArgs;
import io.minio.GetObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.Result;
import io.minio.errors.*;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class MinIOStorageService implements StorageService {
    
    private MinioClient minioClient;
    private String bucketName;
    private String endpoint;
    
    public MinIOStorageService() {}
    
    public void initialize(String endpoint, String bucketName, BasicCredential credential) {
        this.endpoint = endpoint;
        this.bucketName = bucketName;
        
        try {
            log.info("Initializing MinIO client for endpoint: {} and bucket: {}", endpoint, bucketName);
            
            this.minioClient = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(credential.getUsername(), credential.getPassword())
                    .build();
            
            // Test connection
            boolean bucketExists = minioClient.bucketExists(
                    io.minio.BucketExistsArgs.builder()
                            .bucket(bucketName)
                            .build()
            );
            
            if (!bucketExists) {
                log.error("MinIO bucket does not exist: {}", bucketName);
                throw new RuntimeException("MinIO bucket does not exist: " + bucketName);
            }
            
            log.info("Successfully connected to MinIO bucket: {}", bucketName);
            
        } catch (MinioException | InvalidKeyException | NoSuchAlgorithmException | IOException e) {
            log.error("Failed to initialize MinIO client for endpoint: {} and bucket: {} - {}", 
                    endpoint, bucketName, e.getMessage(), e);
            throw new RuntimeException("Failed to initialize MinIO client", e);
        }
    }
    
    @Override
    public List<String> listReviewFiles(String prefix) {
        try {
            log.debug("Listing MinIO objects in bucket: {} with prefix: {}", bucketName, prefix);
            
            Iterable<Result<Item>> objects = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(prefix)
                            .recursive(false)
                            .build()
            );
            
            List<String> files = new ArrayList<>();
            for (Result<Item> result : objects) {
                Item item = result.get();
                if (item.objectName().endsWith(".jl")) {
                    files.add(item.objectName());
                }
            }
            
            log.info("Found {} .jl files in prefix: {}", files.size(), prefix);
            return files;
            
        } catch (MinioException | InvalidKeyException | NoSuchAlgorithmException | IOException e) {
            log.error("Error listing MinIO files in bucket: {} with prefix: {} - {}", 
                    bucketName, prefix, e.getMessage(), e);
            throw new RuntimeException("Failed to list MinIO files", e);
        }
    }
    
    @Override
    public List<String> listReviewFilesRecursive(String prefix) {
        try {
            log.debug("Listing MinIO objects recursively in bucket: {} with prefix: {}", bucketName, prefix);
            
            Iterable<Result<Item>> objects = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(prefix)
                            .recursive(true)
                            .build()
            );
            
            List<String> files = new ArrayList<>();
            for (Result<Item> result : objects) {
                Item item = result.get();
                if (item.objectName().endsWith(".jl")) {
                    files.add(item.objectName());
                }
            }
            
            log.info("Found {} .jl files recursively in prefix: {}", files.size(), prefix);
            
            // Log file names at DEBUG level
            for (String file : files) {
                log.debug("Found file: {}", file);
            }
            
            return files;
            
        } catch (MinioException | InvalidKeyException | NoSuchAlgorithmException | IOException e) {
            log.error("Error listing MinIO files recursively in bucket: {} with prefix: {} - {}", 
                    bucketName, prefix, e.getMessage(), e);
            throw new RuntimeException("Failed to list MinIO files recursively", e);
        }
    }
    
    @Override
    public List<FileMetadata> listReviewFilesWithMetadata(String prefix) {
        try {
            log.info("Listing MinIO objects with metadata in bucket: {} with prefix: {}", bucketName, prefix);
            
            Iterable<Result<Item>> objects = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(prefix)
                            .recursive(true)
                            .build()
            );
            
            // First, let's count and print ALL objects found to understand the structure
            List<Item> allItems = new ArrayList<>();
            for (Result<Item> result : objects) {
                allItems.add(result.get());
            }
            
            log.info("Total objects found in bucket {} with prefix {}: {}", bucketName, prefix, allItems.size());
            
            if (allItems.isEmpty()) {
                log.warn("No objects found in bucket {} with prefix {}", bucketName, prefix);
                return new ArrayList<>();
            }
            
            // Print all objects for debugging
            for (Item item : allItems) {
                log.info("Found object: {} (size: {} bytes, modified: {})", 
                        item.objectName(), item.size(), item.lastModified());
            }
            
            List<FileMetadata> files = new ArrayList<>();
            
            for (Item item : allItems) {
                if (item.objectName().endsWith(".jl")) {
                    String fileName = item.objectName().substring(item.objectName().lastIndexOf('/') + 1);
                    ZonedDateTime lastModified = item.lastModified();
                    
                    FileMetadata metadata = new FileMetadata(
                            fileName,
                            item.objectName(),
                            item.size(),
                            lastModified.toInstant(),
                            lastModified.toInstant(), // MinIO doesn't provide creation time, using lastModified
                            item.etag(),
                            "application/jsonl"
                    );
                    
                    files.add(metadata);
                    log.info("Found .jl file: {} (size: {} bytes, modified: {})", 
                            item.objectName(), item.size(), lastModified);
                }
            }
            
            log.info("Found {} .jl files with metadata in prefix: {}", files.size(), prefix);
            
            // Log subfolder structure
            Set<String> subfolders = new HashSet<>();
            for (Item item : allItems) {
                String key = item.objectName();
                int lastSlash = key.lastIndexOf('/');
                if (lastSlash > 0) {
                    subfolders.add(key.substring(0, lastSlash));
                }
            }
            
            log.info("Found {} subfolders:", subfolders.size());
            for (String subfolder : subfolders) {
                log.info("  - Subfolder: {}", subfolder);
            }
            
            return files;
            
        } catch (MinioException | InvalidKeyException | NoSuchAlgorithmException | IOException e) {
            log.error("Error listing MinIO files with metadata in bucket: {} with prefix: {} - {}", 
                    bucketName, prefix, e.getMessage(), e);
            throw new RuntimeException("Failed to list MinIO files with metadata", e);
        }
    }
    
    @Override
    public byte[] getFile(String key) {
        try {
            log.debug("Getting MinIO file: {} from bucket: {}", key, bucketName);
            
            InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(key)
                            .build()
            );
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = stream.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            
            byte[] content = baos.toByteArray();
            stream.close();
            baos.close();
            
            log.debug("Successfully retrieved MinIO file: {} ({} bytes)", key, content.length);
            return content;
            
        } catch (MinioException | InvalidKeyException | NoSuchAlgorithmException | IOException e) {
            log.error("Error getting MinIO file {} from bucket: {} - {}", key, bucketName, e.getMessage(), e);
            throw new RuntimeException("Failed to get MinIO file", e);
        } catch (Exception e) {
            log.error("Unexpected error getting MinIO file {} from bucket: {} - {}", key, bucketName, e.getMessage(), e);
            throw new RuntimeException("Failed to get MinIO file", e);
        }
    }
    
    @Override
    public FileMetadata getFileMetadata(String key) {
        try {
            log.debug("Getting metadata for MinIO file: {} from bucket: {}", key, bucketName);
            
            var stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(key)
                            .build()
            );
            
            String fileName = key.substring(key.lastIndexOf('/') + 1);
            ZonedDateTime lastModified = stat.lastModified();
            
            FileMetadata metadata = new FileMetadata(
                    fileName,
                    key,
                    stat.size(),
                    lastModified.toInstant(),
                    lastModified.toInstant(), // MinIO doesn't provide creation time
                    stat.etag(),
                    stat.contentType()
            );
            
            log.debug("Retrieved metadata for file: {} (size: {} bytes, modified: {})", 
                    key, stat.size(), lastModified);
            
            return metadata;
            
        } catch (MinioException | InvalidKeyException | NoSuchAlgorithmException | IOException e) {
            log.error("Error getting metadata for MinIO file {} from bucket: {} - {}", 
                    key, bucketName, e.getMessage(), e);
            throw new RuntimeException("Failed to get MinIO file metadata", e);
        }
    }
    
    @Override
    public boolean fileExists(String key) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(key)
                            .build()
            );
            return true;
            
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("NoSuchKey")) {
                return false;
            }
            log.error("Error checking if MinIO file exists: {} in bucket: {} - {}", 
                    key, bucketName, e.getMessage(), e);
            return false;
        } catch (MinioException | InvalidKeyException | NoSuchAlgorithmException | IOException e) {
            log.error("Error checking if MinIO file exists: {} in bucket: {} - {}", 
                    key, bucketName, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public String getStorageType() {
        return "minio";
    }
} 
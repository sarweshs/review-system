package com.reviewproducer.service;

import com.reviewcore.model.BasicCredential;
import io.minio.MinioClient;
import io.minio.ListObjectsArgs;
import io.minio.GetObjectArgs;
import io.minio.Result;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
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
        this.endpoint = endpoint;
        this.bucketName = bucketName;
        
        this.minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(credential.getUsername(), credential.getPassword())
                .build();
        
        log.info("Initialized MinIO client for endpoint: {} and bucket: {}", endpoint, bucketName);
    }
    
    @Override
    public List<String> listReviewFiles(String prefix) {
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
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
            
        } catch (Exception e) {
            log.error("Error listing MinIO files in prefix: {}", prefix, e);
            return List.of();
        }
    }

    @Override
    public byte[] getFile(String key) {
        try {
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
            log.info("Downloaded file: {} ({} bytes)", key, content.length);
            return content;
            
        } catch (Exception e) {
            log.error("Error getting MinIO file: {}", key, e);
            return new byte[0];
        }
    }

    @Override
    public boolean fileExists(String key) {
        try {
            minioClient.statObject(
                io.minio.StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(key)
                    .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getStorageType() {
        return "minio";
    }
} 
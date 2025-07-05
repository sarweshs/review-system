package com.reviewproducer.service;

import java.util.List;

public interface StorageService {
    /**
     * List all .jl files in the given prefix/path
     */
    List<String> listReviewFiles(String prefix);
    
    /**
     * Get file content as bytes
     */
    byte[] getFile(String key);
    
    /**
     * Check if file exists
     */
    boolean fileExists(String key);
    
    /**
     * Get the storage type (s3, minio, gcs)
     */
    String getStorageType();
} 
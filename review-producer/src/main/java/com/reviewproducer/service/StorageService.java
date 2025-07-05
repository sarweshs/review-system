package com.reviewproducer.service;

import com.reviewproducer.model.FileMetadata;
import java.util.List;

public interface StorageService {
    /**
     * List all .jl files in the given prefix/path (non-recursive)
     */
    List<String> listReviewFiles(String prefix);
    
    /**
     * List all .jl files recursively in the given prefix/path with metadata
     */
    List<FileMetadata> listReviewFilesWithMetadata(String prefix);
    
    /**
     * List all .jl files recursively in the given prefix/path
     */
    List<String> listReviewFilesRecursive(String prefix);
    
    /**
     * Get file content as bytes
     */
    byte[] getFile(String key);
    
    /**
     * Get file metadata
     */
    FileMetadata getFileMetadata(String key);
    
    /**
     * Check if file exists
     */
    boolean fileExists(String key);
    
    /**
     * Get the storage type (s3, minio, gcs)
     */
    String getStorageType();
} 
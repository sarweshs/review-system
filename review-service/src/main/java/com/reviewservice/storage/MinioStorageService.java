package com.reviewservice.storage;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class MinioStorageService implements StorageService {
    @Override
    public List<String> listReviewFiles(String prefix) {
        // TODO: Implement MinIO file listing
        return List.of();
    }

    @Override
    public byte[] getFile(String key) {
        // TODO: Implement MinIO file retrieval
        return new byte[0];
    }

    @Override
    public void markFileProcessed(String key) {
        // TODO: Implement processed file tracking
    }

    @Override
    public boolean isFileProcessed(String key) {
        // TODO: Implement processed file check
        return false;
    }
} 
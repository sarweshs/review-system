package com.reviewservice.storage;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class GCSStorageService implements StorageService {
    @Override
    public List<String> listReviewFiles(String prefix) {
        // TODO: Implement GCS file listing
        return List.of();
    }

    @Override
    public byte[] getFile(String key) {
        // TODO: Implement GCS file retrieval
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
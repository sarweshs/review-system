package com.reviewservice.storage;

import java.util.List;

public interface StorageService {
    List<String> listReviewFiles(String prefix);
    byte[] getFile(String key);
    void markFileProcessed(String key);
    boolean isFileProcessed(String key);
} 
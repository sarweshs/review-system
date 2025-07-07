package com.reviewproducer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reviewproducer.model.StorageEvent;
import com.reviewproducer.model.MinIOEvent;
import com.reviewproducer.model.FileMetadata;
import com.reviewcore.model.ReviewSource;
import com.reviewcore.model.Credential;
import com.reviewproducer.repository.ReviewSourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageEventService {
    
    private final ReviewKafkaProducerService kafkaProducerService;
    private final StorageServiceFactory storageServiceFactory;
    private final CredentialService credentialService;
    private final ReviewSourceRepository reviewSourceRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * Process a generic storage event
     */
    public void processStorageEvent(StorageEvent event) {
        log.info("Processing storage event: {}", event);
        
        try {
            // Validate the event
            if (!isValidEvent(event)) {
                log.warn("Invalid storage event received: {}", event);
                return;
            }
            
            // Check if this is a .jl file creation event
            if (event.isJsonlFile() && event.isFileCreated()) {
                processJsonlFileEvent(event);
            } else {
                log.debug("Skipping event - not a .jl file creation: {}", event);
            }
            
        } catch (Exception e) {
            log.error("Failed to process storage event: {} - {}", event, e.getMessage(), e);
            throw new RuntimeException("Failed to process storage event", e);
        }
    }
    
    /**
     * Process MinIO events and convert them to generic storage events
     */
    public void processMinIOEvent(MinIOEvent minioEvent) {
        log.info("Converting and processing MinIO event: {}", minioEvent.getEventName());
        
        if (minioEvent.getRecords() == null || minioEvent.getRecords().isEmpty()) {
            log.warn("No records found in MinIO event");
            return;
        }
        
        for (MinIOEvent.MinIORecord record : minioEvent.getRecords()) {
            try {
                StorageEvent storageEvent = convertMinIOEventToStorageEvent(record);
                processStorageEvent(storageEvent);
            } catch (Exception e) {
                log.error("Failed to convert/process MinIO record: {} - {}", 
                        getObjectKey(record), e.getMessage(), e);
            }
        }
    }
    
    /**
     * Convert MinIO event record to generic storage event
     */
    private StorageEvent convertMinIOEventToStorageEvent(MinIOEvent.MinIORecord record) {
        String objectKey = getObjectKey(record);
        String bucketName = getBucketName(record);
        String eventName = record.getEventName();
        
        // Parse event time
        Instant eventTime = parseEventTime(record.getEventTime());
        
        // Get object info
        Long size = record.getS3().getObject().getSize();
        String etag = record.getS3().getObject().getETag();
        
        // Create metadata map
        Map<String, String> metadata = new HashMap<>();
        metadata.put("eventSource", record.getEventSource());
        metadata.put("awsRegion", record.getAwsRegion());
        metadata.put("eventVersion", record.getEventVersion());
        
        // Convert to generic storage event
        return new StorageEvent(
                "minio",
                eventName,
                bucketName,
                objectKey,
                size,
                etag,
                eventTime,
                metadata,
                null // We could store the original record as JSON if needed
        );
    }
    
    /**
     * Validate storage event
     */
    private boolean isValidEvent(StorageEvent event) {
        return event != null && 
               event.getProvider() != null && 
               event.getBucket() != null && 
               event.getKey() != null && 
               event.getEventType() != null;
    }
    
    /**
     * Process a .jl file creation event
     */
    private void processJsonlFileEvent(StorageEvent event) {
        log.info("Processing .jl file creation event: {}", event.getFileName());
        
        try {
            // Create file metadata
            FileMetadata fileMetadata = createFileMetadataFromEvent(event);
            
            // Process the file content
            processFileContent(fileMetadata, event.getBucket());
            
        } catch (Exception e) {
            log.error("Failed to process .jl file event: {} - {}", 
                    event.getFileName(), e.getMessage(), e);
            throw new RuntimeException("Failed to process .jl file event", e);
        }
    }
    
    /**
     * Create FileMetadata from storage event
     */
    private FileMetadata createFileMetadataFromEvent(StorageEvent event) {
        return new FileMetadata(
                event.getFileName(),
                event.getKey(),
                event.getSize() != null ? event.getSize() : 0L,
                event.getTimestamp() != null ? event.getTimestamp() : Instant.now(),
                event.getTimestamp() != null ? event.getTimestamp() : Instant.now(),
                event.getEtag(),
                "application/jsonl"
        );
    }
    
    /**
     * Process the file content by downloading and processing the .jl file
     */
    private void processFileContent(FileMetadata fileMetadata, String bucketName) {
        log.info("Processing file content for: {} (size: {} bytes) from bucket: {}", 
                fileMetadata.getName(), fileMetadata.getSize(), bucketName);
        
        try {
            // Find the appropriate storage configuration for this bucket
            ReviewSource storageSource = findStorageSourceForBucket(bucketName);
            if (storageSource == null) {
                log.error("No storage configuration found for bucket: {}", bucketName);
                throw new RuntimeException("No storage configuration found for bucket: " + bucketName);
            }
            
            log.info("Found storage configuration for bucket {}: {}", bucketName, storageSource.getName());
            
            // Decrypt credentials
            Credential credentials = credentialService.decryptCredential(storageSource.getCredentialJson());
            if (credentials == null) {
                log.error("Failed to decrypt credentials for storage source: {}", storageSource.getName());
                throw new RuntimeException("Failed to decrypt credentials for storage source: " + storageSource.getName());
            }
            
            // Create storage service
            StorageService storageService = storageServiceFactory.createStorageService(storageSource.getUri(), credentials);
            if (storageService == null) {
                log.error("Failed to create storage service for source: {}", storageSource.getName());
                throw new RuntimeException("Failed to create storage service for source: " + storageSource.getName());
            }
            
            // Download the file content
            log.info("Downloading file: {} from bucket: {}", fileMetadata.getKey(), bucketName);
            String fileContent = storageService.downloadFile(fileMetadata.getKey());
            
            if (fileContent == null || fileContent.trim().isEmpty()) {
                log.warn("Empty file content received for: {}", fileMetadata.getName());
                return;
            }
            
            log.info("Successfully downloaded file: {} ({} bytes)", fileMetadata.getName(), fileContent.length());
            
            // Process the file content
            processJsonlContent(fileContent, storageSource.getName());
            
        } catch (Exception e) {
            log.error("Failed to process file content for: {} - {}", 
                    fileMetadata.getName(), e.getMessage(), e);
            throw new RuntimeException("Failed to process file content", e);
        }
    }
    
    /**
     * Find the appropriate storage source configuration for a given bucket
     */
    private ReviewSource findStorageSourceForBucket(String bucketName) {
        try {
            List<ReviewSource> activeSources = reviewSourceRepository.findAllActive();
            
            for (ReviewSource source : activeSources) {
                try {
                    // Extract bucket from URI
                    String sourceBucket = extractBucketFromUri(source.getUri());
                    if (bucketName.equals(sourceBucket)) {
                        return source;
                    }
                } catch (Exception e) {
                    log.warn("Failed to extract bucket from URI: {} - {}", source.getUri(), e.getMessage());
                }
            }
            
            log.warn("No storage source found for bucket: {}", bucketName);
            return null;
            
        } catch (Exception e) {
            log.error("Error finding storage source for bucket: {} - {}", bucketName, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Extract bucket name from storage URI
     */
    private String extractBucketFromUri(String uri) {
        try {
            java.net.URI parsedUri = new java.net.URI(uri);
            String path = parsedUri.getPath();
            
            // Remove leading slash and get the first part (bucket name)
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            
            // Split by '/' and take the first part as bucket name
            String[] parts = path.split("/");
            return parts.length > 0 ? parts[0] : "";
            
        } catch (Exception e) {
            log.error("Error extracting bucket from URI: {} - {}", uri, e.getMessage(), e);
            throw new RuntimeException("Error extracting bucket from URI", e);
        }
    }
    

    
    /**
     * Process a .jl file content line by line
     */
    private void processJsonlContent(String fileContent, String sourceName) {
        if (fileContent == null || fileContent.trim().isEmpty()) {
            log.warn("Empty file content received for source: {}", sourceName);
            return;
        }
        
        String[] lines = fileContent.split("\n");
        int totalLines = lines.length;
        int processedLines = 0;
        int errorLines = 0;
        
        log.info("Processing {} lines from file for source: {}", totalLines, sourceName);
        
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }
            
            try {
                kafkaProducerService.processReviewLine(line.trim());
                processedLines++;
            } catch (Exception e) {
                log.error("Failed to process line from source: {} - {}", sourceName, e.getMessage());
                errorLines++;
            }
        }
        
        log.info("File processing completed for source: {} - Processed: {}, Errors: {}", 
                sourceName, processedLines, errorLines);
    }
    
    // Helper methods for MinIO event conversion
    private String getObjectKey(MinIOEvent.MinIORecord record) {
        if (record.getS3() != null && record.getS3().getObject() != null) {
            return record.getS3().getObject().getKey();
        }
        return null;
    }
    
    private String getBucketName(MinIOEvent.MinIORecord record) {
        if (record.getS3() != null && record.getS3().getBucket() != null) {
            return record.getS3().getBucket().getName();
        }
        return null;
    }
    
    private Instant parseEventTime(String eventTimeStr) {
        try {
            return Instant.parse(eventTimeStr);
        } catch (Exception e) {
            log.warn("Failed to parse event time: {}, using current time", eventTimeStr);
            return Instant.now();
        }
    }
} 
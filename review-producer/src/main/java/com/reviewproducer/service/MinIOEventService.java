package com.reviewproducer.service;

import com.reviewproducer.model.MinIOEvent;
import com.reviewproducer.model.FileMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinIOEventService {
    
    private final ReviewKafkaProducerService kafkaProducerService;
    private final StorageServiceFactory storageServiceFactory;
    private final CredentialService credentialService;
    
    /**
     * Process MinIO events containing information about uploaded .jl files
     */
    public void processMinIOEvent(MinIOEvent event) {
        log.info("Processing MinIO event: {}", event.getEventName());
        
        if (event.getRecords() == null || event.getRecords().isEmpty()) {
            log.warn("No records found in MinIO event");
            return;
        }
        
        int processedFiles = 0;
        int errorFiles = 0;
        
        for (MinIOEvent.MinIORecord record : event.getRecords()) {
            try {
                if (shouldProcessRecord(record)) {
                    processRecord(record);
                    processedFiles++;
                } else {
                    log.debug("Skipping record for file: {} (not a .jl file or not a PUT event)", 
                            getObjectKey(record));
                }
            } catch (Exception e) {
                log.error("Failed to process MinIO record for file: {} - {}", 
                        getObjectKey(record), e.getMessage(), e);
                errorFiles++;
            }
        }
        
        log.info("MinIO event processing completed - Processed: {}, Errors: {}", processedFiles, errorFiles);
    }
    
    /**
     * Check if the record should be processed (only .jl files with PUT events)
     */
    private boolean shouldProcessRecord(MinIOEvent.MinIORecord record) {
        String objectKey = getObjectKey(record);
        String eventName = record.getEventName();
        
        // Only process PUT events for .jl files
        return objectKey != null && 
               objectKey.endsWith(".jl") && 
               "ObjectCreated:Put".equals(eventName);
    }
    
    /**
     * Get the object key from the record
     */
    private String getObjectKey(MinIOEvent.MinIORecord record) {
        if (record.getS3() != null && record.getS3().getObject() != null) {
            return record.getS3().getObject().getKey();
        }
        return null;
    }
    
    /**
     * Process a single MinIO record
     */
    private void processRecord(MinIOEvent.MinIORecord record) {
        String objectKey = getObjectKey(record);
        String bucketName = getBucketName(record);
        
        log.info("Processing MinIO record for file: {} in bucket: {}", objectKey, bucketName);
        
        // Create file metadata from the record
        FileMetadata fileMetadata = createFileMetadataFromRecord(record);
        
        // Process the file content
        processFileContent(fileMetadata, bucketName);
    }
    
    /**
     * Get the bucket name from the record
     */
    private String getBucketName(MinIOEvent.MinIORecord record) {
        if (record.getS3() != null && record.getS3().getBucket() != null) {
            return record.getS3().getBucket().getName();
        }
        return null;
    }
    
    /**
     * Create FileMetadata from MinIO record
     */
    private FileMetadata createFileMetadataFromRecord(MinIOEvent.MinIORecord record) {
        String objectKey = getObjectKey(record);
        String fileName = objectKey.substring(objectKey.lastIndexOf('/') + 1);
        Long size = record.getS3().getObject().getSize();
        String eTag = record.getS3().getObject().getETag();
        
        // Parse event time
        Instant eventTime = parseEventTime(record.getEventTime());
        
        return new FileMetadata(
                fileName,
                objectKey,
                size != null ? size : 0L,
                eventTime,
                eventTime, // Using event time as both created and modified
                eTag,
                "application/jsonl"
        );
    }
    
    /**
     * Parse event time string to Instant
     */
    private Instant parseEventTime(String eventTimeStr) {
        try {
            // Parse ISO 8601 format: 2023-12-01T10:30:00.000Z
            return Instant.parse(eventTimeStr);
        } catch (Exception e) {
            log.warn("Failed to parse event time: {}, using current time", eventTimeStr);
            return Instant.now();
        }
    }
    
    /**
     * Process the file content by downloading and processing the .jl file
     */
    private void processFileContent(FileMetadata fileMetadata, String bucketName) {
        log.info("Processing file content for: {} (size: {} bytes) from bucket: {}", 
                fileMetadata.getName(), fileMetadata.getSize(), bucketName);
        
        try {
            // For now, we'll log the event and simulate processing
            // In a production environment, you would:
            // 1. Get the appropriate storage service configuration
            // 2. Download the file content using the storage service
            // 3. Process the .jl file line by line
            
            log.info("Received MinIO event for file: {} in bucket: {}", 
                    fileMetadata.getName(), bucketName);
            
            // TODO: Implement actual file download and processing
            // This would involve:
            // - Getting the storage service configuration for the bucket
            // - Downloading the file content using storageService.downloadFile(fileMetadata.getKey())
            // - Processing the content using processJsonlContent(fileContent, bucketName)
            
            // For demonstration purposes, we'll create a mock file content
            String mockContent = createMockJsonlContent(fileMetadata.getName());
            processJsonlContent(mockContent, bucketName);
            
        } catch (Exception e) {
            log.error("Failed to process file content for: {} - {}", 
                    fileMetadata.getName(), e.getMessage(), e);
            throw new RuntimeException("Failed to process file content", e);
        }
    }
    
    /**
     * Create mock JSONL content for demonstration purposes
     */
    private String createMockJsonlContent(String fileName) {
        // This is just for demonstration - in real implementation, you'd download the actual file
        return String.format(
            "{\"reviewId\": 1001, \"providerId\": 101, \"platform\": \"google\", \"rating\": 5, \"comment\": \"Great service from %s\"}\n" +
            "{\"reviewId\": 1002, \"providerId\": 102, \"platform\": \"yelp\", \"rating\": 4, \"comment\": \"Good experience from %s\"}\n" +
            "{\"reviewId\": 1003, \"providerId\": 103, \"platform\": \"facebook\", \"rating\": 3, \"comment\": \"Average service from %s\"}",
            fileName, fileName, fileName
        );
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
} 
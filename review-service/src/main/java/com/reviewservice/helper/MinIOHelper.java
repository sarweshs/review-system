package com.reviewservice.helper;

import io.minio.MinioClient;
import io.minio.ListObjectsArgs;
import io.minio.GetObjectArgs;
import io.minio.Result;
import io.minio.messages.Item;
import io.minio.errors.MinioException;
import lombok.extern.slf4j.Slf4j;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Slf4j
public class MinIOHelper {
    private final MinioClient minioClient;
    private final String bucketName;

    public MinIOHelper(String endpoint, String accessKey, String secretKey, String bucketName) {
        this.minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        this.bucketName = bucketName;
        log.info("MinIO Helper initialized for bucket: {} at endpoint: {}", bucketName, endpoint);
    }
    
    public void processFilesIncrementally(Date lastProcessedTime) throws Exception {
        try {
            log.info("Listing objects in bucket: {}", bucketName);
            
            // List all objects in the bucket
            Iterable<Result<Item>> objects = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .recursive(true)
                            .build()
            );

            int totalFiles = 0;
            int processedFiles = 0;
            int skippedFiles = 0;

            for (Result<Item> result : objects) {
                totalFiles++;
                Item item = result.get();
                ZonedDateTime objectModifiedTime = item.lastModified();

                log.debug("Found file: {} (modified: {})", item.objectName(), objectModifiedTime);

                // Skip if this file was modified before our last processed time
                if (lastProcessedTime != null &&
                        objectModifiedTime.toInstant().isBefore(lastProcessedTime.toInstant())) {
                    log.debug("Skipping file (older than last processed time): {}", item.objectName());
                    skippedFiles++;
                    continue;
                }

                // Only process .jl files
                if (item.objectName().endsWith(".jl")) {
                    log.info("Processing .jl file: {}", item.objectName());
                    processJLFile(item.objectName(), lastProcessedTime);
                    processedFiles++;
                } else {
                    log.debug("Skipping non-.jl file: {}", item.objectName());
                    skippedFiles++;
                }
            }
            
            log.info("Processing complete - Total files: {}, Processed: {}, Skipped: {}", 
                    totalFiles, processedFiles, skippedFiles);
            
        } catch (MinioException e) {
            log.error("MinIO Error occurred: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error occurred: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void processJLFile(String objectName, Date lastProcessedTime) throws Exception {
        log.info("Reading file content from: {}", objectName);
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        minioClient.getObject(
                                GetObjectArgs.builder()
                                        .bucket(bucketName)
                                        .object(objectName)
                                        .build()
                        )
                )
        )) {

            String line;
            int lineCount = 0;
            while ((line = reader.readLine()) != null) {
                lineCount++;
                
                // Parse your JSON line here
                // Example: JsonNode node = objectMapper.readTree(line);

                // Extract timestamp from your JSON data
                // Date recordTime = ... parse from your JSON

                // Skip if this record is older than our last processed time
                // if (lastProcessedTime != null && recordTime.before(lastProcessedTime)) {
                //     continue;
                // }

                // Process your record here
                log.debug("Line {}: {}", lineCount, line);
            }
            
            log.info("Finished processing file: {} (processed {} lines)", objectName, lineCount);
        } catch (Exception e) {
            log.error("Error processing file {}: {}", objectName, e.getMessage(), e);
            throw e;
        }
    }
}
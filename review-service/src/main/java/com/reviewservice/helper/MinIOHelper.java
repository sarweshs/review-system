package com.reviewservice.helper;

import io.minio.MinioClient;
import io.minio.ListObjectsArgs;
import io.minio.GetObjectArgs;
import io.minio.Result;
import io.minio.messages.Item;
import io.minio.errors.MinioException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class MinIOHelper {
    private final MinioClient minioClient;
    private final String bucketName;

    public MinIOHelper(String endpoint, String accessKey, String secretKey, String bucketName) {
        this.minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        this.bucketName = bucketName;
    }
    public void processFilesIncrementally(Date lastProcessedTime) throws Exception {
        try {
            System.out.println("Listing objects in bucket: " + bucketName);
            
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

                System.out.println("Found file: " + item.objectName() + " (modified: " + objectModifiedTime + ")");

                // Skip if this file was modified before our last processed time
                if (lastProcessedTime != null &&
                        objectModifiedTime.toInstant().isBefore(lastProcessedTime.toInstant())) {
                    System.out.println("Skipping file (older than last processed time): " + item.objectName());
                    skippedFiles++;
                    continue;
                }

                // Only process .jl files
                if (item.objectName().endsWith(".jl")) {
                    System.out.println("Processing .jl file: " + item.objectName());
                    processJLFile(item.objectName(), lastProcessedTime);
                    processedFiles++;
                } else {
                    System.out.println("Skipping non-.jl file: " + item.objectName());
                    skippedFiles++;
                }
            }
            
            System.out.println("Processing complete:");
            System.out.println("  Total files found: " + totalFiles);
            System.out.println("  Files processed: " + processedFiles);
            System.out.println("  Files skipped: " + skippedFiles);
            
        } catch (MinioException e) {
            System.err.println("MinIO Error occurred: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("Unexpected error occurred: " + e.getMessage());
            throw e;
        }
    }

    private void processJLFile(String objectName, Date lastProcessedTime) throws Exception {
        System.out.println("Reading file content from: " + objectName);
        
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
                System.out.println("Line " + lineCount + ": " + line);
            }
            
            System.out.println("Finished processing file: " + objectName + " (processed " + lineCount + " lines)");
        } catch (Exception e) {
            System.err.println("Error processing file " + objectName + ": " + e.getMessage());
            throw e;
        }
    }
}
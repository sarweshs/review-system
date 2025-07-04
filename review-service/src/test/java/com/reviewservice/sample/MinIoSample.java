package com.reviewservice.sample;

import com.reviewservice.helper.MinIOHelper;
import io.minio.MinioClient;
import io.minio.ListBucketsArgs;
import io.minio.errors.MinioException;

import java.util.Date;

public class MinIoSample {
    public static void main(String[] args) {
        System.out.println("Starting MinIO Sample...");
        
        try {
            // First, test the connection and check if bucket exists
            testMinIOConnection();
            
            // Initialize MinIO client
            MinIOHelper minioHelper = new MinIOHelper(
                    "http://localhost:9000",
                    "minioadmin",
                    "minioadmin",
                    "review-data"
            );

            System.out.println("MinIO Helper initialized successfully");

            // Get your last processed timestamp from your state store
            Date lastProcessedTime = getLastProcessedTimeFromState();
            System.out.println("Last processed time: " + lastProcessedTime);

            System.out.println("Starting to process files incrementally...");
            minioHelper.processFilesIncrementally(lastProcessedTime);
            System.out.println("Finished processing files");

            // Update your state with the new timestamp
            updateLastProcessedTime(new Date());
            System.out.println("Updated last processed time");
            
        } catch (Exception e) {
            System.err.println("Error in MinIoSample: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("MinIO Sample completed.");
    }

    private static void testMinIOConnection() throws Exception {
        System.out.println("Testing MinIO connection...");
        
        MinioClient minioClient = MinioClient.builder()
                .endpoint("http://localhost:9000")
                .credentials("minioadmin", "minioadmin")
                .build();
        
        try {
            // List buckets to test connection
            var buckets = minioClient.listBuckets();
            System.out.println("Successfully connected to MinIO");
            System.out.println("Available buckets:");
            buckets.forEach(bucket -> System.out.println("  - " + bucket.name()));
            
            // Check if our target bucket exists
            boolean bucketExists = buckets.stream()
                    .anyMatch(bucket -> bucket.name().equals("review-data"));
            
            if (!bucketExists) {
                System.out.println("Warning: 'review-data' bucket does not exist!");
                System.out.println("Creating 'review-data' bucket...");
                minioClient.makeBucket(io.minio.MakeBucketArgs.builder().bucket("review-data").build());
                System.out.println("'review-data' bucket created successfully");
            } else {
                System.out.println("'review-data' bucket exists");
            }
            
        } catch (MinioException e) {
            System.err.println("Failed to connect to MinIO: " + e.getMessage());
            throw e;
        }
    }

    private static Date getLastProcessedTimeFromState() {
        // Implement your state retrieval logic here
        // Return null if no state exists (will process all files)
        return null;
    }

    private static void updateLastProcessedTime(Date newTime) {
        // Implement your state update logic here
    }
}
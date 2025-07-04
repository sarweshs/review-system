package com.reviewservice.sample;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.net.URI;

public class StorjS3Example {
    public static void main(String[] args) {
        String accessKey = System.getenv("AWS_ACCESS_KEY_ID");
        String secretKey = System.getenv("AWS_SECRET_ACCESS_KEY");
        String endpoint = System.getenv().getOrDefault("AWS_ENDPOINT", "https://gateway.storjshare.io");
        String bucket = System.getenv().getOrDefault("AWS_BUCKET", "review-data");
        String objectKey = "test.txt";
        String content = "Hello Storj via S3!";

        if (accessKey == null || secretKey == null) {
            System.err.println("Missing AWS_ACCESS_KEY_ID or AWS_SECRET_ACCESS_KEY environment variables.");
            System.exit(1);
        }

        // Create S3 Client
        S3Client s3 = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .endpointOverride(URI.create(endpoint))
                .region(Region.US_EAST_1) // required but not used by Storj
                .build();

        // Upload
        s3.putObject(PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(objectKey)
                        .build(),
                software.amazon.awssdk.core.sync.RequestBody.fromString(content));

        // List
        ListObjectsV2Response list = s3.listObjectsV2(ListObjectsV2Request.builder().bucket(bucket).build());
        list.contents().forEach(obj -> System.out.println("Found object: " + obj.key()));

        // Download
        String downloaded = s3.getObjectAsBytes(GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(objectKey)
                        .build())
                .asUtf8String();

        System.out.println("Downloaded content: " + downloaded);
    }
}

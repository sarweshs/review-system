# Storage Events API

This document describes the storage event endpoints that allow MinIO and other storage providers to send events to the Review Producer when new `.jl` files are uploaded.

## Overview

The Review Producer now supports receiving events from storage providers (like MinIO) when new `.jl` files are uploaded. This enables real-time processing of review data without the need for polling.

## Endpoints

### 1. MinIO Events Endpoint

**URL:** `POST /api/producer/storage/event/minio`

**Description:** Handles MinIO-specific events in the S3-compatible format.

**Request Body:** MinIO event in S3-compatible format

**Example Request:**
```json
{
  "EventName": "s3:ObjectCreated:Put",
  "Key": "reviews/2024/01/reviews_20240101.jl",
  "Records": [
    {
      "eventVersion": "2.1",
      "eventSource": "minio:s3",
      "awsRegion": "us-east-1",
      "eventTime": "2024-01-01T10:30:00.000Z",
      "eventName": "ObjectCreated:Put",
      "s3": {
        "s3SchemaVersion": "1.0",
        "configurationId": "minio-event-config",
        "bucket": {
          "name": "review-data",
          "ownerIdentity": {
            "principalId": "minio"
          },
          "arn": "arn:aws:s3:::review-data"
        },
        "object": {
          "key": "reviews/2024/01/reviews_20240101.jl",
          "size": 1024,
          "eTag": "\"abc123\"",
          "sequencer": "0A1B2C3D4E5F678901"
        }
      }
    }
  ]
}
```

**Response:**
```json
{
  "status": "MinIO event processed successfully"
}
```

### 2. Generic Storage Events Endpoint

**URL:** `POST /api/producer/storage/event`

**Description:** Handles generic storage events from any storage provider.

**Request Body:** Generic storage event format

**Example Request:**
```json
{
  "provider": "minio",
  "eventType": "ObjectCreated:Put",
  "bucket": "review-data",
  "key": "reviews/2024/01/reviews_20240101.jl",
  "size": 1024,
  "etag": "\"abc123\"",
  "timestamp": "2024-01-01T10:30:00.000Z",
  "metadata": {
    "eventSource": "minio:s3",
    "awsRegion": "us-east-1",
    "eventVersion": "2.1"
  }
}
```

**Response:**
```json
{
  "status": "Storage event processed successfully"
}
```

### 3. Raw Storage Events Endpoint

**URL:** `POST /api/producer/storage/event/raw`

**Description:** Handles raw storage event payloads for manual processing.

**Request Body:** Raw JSON string

**Example Request:**
```json
"{\"provider\":\"minio\",\"eventType\":\"ObjectCreated:Put\",\"bucket\":\"review-data\",\"key\":\"reviews/2024/01/reviews_20240101.jl\"}"
```

**Response:**
```json
{
  "status": "Raw storage event received"
}
```

## MinIO Configuration

To configure MinIO to send events to the Review Producer:

### 1. Create Event Notification Configuration

```bash
# Create a webhook endpoint configuration
mc admin config set myminio notify_webhook:review-producer endpoint="http://review-producer:8080/api/producer/storage/event/minio" enable="on"

# Apply the configuration
mc admin service restart myminio
```

### 2. Set Bucket Notification

```bash
# Create notification configuration for .jl files
mc event add myminio/review-data arn:minio:sqs::1:review-producer --suffix .jl --events put

# Or using the webhook directly
mc event add myminio/review-data arn:minio:webhook::1:review-producer --suffix .jl --events put
```

### 3. Alternative: Using mc command for webhook

```bash
# Add webhook notification for PUT events on .jl files
mc event add myminio/review-data arn:minio:webhook::1:review-producer --suffix .jl --events put
```

## Event Processing Flow

1. **Event Reception**: MinIO sends an event when a `.jl` file is uploaded
2. **Event Validation**: The service validates the event format and checks if it's a `.jl` file creation
3. **File Processing**: If valid, the service processes the file content line by line
4. **Kafka Production**: Each line (JSON review) is sent to Kafka for further processing
5. **Error Handling**: Invalid records are sent to DLQ or bad review topics

## Supported File Types

- **`.jl` files**: JSON Lines format containing review data
- **Event Types**: `ObjectCreated:Put`, `ObjectCreated:Post`

## Error Handling

- **Invalid Events**: Logged and ignored
- **Processing Errors**: Logged with details
- **File Download Errors**: Logged and skipped
- **Kafka Errors**: Handled by existing error handling mechanisms

## Monitoring

The following metrics are available for monitoring:

- Event processing success/failure rates
- File processing statistics
- Processing time metrics
- Error counts by type

## Security Considerations

1. **Authentication**: API key authentication is required for all storage event endpoints
2. **HTTPS**: Use HTTPS in production environments
3. **Rate Limiting**: Implement rate limiting to prevent abuse
4. **Validation**: All events are validated before processing

## Authentication

All storage event endpoints require API key authentication. The API key should be included in the request header:

```
X-API-Key: your-api-key-here
```

### Configuration

Set the API key in your application properties:

```properties
# Webhook API key for authentication
webhook.api.key=your-secure-api-key-here

# Custom header name (optional, defaults to X-API-Key)
webhook.api.key.header=X-API-Key
```

### MinIO Configuration with Authentication

When configuring MinIO to send events, include the API key in the webhook configuration:

```bash
# Configure webhook with authentication
mc admin config set myminio notify_webhook:review-producer \
  endpoint="http://review-producer:8080/api/producer/storage/event/minio" \
  enable="on" \
  header="X-API-Key:your-api-key-here"

# Apply the configuration
mc admin service restart myminio
```

## Example MinIO Event Payload

Here's a complete example of what MinIO sends when a `.jl` file is uploaded:

```json
{
  "EventName": "s3:ObjectCreated:Put",
  "Key": "reviews/2024/01/reviews_20240101.jl",
  "Records": [
    {
      "eventVersion": "2.1",
      "eventSource": "minio:s3",
      "awsRegion": "us-east-1",
      "eventTime": "2024-01-01T10:30:00.000Z",
      "eventName": "ObjectCreated:Put",
      "userIdentity": {
        "principalId": "minio"
      },
      "requestParameters": {
        "sourceIPAddress": "127.0.0.1"
      },
      "responseElements": {
        "x-amz-request-id": "ABC123",
        "x-minio-origin-endpoint": "http://localhost:9000"
      },
      "s3": {
        "s3SchemaVersion": "1.0",
        "configurationId": "minio-event-config",
        "bucket": {
          "name": "review-data",
          "ownerIdentity": {
            "principalId": "minio"
          },
          "arn": "arn:aws:s3:::review-data"
        },
        "object": {
          "key": "reviews/2024/01/reviews_20240101.jl",
          "size": 1024,
          "eTag": "\"abc123\"",
          "sequencer": "0A1B2C3D4E5F678901"
        }
      }
    }
  ]
}
```

## Testing

You can test the endpoints using curl:

```bash
# Test MinIO event endpoint with authentication
curl -X POST http://localhost:8080/api/producer/storage/event/minio \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your-api-key-here" \
  -d @minio-event-example.json

# Test generic storage event endpoint with authentication
curl -X POST http://localhost:8080/api/producer/storage/event \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your-api-key-here" \
  -d @storage-event-example.json

# Test authentication failure
curl -X POST http://localhost:8080/api/producer/storage/event/minio \
  -H "Content-Type: application/json" \
  -H "X-API-Key: invalid-key" \
  -d @minio-event-example.json

# Test missing API key
curl -X POST http://localhost:8080/api/producer/storage/event/minio \
  -H "Content-Type: application/json" \
  -d @minio-event-example.json
```

### Using the Test Script

Run the provided test script to verify all endpoints:

```bash
# Make the script executable
chmod +x test-storage-events.sh

# Run the tests
./test-storage-events.sh
``` 
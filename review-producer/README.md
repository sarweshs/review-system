# Review Producer Module

## Overview

The Review Producer module is a critical component of the Review System that fetches configured review sources from the database, downloads JSONL files, validates the entries, and processes them through Kafka for further downstream processing.

## Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Database      │    │   Storage       │    │   Review        │    │   Kafka         │
│   (Sources)     │───▶│   (S3/MinIO)    │───▶│   Producer      │───▶│   (Topics)      │
└─────────────────┘    └─────────────────┘    └─────────────────┘    └─────────────────┘
                              │                        │
                              ▼                        ▼
                       ┌─────────────────┐    ┌─────────────────┐
                       │   Bad Records   │    │   Error Logs    │
                       │   Database      │    │   (Console)     │
                       └─────────────────┘    └─────────────────┘
```

## Features

### 1. **Source Management**
- Fetches active review sources from the database
- Supports multiple storage platforms (AWS S3, MinIO, GCS)
- Handles encrypted credentials securely via Vault

### 2. **File Processing**
- Downloads `.jl` (JSONL) files from configured sources
- Processes files one by one to ensure reliability
- Supports large file handling with streaming

### 3. **Data Validation**
- Validates each JSON entry against predefined schema
- Checks for required fields and data types
- Ensures data integrity before processing

### 4. **Error Handling**
- Logs validation errors with detailed information
- Stores invalid records in `bad_review_records` table
- Continues processing despite individual record failures

### 5. **Kafka Integration**
- Sends valid records to Kafka topics for downstream processing
- Implements retry mechanisms for failed Kafka operations
- Supports multiple Kafka topics based on record type

## Database Schema

### Bad Review Records Table

```sql
CREATE TABLE bad_review_records (
    id BIGSERIAL PRIMARY KEY,
    json_data JSONB NOT NULL,
    platform VARCHAR(100) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**Fields:**
- `id`: Auto-incrementing primary key
- `json_data`: JSONB column to store the invalid record
- `platform`: Source platform (e.g., "aws_s3", "minio", "gcs")
- `reason`: Validation error reason
- `created_at`: Timestamp when the record was created

## Configuration

### Application Properties

```yaml
# Database Configuration
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/review_system
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect

# Kafka Configuration
kafka:
  bootstrap-servers: localhost:9092
  producer:
    key-serializer: org.apache.kafka.common.serialization.StringSerializer
    value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    topic:
      reviews: reviews

# Vault Configuration
vault:
  host: localhost
  port: 8200
  token: ${VAULT_TOKEN}
  scheme: http

# Processing Configuration
review:
  producer:
    batch-size: 100
    retry-attempts: 3
    retry-delay: 1000
    max-file-size: 100MB
```

## Processing Flow

### 1. **Source Discovery**
```java
// Fetch active sources from database
List<ReviewSource> activeSources = reviewSourceRepository.findByActiveTrue();
```

### 2. **File Download**
```java
// Download .jl files from storage
for (ReviewSource source : activeSources) {
    List<String> files = storageService.listFiles(source, "*.jl");
    for (String file : files) {
        processFile(source, file);
    }
}
```

### 3. **Record Validation**
```java
// Validate each JSON record
for (String line : fileLines) {
    try {
        ReviewRecord record = validateRecord(line);
        kafkaProducer.send(record);
    } catch (ValidationException e) {
        badRecordService.saveBadRecord(line, source.getPlatform(), e.getMessage());
    }
}
```

### 4. **Error Handling**
```java
// Save bad records to database
public void saveBadRecord(String jsonData, String platform, String reason) {
    BadReviewRecord badRecord = new BadReviewRecord();
    badRecord.setJsonData(jsonData);
    badRecord.setPlatform(platform);
    badRecord.setReason(reason);
    badReviewRecordRepository.save(badRecord);
}
```

## API Endpoints

### Health Check
```
GET /actuator/health
```

### Processing Status
```
GET /api/processing/status
```

### Bad Records
```
GET /api/bad-records
GET /api/bad-records/{id}
DELETE /api/bad-records/{id}
```

## Monitoring

### Metrics
- Files processed per minute
- Records validated per second
- Bad records count
- Kafka message send rate
- Processing latency

### Logs
- File download status
- Validation errors with details
- Kafka send failures
- Database operation errors

## Error Scenarios

### 1. **Invalid JSON Format**
```json
{
  "json_data": "invalid json string",
  "platform": "aws_s3",
  "reason": "Invalid JSON format: Unexpected token"
}
```

### 2. **Missing Required Fields**
```json
{
  "json_data": {"title": "Good product"},
  "platform": "minio",
  "reason": "Missing required field: rating"
}
```

### 3. **Invalid Data Types**
```json
{
  "json_data": {"rating": "not_a_number"},
  "platform": "gcs",
  "reason": "Invalid data type: rating must be numeric"
}
```

## Development

### Prerequisites
- Java 17+
- Maven 3.6+
- PostgreSQL 12+
- Kafka 2.8+
- Vault (for credential management)

### Building
```bash
mvn clean compile
```

### Running
```bash
mvn spring-boot:run
```

### Testing
```bash
mvn test
```

## Deployment

### Docker
```dockerfile
FROM openjdk:17-jre-slim
COPY target/review-producer-*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### Kubernetes
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: review-producer
spec:
  replicas: 2
  selector:
    matchLabels:
      app: review-producer
  template:
    metadata:
      labels:
        app: review-producer
    spec:
      containers:
      - name: review-producer
        image: review-producer:latest
        env:
        - name: DB_USERNAME
          valueFrom:
            secretKeyRef:
              name: db-secret
              key: username
```

## Troubleshooting

### Common Issues

1. **Database Connection Failed**
   - Check database credentials
   - Verify network connectivity
   - Ensure database is running

2. **Kafka Connection Failed**
   - Verify Kafka bootstrap servers
   - Check topic existence
   - Validate producer configuration

3. **Vault Authentication Failed**
   - Verify Vault token
   - Check Vault server connectivity
   - Ensure proper permissions

4. **File Download Failed**
   - Validate storage credentials
   - Check file permissions
   - Verify network connectivity to storage

### Log Analysis
```bash
# View application logs
tail -f logs/review-producer.log

# Search for errors
grep "ERROR" logs/review-producer.log

# Monitor bad records
grep "Bad record saved" logs/review-producer.log
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details. 
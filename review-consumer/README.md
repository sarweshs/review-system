# Review Consumer

A Spring Boot application that consumes review records from Kafka topics and processes them.

## Overview

The Review Consumer module is responsible for:
- Consuming valid review records from the `good_review_records` Kafka topic
- Consuming bad review records from the `bad_review_records` Kafka topic
- Processing and handling the consumed records
- Providing metrics and health endpoints
- Manual acknowledgment of Kafka messages

## Features

- **Kafka Consumer**: Consumes messages from multiple topics with manual acknowledgment
- **Concurrent Processing**: Uses multiple consumer threads for parallel processing
- **Metrics Collection**: Tracks processing statistics, error rates, and performance metrics
- **Health Monitoring**: Provides health and metrics endpoints
- **Error Handling**: Graceful error handling with logging and metrics tracking
- **Logging**: Comprehensive logging with file rotation

## Configuration

### Application Properties

The application is configured via `application.yml`:

```yaml
server:
  port: 7073

spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: review-consumer-group
      auto-offset-reset: earliest
    listener:
      ack-mode: manual
      concurrency: 3
      poll-timeout: 3000

kafka:
  topics:
    reviews: reviews
    bad-reviews: bad_review_records
```

### Kafka Topics

- `reviews`: Valid review records from the producer
- `bad_review_records`: Invalid review records that failed validation

## API Endpoints

### Health Check
```
GET /api/v1/health
```

### Metrics
```
GET /api/v1/metrics
```

Returns processing statistics including:
- Processed reviews count
- Bad reviews count
- Error count
- Processing rates
- Average processing time
- Uptime

## Running the Application

### Prerequisites
- Java 17+
- Maven 3.6+
- Kafka running on localhost:9092
- Kafka topics created

### Build and Run

```bash
# Build the project
mvn clean install

# Run the consumer
cd review-consumer
mvn spring-boot:run
```

### Docker (if needed)

```bash
# Build Docker image
docker build -t review-consumer .

# Run container
docker run -p 7073:7073 review-consumer
```

## Consumer Behavior

### Review Processing
1. Consumes `Review` objects from the `good_review_records` topic
2. Processes each review (placeholder for business logic)
3. Updates metrics
4. Manually acknowledges the message

### Bad Review Processing
1. Consumes JSON strings from the `bad_review_records` topic
2. Processes bad review records (placeholder for business logic)
3. Updates metrics
4. Manually acknowledges the message

### Error Handling
- Errors are logged with full context
- Error counts are tracked in metrics
- Messages are acknowledged to prevent infinite retries
- In production, consider implementing dead letter queues

## Monitoring

### Logs
- Console and file logging
- Log rotation (10MB max file size, 30 days retention)
- Different log levels for different components

### Metrics
- Processing rates
- Error rates
- Processing times
- Uptime statistics

### Health Checks
- Application health status
- Service uptime
- Basic connectivity checks

## Development

### Adding New Topics
1. Add topic configuration to `application.yml`
2. Create new `@KafkaListener` method in `ReviewConsumerService`
3. Implement processing logic
4. Add metrics tracking

### Custom Processing Logic
Replace the placeholder processing methods in `ReviewConsumerService`:
- `processReview(Review review)`: Handle valid reviews
- `processBadReview(String badReviewJson)`: Handle bad reviews

### Testing
```bash
# Run tests
mvn test

# Run with test profile
mvn spring-boot:run -Dspring.profiles.active=test
```

## Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Kafka Topics  │    │ Review Consumer │    │   Processing    │
│                 │    │                 │    │   Services      │
│ ┌─────────────┐ │    │ ┌─────────────┐ │    │ ┌─────────────┐ │
│ │   reviews   │◄┼────┼►│  Consumer   │ │    │ │   Review    │ │
│ └─────────────┘ │    │ │   Service   │ │    │ │ Processing  │ │
│ ┌─────────────┐ │    │ └─────────────┘ │    │ └─────────────┘ │
│ │bad_review_  │◄┼────┼►│  Metrics     │ │    │ ┌─────────────┐ │
│ │  records    │ │    │ │   Service    │ │    │ │ Bad Review  │ │
│ └─────────────┘ │    │ └─────────────┘ │    │ │ Processing  │ │
└─────────────────┘    └─────────────────┘    │ └─────────────┘ │
                                              └─────────────────┘
```

## Future Enhancements

- Database integration for storing processed reviews
- Dead letter queue for failed messages
- Retry mechanisms with exponential backoff
- Circuit breaker pattern for downstream services
- Prometheus metrics integration
- Distributed tracing with OpenTelemetry
- Message deduplication
- Batch processing capabilities 
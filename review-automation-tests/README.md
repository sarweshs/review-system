# Review System Automation Tests

This module contains comprehensive integration and API automation tests for the review system. It uses TestContainers to provide isolated test environments with PostgreSQL and Kafka.

## Features

- **API Integration Tests**: Tests all REST endpoints of the review service
- **Kafka Integration Tests**: Tests message production and consumption
- **Database Integration Tests**: Tests data persistence and retrieval
- **End-to-End Tests**: Tests complete workflows from Kafka to database
- **TestContainers**: Isolated test environments with PostgreSQL and Kafka
- **REST Assured**: Powerful API testing framework
- **Comprehensive Assertions**: Detailed validation of responses and data

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- Docker (for TestContainers)
- Review System modules (review-core, review-service, review-consumer)

## Project Structure

```
review-automation-tests/
├── src/
│   └── test/
│       ├── java/
│       │   └── com/
│       │       └── reviewautomation/
│       │           ├── base/
│       │           │   └── BaseIntegrationTest.java
│       │           ├── config/
│       │           │   └── TestConfig.java
│       │           ├── tests/
│       │           │   ├── ReviewApiIntegrationTest.java
│       │           │   └── KafkaIntegrationTest.java
│       │           └── util/
│       │               └── TestDataBuilder.java
│       └── resources/
│           ├── application-test.yml
│           └── init-test-db.sql
├── pom.xml
└── README.md
```

## Test Categories

### 1. API Integration Tests (`ReviewApiIntegrationTest`)

Tests all REST endpoints of the review service:

- **Health Checks**: `/actuator/health`
- **Metrics**: `/actuator/metrics`
- **Review CRUD Operations**: `/api/reviews`
- **Filtering and Search**: Platform, rating, entity-based queries
- **Statistics**: Review statistics and summaries
- **Error Handling**: Invalid requests and edge cases
- **CORS**: Cross-origin request handling

### 2. Kafka Integration Tests (`KafkaIntegrationTest`)

Tests Kafka message flow:

- **Good Review Messages**: Production and consumption of good reviews
- **Bad Review Messages**: Production and consumption of bad reviews
- **Multiple Messages**: Batch processing scenarios
- **Error Handling**: Malformed JSON and edge cases
- **Message Validation**: Content and structure verification

### 3. Base Test Infrastructure

- **BaseIntegrationTest**: Common setup and utilities
- **TestConfig**: TestContainers configuration
- **TestDataBuilder**: Test data generation utilities

## Running Tests

### Run All Tests

```bash
mvn clean test
```

### Run Specific Test Categories

```bash
# Run only API tests
mvn test -Dtest=ReviewApiIntegrationTest

# Run only Kafka tests
mvn test -Dtest=KafkaIntegrationTest

# Run tests with specific profile
mvn test -Dspring.profiles.active=test
```

### Run Integration Tests Only

```bash
mvn verify -Dskip.unit.tests=true
```

### Run Tests with Debug Logging

```bash
mvn test -Dlogging.level.com.reviewautomation=DEBUG
```

## Test Configuration

### TestContainers Setup

The tests automatically start:
- **PostgreSQL Container**: Test database with schema initialization
- **Kafka Container**: Message broker for testing

### Database Schema

The test database is initialized with:
- `entities` table
- `entity_reviews` table (composite primary key)
- `reviewer_info` table (composite primary key)
- `overall_provider_scores` table (composite primary key)
- `bad_review_records` table
- Appropriate indexes and foreign key constraints

### Test Data

The `TestDataBuilder` utility provides:
- Sample review messages
- Test entities
- Review data with various scenarios
- Bad review data for error testing

## Test Scenarios

### API Test Scenarios

1. **Health and Monitoring**
   - Health check endpoint
   - Metrics endpoint
   - Service readiness

2. **Review Operations**
   - Get all reviews with pagination
   - Get reviews by platform
   - Get reviews by entity ID
   - Get reviews by rating range
   - Search reviews by text
   - Get review statistics

3. **Error Handling**
   - Invalid pagination parameters
   - Invalid sort fields
   - Non-existent resources
   - Malformed requests

4. **Performance and Reliability**
   - Response time validation
   - Content type verification
   - CORS header validation

### Kafka Test Scenarios

1. **Message Flow**
   - Produce good review messages
   - Produce bad review messages
   - Consume and validate messages
   - Multiple message processing

2. **Error Scenarios**
   - Malformed JSON handling
   - Connection failures
   - Message serialization issues

3. **Performance**
   - Batch message processing
   - Concurrent message handling
   - Message ordering validation

## Test Data Management

### Test Data Builder

The `TestDataBuilder` class provides methods to create:
- `ReviewMessage` objects with realistic data
- `EntityReview` objects for database testing
- `ReviewEntity` objects for entity testing
- Various scenarios (good reviews, bad reviews, edge cases)

### Database Cleanup

Tests automatically clean up data between runs:
- Database is recreated for each test class
- TestContainers provide isolated environments
- No data leakage between tests

## Continuous Integration

### GitHub Actions Integration

Add to your CI pipeline:

```yaml
- name: Run Integration Tests
  run: |
    mvn clean verify -pl review-automation-tests
  env:
    DOCKER_HOST: unix:///var/run/docker.sock
```

### Docker Requirements

Ensure Docker is available in CI:
- Docker daemon running
- Sufficient resources for containers
- Network access for container images

## Troubleshooting

### Common Issues

1. **Docker Not Available**
   ```
   Error: Could not find a valid Docker environment
   ```
   Solution: Ensure Docker is running and accessible

2. **Port Conflicts**
   ```
   Error: Port already in use
   ```
   Solution: Tests use random ports, but ensure no conflicting services

3. **Container Startup Timeout**
   ```
   Error: Container startup timeout
   ```
   Solution: Increase timeout or check system resources

4. **Database Connection Issues**
   ```
   Error: Connection refused
   ```
   Solution: Check TestContainers configuration and Docker networking

### Debug Mode

Enable debug logging:

```bash
mvn test -Dlogging.level.com.reviewautomation=DEBUG \
         -Dlogging.level.org.testcontainers=DEBUG
```

### Manual Container Inspection

```bash
# List running containers
docker ps

# Check container logs
docker logs <container-id>

# Execute commands in container
docker exec -it <container-id> /bin/bash
```

## Best Practices

### Test Design

1. **Isolation**: Each test should be independent
2. **Cleanup**: Always clean up test data
3. **Realistic Data**: Use realistic test data
4. **Assertions**: Comprehensive validation of responses
5. **Error Scenarios**: Test both success and failure cases

### Performance

1. **Container Reuse**: TestContainers reuse containers when possible
2. **Parallel Execution**: Tests can run in parallel
3. **Resource Management**: Proper cleanup of resources
4. **Timeout Configuration**: Appropriate timeouts for async operations

### Maintenance

1. **Test Data Updates**: Keep test data current with schema changes
2. **Dependency Updates**: Regular updates of test dependencies
3. **Documentation**: Keep test documentation current
4. **Code Review**: Review test code as thoroughly as application code

## Contributing

### Adding New Tests

1. Extend `BaseIntegrationTest` for integration tests
2. Use `TestDataBuilder` for test data
3. Follow naming conventions
4. Add comprehensive assertions
5. Include error scenarios

### Test Data Updates

1. Update `TestDataBuilder` when models change
2. Update database schema in `init-test-db.sql`
3. Update test configurations as needed
4. Validate test data against current schema

### Documentation

1. Update README for new features
2. Document new test scenarios
3. Update troubleshooting section
4. Keep configuration examples current 
# Dead Letter Queue (DLQ) Guide for Review Producer

## Overview

The Review Producer now includes a Dead Letter Queue (DLQ) system to handle records that are missing critical fields like `review_id` and `provider_id`. This ensures that data integrity is maintained while providing a mechanism to analyze and potentially reprocess problematic records.

## How DLQ Works

### 1. **Validation Flow**

The system uses a three-tier validation approach:

```
Input Record → Validation → Routing Decision
```

**Validation Results:**
- ✅ **Valid**: Sent to `good_review_records` topic
- ⚠️ **Invalid (Non-Critical)**: Sent to `bad_review_records` topic  
- 🚨 **Invalid (Critical Fields Missing)**: Sent to `dlq` topic

### 2. **Critical Field Validation**

Records are sent to DLQ when any of these critical fields are missing or invalid:

- `comment.hotelReviewId` (review_id) - **REQUIRED**
- `comment.providerId` (provider_id) - **REQUIRED**

**DLQ Triggers:**
- `REVIEW_ID_MISSING`: `hotelReviewId` field is null or missing
- `PROVIDER_ID_MISSING`: `providerId` field is null or missing
- `COMMENT_SECTION_MISSING`: Entire comment section is missing
- `REVIEW_ID_INVALID_VALUE`: `hotelReviewId` is 0 or negative
- `PROVIDER_ID_INVALID_VALUE`: `providerId` is 0 or negative
- `REVIEW_ID_NOT_NUMBER`: `hotelReviewId` is not a valid number
- `PROVIDER_ID_NOT_NUMBER`: `providerId` is not a valid number

### 3. **Topic Structure**

```
Kafka Topics:
├── good_review_records (valid reviews)
├── bad_review_records (invalid but non-critical issues)
└── dlq (critical field missing - requires manual intervention)
```

## Configuration

### Application Properties

```yaml
kafka:
  topic:
    reviews: good_review_records
    bad-reviews: bad_review_records
    dlq: dlq
```

### Kafka Topic Configuration

The DLQ topic is automatically created with:
- **Partitions**: 3
- **Replicas**: 1
- **Retention**: Default Kafka retention policy

## DLQ Record Format

Records sent to DLQ have the following JSON structure:

```json
{
  "reviewId": 987654321,
  "providerId": 334,
  "originalJson": "{...original review JSON...}",
  "platform": "Booking.com",
  "reason": "REVIEW_ID_MISSING",
  "timestamp": 1703123456789
}
```

**Fields:**
- `reviewId`: Extracted review ID (null if missing)
- `providerId`: Extracted provider ID (null if missing)
- `originalJson`: Complete original review JSON
- `platform`: Source platform identifier
- `reason`: Validation failure reason
- `timestamp`: Unix timestamp when sent to DLQ

## API Endpoints

### 1. **Process Review with Validation**
```http
POST /api/producer/review
Content-Type: application/json

{
  "reviewJson": "{...review JSON...}",
  "platform": "Booking.com"
}
```

### 2. **Send Directly to DLQ**
```http
POST /api/producer/review/dlq
Content-Type: application/json

{
  "reviewJson": "{...review JSON...}",
  "platform": "Booking.com"
}
```

### 3. **Send Valid Review**
```http
POST /api/producer/review/valid
Content-Type: application/json

{
  "reviewJson": "{...review JSON...}",
  "platform": "Booking.com"
}
```

### 4. **Send Bad Review**
```http
POST /api/producer/review/bad
Content-Type: application/json

{
  "reviewJson": "{...review JSON...}",
  "platform": "Booking.com"
}
```

## Usage Examples

### Example 1: Valid Review
```json
{
  "hotelId": 12345,
  "hotelName": "Test Hotel",
  "platform": "Booking.com",
  "comment": {
    "hotelReviewId": 987654321,
    "providerId": 334,
    "rating": 4.5,
    "reviewComments": "Great hotel!"
  }
}
```
**Result**: Sent to `good_review_records` topic

### Example 2: Missing Review ID
```json
{
  "hotelId": 12345,
  "hotelName": "Test Hotel",
  "platform": "Booking.com",
  "comment": {
    "providerId": 334,
    "rating": 4.5,
    "reviewComments": "Great hotel!"
  }
}
```
**Result**: Sent to `dlq` topic with reason `REVIEW_ID_MISSING`

### Example 3: Missing Provider ID
```json
{
  "hotelId": 12345,
  "hotelName": "Test Hotel",
  "platform": "Booking.com",
  "comment": {
    "hotelReviewId": 987654321,
    "rating": 4.5,
    "reviewComments": "Great hotel!"
  }
}
```
**Result**: Sent to `dlq` topic with reason `PROVIDER_ID_MISSING`

### Example 4: Missing Hotel Name (Non-Critical)
```json
{
  "hotelId": 12345,
  "platform": "Booking.com",
  "comment": {
    "hotelReviewId": 987654321,
    "providerId": 334,
    "rating": 4.5
  }
}
```
**Result**: Sent to `bad_review_records` topic with reason `HOTEL_NAME_NULL`

## Monitoring and Alerting

### Log Messages

The system logs different levels of information:

```java
// Valid review
log.debug("Valid review sent to Kafka");

// Bad review (non-critical)
log.warn("Bad review record detected - Platform: {}, Reason: {}, Record: {}", 
         platform, reason, reviewJson);

// DLQ record (critical)
log.warn("Record sent to DLQ - Platform: {}, Reason: {}, ReviewId: {}, ProviderId: {}", 
         platform, reason, reviewId, providerId);
```

### Metrics

Monitor these metrics for DLQ health:
- DLQ message count
- DLQ message rate
- DLQ processing latency
- DLQ error rate

## Best Practices

### 1. **DLQ Monitoring**
- Set up alerts for high DLQ message rates
- Monitor DLQ topic size and growth
- Review DLQ messages regularly to identify data quality issues

### 2. **Data Quality**
- Investigate patterns in DLQ messages
- Work with data providers to fix upstream issues
- Consider implementing data quality dashboards

### 3. **Reprocessing**
- Develop tools to reprocess DLQ messages after fixing issues
- Implement idempotency to handle duplicate processing
- Test reprocessing logic thoroughly

### 4. **Retention Policy**
- Set appropriate retention policies for DLQ topics
- Archive old DLQ messages for analysis
- Clean up resolved DLQ messages

## Troubleshooting

### Common Issues

1. **High DLQ Volume**
   - Check data source quality
   - Review validation logic
   - Investigate upstream data changes

2. **DLQ Processing Errors**
   - Check Kafka connectivity
   - Verify topic permissions
   - Review application logs

3. **Missing DLQ Messages**
   - Verify DLQ topic exists
   - Check producer configuration
   - Review error handling logic

### Debug Commands

```bash
# Check DLQ topic
kafka-topics.sh --describe --topic dlq --bootstrap-server localhost:9092

# Consume DLQ messages
kafka-console-consumer.sh --topic dlq --bootstrap-server localhost:9092 --from-beginning

# Check DLQ topic size
kafka-run-class.sh kafka.tools.GetOffsetShell --broker-list localhost:9092 --topic dlq
```

## Integration with Consumer

The review-consumer can be enhanced to:
- Consume from DLQ topic
- Implement reprocessing logic
- Provide DLQ management APIs
- Generate DLQ analytics and reports

This DLQ system ensures data integrity while providing visibility into data quality issues that require manual intervention. 
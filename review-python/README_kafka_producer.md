# Kafka Producer for Review System

This Python script reads a JSONL file and sends each line to a Kafka topic. It's designed to work with the review system's data format.

## Features

- Reads JSONL files line by line
- Sends each JSON object to Kafka topic
- Configurable sleep interval between loops
- Uses hotelId as message key (configurable)
- Comprehensive error handling and logging
- Support for single run or continuous loop
- Dry-run mode for testing
- Uses confluent-kafka library for reliable Kafka communication

## Prerequisites

1. **Python 3.7+**
2. **Kafka running in Docker** (or any Kafka cluster)
3. **Required Python packages** (install via `pip install -r requirements.txt`)

## Installation

```bash
# Activate virtual environment (if using one)
source ../.venv/bin/activate

# Install dependencies
pip install -r requirements.txt
```

## Usage

### Basic Usage (Default Settings)

```bash
python kafka_producer.py
```

This will:
- Read from `sample.jl` file
- Connect to Kafka at `localhost:9092`
- Send messages to `reviews` topic
- Use `hotelId` as message key
- Run once (no loop)

### Advanced Usage

#### Send to Different Kafka Cluster
```bash
python kafka_producer.py --bootstrap-servers kafka.example.com:9092
```

#### Use Different Topic
```bash
python kafka_producer.py --topic hotel-reviews
```

#### Run in Continuous Loop
```bash
python kafka_producer.py --loop
```

#### Custom Sleep Interval
```bash
python kafka_producer.py --loop --sleep 60  # Sleep 1 minute between loops
```

#### Use Different Key Field
```bash
python kafka_producer.py --key-field providerId
```

#### Dry Run (Test Mode)
```bash
python kafka_producer.py --dry-run
```

#### Read Different File
```bash
python kafka_producer.py --file /path/to/reviews.jl
```

### Test Script

For quick testing with shorter intervals:

```bash
python test_kafka_producer.py
```

This runs the producer with:
- 10-second sleep interval
- Loop mode enabled
- Dry-run mode (no actual Kafka messages)

## Configuration

### Environment Variables

You can set these environment variables for default values:

- `KAFKA_BOOTSTRAP_SERVERS`: Default Kafka servers
- `KAFKA_TOPIC`: Default topic name
- `REVIEW_FILE`: Default JSONL file path

### Kafka Configuration

The producer uses these Kafka settings:
- `acks: 'all'` - Wait for all replicas
- `retries: 3` - Retry failed messages
- `max.in.flight.requests.per.connection: 1` - Ensure ordering

## Data Format

The script expects JSONL format where each line is a valid JSON object:

```json
{
  "hotelId": 10984,
  "platform": "Agoda",
  "hotelName": "Oscar Saigon Hotel",
  "comment": {
    "rating": 6.4,
    "reviewComments": "Hotel room is basic...",
    "reviewDate": "2025-04-10T05:37:00+07:00"
  }
}
```

## Error Handling

- **Invalid JSON**: Logs error and continues with next line
- **Kafka Connection**: Retries with exponential backoff
- **Message Delivery**: Logs success/failure for each message
- **File Not Found**: Exits with error code 1

## Logging

The script provides detailed logging:
- Connection status
- Message delivery confirmations
- Error details
- Statistics (sent/failed counts)

## Examples

### Production Usage
```bash
# Run continuously, sending to production Kafka
python kafka_producer.py \
  --bootstrap-servers kafka-prod:9092 \
  --topic hotel-reviews-prod \
  --loop \
  --sleep 300
```

### Development Testing
```bash
# Test with local Kafka
python kafka_producer.py \
  --bootstrap-servers localhost:9092 \
  --topic reviews-dev \
  --dry-run \
  --loop \
  --sleep 30
```

### Custom Configuration
```bash
# Use custom file and settings
python kafka_producer.py \
  --file /data/reviews/2025-04-10.jl \
  --bootstrap-servers kafka1:9092,kafka2:9092 \
  --topic daily-reviews \
  --key-field hotelId \
  --loop \
  --sleep 600
```

## Troubleshooting

### Connection Issues
- Verify Kafka is running: `docker ps | grep kafka`
- Check bootstrap servers are accessible
- Ensure topic exists in Kafka

### Permission Issues
- Make script executable: `chmod +x kafka_producer.py`
- Check file read permissions

### Dependencies
- Install confluent-kafka: `pip install confluent-kafka`
- Ensure virtual environment is activated

## Dependencies

- `confluent-kafka>=2.3.0` - Kafka client library
- Standard Python libraries (json, time, argparse, logging, os) 
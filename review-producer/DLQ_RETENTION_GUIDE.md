# DLQ Retention Guide

## Overview

DLQ (Dead Letter Queue) retention determines how long messages remain in the DLQ topic before being automatically deleted. Proper retention configuration is crucial for managing storage costs, system performance, and data governance.

## Retention Configuration

### Current Settings

```yaml
kafka:
  dlq:
    retention:
      ms: 604800000      # 7 days (7 * 24 * 60 * 60 * 1000)
      bytes: 1073741824  # 1GB (1024^3)
    segment:
      ms: 86400000       # 1 day segment rotation
```

### Configuration Parameters

| Parameter | Value | Description |
|-----------|-------|-------------|
| `retention.ms` | 604800000 | Messages kept for 7 days |
| `retention.bytes` | 1073741824 | Topic size limited to 1GB |
| `segment.ms` | 86400000 | Log segments rotated daily |
| `cleanup.policy` | delete | Messages deleted after retention |
| `delete.retention.ms` | 1000 | Immediate deletion after retention |

## Retention Strategies

### 1. **Time-Based Retention (Recommended)**

**Advantages:**
- Predictable cleanup schedule
- Easy to understand and manage
- Consistent with compliance requirements

**Configuration:**
```yaml
kafka:
  dlq:
    retention:
      ms: 604800000  # 7 days
```

**Use Cases:**
- Most DLQ scenarios
- Compliance requirements
- Predictable storage costs

### 2. **Size-Based Retention**

**Advantages:**
- Controls storage usage
- Prevents disk space issues

**Configuration:**
```yaml
kafka:
  dlq:
    retention:
      bytes: 1073741824  # 1GB
```

**Use Cases:**
- Limited storage environments
- High-volume DLQ scenarios

### 3. **Hybrid Retention**

**Advantages:**
- Both time and size limits
- Comprehensive protection

**Configuration:**
```yaml
kafka:
  dlq:
    retention:
      ms: 604800000      # 7 days OR
      bytes: 1073741824  # 1GB (whichever comes first)
```

## Retention Period Recommendations

### **Development/Testing: 1-3 Days**
```yaml
kafka:
  dlq:
    retention:
      ms: 259200000  # 3 days
```

### **Production: 7-30 Days**
```yaml
kafka:
  dlq:
    retention:
      ms: 604800000  # 7 days (current)
      # OR
      ms: 2592000000 # 30 days (for compliance)
```

### **Compliance/Regulatory: 30-90 Days**
```yaml
kafka:
  dlq:
    retention:
      ms: 7776000000  # 90 days
```

## Monitoring Retention

### 1. **Topic Metrics**

Monitor these Kafka metrics:
- `kafka.log.logend.retention.ms` - Current retention setting
- `kafka.log.logend.retention.bytes` - Size-based retention
- `kafka.log.logend.retention.check.interval.ms` - Cleanup frequency

### 2. **DLQ Health Checks**

```bash
# Check DLQ topic configuration
kafka-topics.sh --describe --topic dlq --bootstrap-server localhost:9092

# Check DLQ topic size
kafka-run-class.sh kafka.tools.GetOffsetShell --broker-list localhost:9092 --topic dlq

# Monitor DLQ message age
kafka-console-consumer.sh --topic dlq --bootstrap-server localhost:9092 --property print.timestamp=true
```

### 3. **Application Metrics**

Monitor these application metrics:
- DLQ message count
- DLQ message age distribution
- DLQ cleanup events
- Storage usage trends

## Retention Best Practices

### 1. **Align with Business Requirements**

```yaml
# High-priority data (e.g., financial transactions)
kafka:
  dlq:
    retention:
      ms: 2592000000  # 30 days

# Standard business data
kafka:
  dlq:
    retention:
      ms: 604800000   # 7 days

# Development/testing data
kafka:
  dlq:
    retention:
      ms: 86400000    # 1 day
```

### 2. **Consider Data Volume**

```yaml
# High-volume DLQ (1000+ messages/day)
kafka:
  dlq:
    retention:
      ms: 259200000   # 3 days
      bytes: 536870912 # 512MB

# Low-volume DLQ (<100 messages/day)
kafka:
  dlq:
    retention:
      ms: 604800000   # 7 days
      bytes: 1073741824 # 1GB
```

### 3. **Storage Cost Optimization**

```yaml
# Cost-sensitive environments
kafka:
  dlq:
    retention:
      ms: 172800000   # 2 days
      bytes: 268435456 # 256MB
    segment:
      ms: 3600000     # 1 hour segments (faster cleanup)
```

### 4. **Compliance Considerations**

```yaml
# GDPR compliance (30 days minimum)
kafka:
  dlq:
    retention:
      ms: 2592000000  # 30 days

# SOX compliance (7 years)
kafka:
  dlq:
    retention:
      ms: 220752000000 # 7 years (consider archival instead)
```

## Retention Management

### 1. **Dynamic Retention Updates**

You can update retention settings without recreating the topic:

```bash
# Update retention time
kafka-configs.sh --bootstrap-server localhost:9092 \
  --entity-type topics --entity-name dlq \
  --alter --add-config retention.ms=259200000

# Update retention size
kafka-configs.sh --bootstrap-server localhost:9092 \
  --entity-type topics --entity-name dlq \
  --alter --add-config retention.bytes=536870912
```

### 2. **Retention Monitoring Script**

```bash
#!/bin/bash
# monitor-dlq-retention.sh

TOPIC="dlq"
BROKER="localhost:9092"

echo "DLQ Retention Monitoring"
echo "======================="

# Get topic configuration
echo "Topic Configuration:"
kafka-configs.sh --bootstrap-server $BROKER \
  --entity-type topics --entity-name $TOPIC --describe

# Get topic size
echo -e "\nTopic Size:"
kafka-run-class.sh kafka.tools.GetOffsetShell \
  --broker-list $BROKER --topic $TOPIC

# Get message count
echo -e "\nMessage Count:"
kafka-run-class.sh kafka.tools.GetOffsetShell \
  --broker-list $BROKER --topic $TOPIC | \
  awk -F: '{sum += $3} END {print "Total messages: " sum}'
```

### 3. **Retention Alerts**

Set up alerts for:
- DLQ topic size approaching retention limit
- High message age (approaching retention period)
- Retention policy violations
- Storage usage thresholds

## Troubleshooting Retention Issues

### 1. **Messages Not Being Deleted**

**Symptoms:**
- DLQ topic size not decreasing
- Old messages still present after retention period

**Causes:**
- Incorrect retention configuration
- Cleanup policy not set to "delete"
- Kafka broker issues

**Solutions:**
```bash
# Check retention configuration
kafka-configs.sh --bootstrap-server localhost:9092 \
  --entity-type topics --entity-name dlq --describe

# Force cleanup
kafka-topics.sh --bootstrap-server localhost:9092 \
  --alter --topic dlq --config cleanup.policy=delete
```

### 2. **Premature Message Deletion**

**Symptoms:**
- Messages disappearing before retention period
- Unexpected data loss

**Causes:**
- Incorrect retention.ms value
- Size-based retention triggering first
- Manual topic cleanup

**Solutions:**
```bash
# Verify retention settings
kafka-configs.sh --bootstrap-server localhost:9092 \
  --entity-type topics --entity-name dlq --describe

# Check for manual cleanup operations
grep "cleanup" /var/log/kafka/server.log
```

### 3. **Storage Issues**

**Symptoms:**
- Disk space warnings
- Kafka broker performance degradation

**Causes:**
- Retention period too long
- High DLQ message volume
- Insufficient storage allocation

**Solutions:**
```yaml
# Reduce retention period
kafka:
  dlq:
    retention:
      ms: 172800000   # 2 days instead of 7
      bytes: 268435456 # 256MB instead of 1GB
```

## Environment-Specific Configurations

### Development Environment
```yaml
kafka:
  dlq:
    retention:
      ms: 86400000    # 1 day
      bytes: 134217728 # 128MB
    segment:
      ms: 3600000     # 1 hour
```

### Staging Environment
```yaml
kafka:
  dlq:
    retention:
      ms: 259200000   # 3 days
      bytes: 268435456 # 256MB
    segment:
      ms: 86400000    # 1 day
```

### Production Environment
```yaml
kafka:
  dlq:
    retention:
      ms: 604800000   # 7 days
      bytes: 1073741824 # 1GB
    segment:
      ms: 86400000    # 1 day
```

## Summary

DLQ retention is a critical aspect of data management that balances:
- **Data availability** for analysis and reprocessing
- **Storage costs** and resource utilization
- **Compliance requirements** and data governance
- **System performance** and operational efficiency

Choose retention settings based on your specific business requirements, data volume, and compliance needs. Monitor retention behavior regularly and adjust settings as your system evolves. 
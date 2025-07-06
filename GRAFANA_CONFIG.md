# Grafana Configuration Guide for Review System

This guide covers setting up Grafana dashboards to monitor the Review System's producer and consumer services using Prometheus metrics.

## Prerequisites

- Docker and Docker Compose running
- Prometheus and Grafana services running (already configured in `docker-compose.yaml`)
- Review Producer running on port 7072
- Review Consumer running on port 7073
- Both services exposing Prometheus metrics at `/actuator/prometheus`

## 1. Access Grafana

1. Open your browser and go to [http://localhost:3000](http://localhost:3000)
2. Login with default credentials:
   - **Username**: `admin`
   - **Password**: `admin`
3. You'll be prompted to change the password on first login (recommended)

## 2. Add Prometheus Data Source

1. Go to **Settings** (gear icon) → **Data Sources**
2. Click **Add data source**
3. Select **Prometheus**
4. Configure the data source:
   - **Name**: `Prometheus`
   - **URL**: `http://prometheus:9090`
   - **Access**: `Server (default)`
5. Click **Save & Test**
6. You should see a green "Data source is working" message

## 3. Create Review System Dashboard

### 3.1 Create New Dashboard

1. Click **+** (plus icon) → **Dashboard**
2. Click **Add new panel**

### 3.2 Add Review Producer Metrics

#### Files Processed Counter
- **Query**: `review_producer_files_processed_total`
- **Panel Title**: `Files Processed`
- **Visualization**: **Stat**
- **Field**: `Last (not null)`

#### Lines Processed Counter
- **Query**: `review_producer_lines_processed_total`
- **Panel Title**: `Lines Processed`
- **Visualization**: **Stat**
- **Field**: `Last (not null)`

#### Valid vs Invalid Reviews
- **Query A**: `review_producer_valid_reviews_total`
- **Query B**: `review_producer_invalid_reviews_total`
- **Panel Title**: `Valid vs Invalid Reviews`
- **Visualization**: **Time series**
- **Legend**: `Valid Reviews`, `Invalid Reviews`

#### File Processing Duration
- **Query**: `rate(review_producer_file_processing_duration_seconds_sum[5m]) / rate(review_producer_file_processing_duration_seconds_count[5m])`
- **Panel Title**: `Average File Processing Time`
- **Visualization**: **Time series**
- **Unit**: `s`

#### Active Threads Gauge
- **Query**: `review_producer_active_threads`
- **Panel Title**: `Active Processing Threads`
- **Visualization**: **Gauge**
- **Min**: `0`
- **Max**: `10`

#### Queue Depth
- **Query**: `review_producer_queue_depth`
- **Panel Title**: `Processing Queue Depth`
- **Visualization**: **Gauge**
- **Min**: `0`
- **Max**: `100`

### 3.3 Add Review Consumer Metrics

#### Processed Reviews Counter
- **Query**: `review_consumer_processed_reviews_total`
- **Panel Title**: `Reviews Processed`
- **Visualization**: **Stat**
- **Field**: `Last (not null)`

#### Bad Reviews Counter
- **Query**: `review_consumer_bad_reviews_total`
- **Panel Title**: `Bad Reviews Processed`
- **Visualization**: **Stat**
- **Field**: `Last (not null)`

#### Processing Rate
- **Query**: `rate(review_consumer_processed_reviews_total[5m])`
- **Panel Title**: `Reviews Processing Rate`
- **Visualization**: **Time series**
- **Unit**: `reviews/sec`

#### Error Rate
- **Query**: `rate(review_consumer_errors_total[5m])`
- **Panel Title**: `Error Rate`
- **Visualization**: **Time series**
- **Unit**: `errors/sec`

#### Processing Duration
- **Query**: `rate(review_consumer_processing_duration_seconds_sum[5m]) / rate(review_consumer_processing_duration_seconds_count[5m])`
- **Panel Title**: `Average Processing Time`
- **Visualization**: **Time series**
- **Unit**: `s`

### 3.4 Add System Health Metrics

#### JVM Memory Usage
- **Query**: `jvm_memory_used_bytes{job=~"review-producer|review-consumer"}`
- **Panel Title**: `JVM Memory Usage`
- **Visualization**: **Time series**
- **Unit**: `bytes`

#### JVM Threads
- **Query**: `jvm_threads_live_threads{job=~"review-producer|review-consumer"}`
- **Panel Title**: `JVM Live Threads`
- **Visualization**: **Time series**

#### HTTP Request Rate
- **Query**: `rate(http_server_requests_seconds_count{job=~"review-producer|review-consumer"}[5m])`
- **Panel Title**: `HTTP Request Rate`
- **Visualization**: **Time series**
- **Unit**: `req/sec`

## 4. Dashboard Layout

### 4.1 Organize Panels

Arrange your panels in a logical order:

**Row 1: System Overview**
- Files Processed (Producer)
- Reviews Processed (Consumer)
- Active Threads (Producer)
- Queue Depth (Producer)

**Row 2: Processing Metrics**
- Valid vs Invalid Reviews (Producer)
- Processing Rate (Consumer)
- Error Rate (Consumer)
- Bad Reviews Processed (Consumer)

**Row 3: Performance Metrics**
- File Processing Duration (Producer)
- Processing Duration (Consumer)
- JVM Memory Usage
- JVM Live Threads

**Row 4: System Health**
- HTTP Request Rate
- Lines Processed (Producer)

### 4.2 Dashboard Settings

1. Click the dashboard settings (gear icon)
2. Set **Title**: `Review System Monitoring`
3. Set **Description**: `Comprehensive monitoring dashboard for Review Producer and Consumer services`
4. Set **Tags**: `review-system`, `kafka`, `spring-boot`
5. Set **Time range**: `Last 1 hour`
6. Set **Auto-refresh**: `30s`

## 5. Alerts (Optional)

### 5.1 Create Alert Rules

1. Go to **Alerting** → **Alert Rules**
2. Click **New alert rule**

#### High Error Rate Alert
- **Query**: `rate(review_consumer_errors_total[5m]) > 0.1`
- **Condition**: `Error rate is high`
- **Duration**: `2m`
- **Severity**: `Warning`

#### Queue Backlog Alert
- **Query**: `review_producer_queue_depth > 50`
- **Condition**: `Processing queue is backing up`
- **Duration**: `5m`
- **Severity**: `Warning`

#### High Processing Time Alert
- **Query**: `rate(review_producer_file_processing_duration_seconds_sum[5m]) / rate(review_producer_file_processing_duration_seconds_count[5m]) > 30`
- **Condition**: `File processing is taking too long`
- **Duration**: `5m`
- **Severity**: `Warning`

## 6. Useful Prometheus Queries

### 6.1 Review Producer Queries

```promql
# Total files processed
review_producer_files_processed_total

# Files processed per minute
rate(review_producer_files_processed_total[1m])

# Valid vs invalid reviews ratio
review_producer_valid_reviews_total / (review_producer_valid_reviews_total + review_producer_invalid_reviews_total)

# Average processing time per file
rate(review_producer_file_processing_duration_seconds_sum[5m]) / rate(review_producer_file_processing_duration_seconds_count[5m])

# Queue utilization percentage
(review_producer_queue_depth / 100) * 100
```

### 6.2 Review Consumer Queries

```promql
# Total reviews processed
review_consumer_processed_reviews_total

# Processing rate (reviews per second)
rate(review_consumer_processed_reviews_total[5m])

# Error rate
rate(review_consumer_errors_total[5m])

# Success rate percentage
(review_consumer_processed_reviews_total / (review_consumer_processed_reviews_total + review_consumer_bad_reviews_total)) * 100

# Average processing time
rate(review_consumer_processing_duration_seconds_sum[5m]) / rate(review_consumer_processing_duration_seconds_count[5m])
```

## 7. Troubleshooting

### 7.1 No Data Showing

1. **Check Prometheus Targets**:
   - Go to [http://localhost:9090/targets](http://localhost:9090/targets)
   - Ensure `review-producer` and `review-consumer` targets are `UP`

2. **Check Service Endpoints**:
   - Producer: [http://localhost:7072/actuator/prometheus](http://localhost:7072/actuator/prometheus)
   - Consumer: [http://localhost:7073/actuator/prometheus](http://localhost:7073/actuator/prometheus)

3. **Check Docker Network**:
   - If using `host.docker.internal`, ensure it resolves correctly
   - On Linux, replace with your host IP address

### 7.2 Metrics Not Available

1. **Verify Spring Boot Configuration**:
   - Ensure `management.endpoints.web.exposure.include=prometheus` is set
   - Check that Micrometer Prometheus dependency is included

2. **Check Application Logs**:
   - Look for any errors related to metrics or actuator endpoints

### 7.3 Performance Issues

1. **Reduce Scrape Interval**: Change `scrape_interval` in `prometheus.yml` to `30s` or `60s`
2. **Filter Metrics**: Use metric relabeling to only collect needed metrics
3. **Increase Memory**: Allocate more memory to Prometheus container

## 8. Advanced Configuration

### 8.1 Metric Relabeling

Add to `prometheus.yml` to filter metrics:

```yaml
scrape_configs:
  - job_name: 'review-producer'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['host.docker.internal:7072']
    metric_relabel_configs:
      - source_labels: [__name__]
        regex: 'review_producer_.*'
        action: keep
```

### 8.2 Retention and Storage

Configure Prometheus retention in `docker-compose.yaml`:

```yaml
prometheus:
  image: prom/prometheus
  command:
    - '--config.file=/etc/prometheus/prometheus.yml'
    - '--storage.tsdb.retention.time=15d'
    - '--storage.tsdb.path=/prometheus'
  volumes:
    - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
    - prometheus_data:/prometheus
  ports:
    - "9090:9090"
```

## 9. Next Steps

1. **Customize Dashboards**: Add more panels based on your specific needs
2. **Set Up Alerts**: Configure alerting rules for critical metrics
3. **Add Log Correlation**: Use Loki to correlate logs with metrics
4. **Performance Tuning**: Optimize Prometheus and Grafana for your workload
5. **Backup Configuration**: Export dashboard JSON files for backup

## 10. Useful Links

- [Grafana Documentation](https://grafana.com/docs/)
- [Prometheus Query Language](https://prometheus.io/docs/prometheus/latest/querying/)
- [Micrometer Prometheus](https://micrometer.io/docs/registry/prometheus)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html) 
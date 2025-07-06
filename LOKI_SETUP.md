# Quick Loki Setup Guide

This guide helps you quickly set up Loki log monitoring for your Review System.

## 1. Start Loki and Promtail

Your `docker-compose.yaml` already includes Loki and Promtail. Start them:

```bash
docker-compose up -d loki promtail
```

## 2. Verify Services Are Running

```bash
# Check if services are up
docker-compose ps loki promtail

# Check Loki status
curl http://localhost:3100/ready

# Check Promtail status  
curl http://localhost:9080/ready
```

## 3. Create Log Directory

```bash
# Create logs directory for your applications
mkdir -p logs
```

## 4. Configure Your Spring Boot Applications

Update your Spring Boot applications to write logs to the `logs/` directory:

### For Review Producer (`review-producer/src/main/resources/application.yml`):
```yaml
logging:
  file:
    name: logs/review-producer.log
  pattern:
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
```

### For Review Consumer (`review-consumer/src/main/resources/application.yml`):
```yaml
logging:
  file:
    name: logs/review-consumer.log
  pattern:
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
```

## 5. Add Loki Data Source in Grafana

1. Go to [http://localhost:3000](http://localhost:3000)
2. Login with `admin` / `admin`
3. Go to **Settings** → **Data Sources**
4. Click **Add data source**
5. Select **Loki**
6. Configure:
   - **Name**: `Loki`
   - **URL**: `http://loki:3100`
   - **Access**: `Server (default)`
7. Click **Save & Test**

## 6. Test Log Collection

### 6.1 Create Test Logs

```bash
# Create some test logs
echo "$(date): Test log from review-producer" >> logs/review-producer.log
echo "$(date): Test log from review-consumer" >> logs/review-consumer.log
echo "$(date): ERROR - Test error log" >> logs/review-producer.log
```

### 6.2 Query Logs in Grafana

1. Go to **Explore** in Grafana
2. Select **Loki** as data source
3. Try these queries:

```logql
# All application logs
{job="apps"}

# Review producer logs
{job="apps"} |= "review-producer"

# Error logs
{job="apps"} |= "ERROR"

# Recent logs (last 5 minutes)
{job="apps"} [5m]
```

## 7. Create Log Dashboard

Follow the detailed instructions in `GRAFANA_CONFIG.md` section 4 to create a comprehensive log monitoring dashboard.

## 8. Troubleshooting

### No Logs Showing?

1. **Check if log files exist**:
   ```bash
   ls -la logs/
   ```

2. **Check Promtail logs**:
   ```bash
   docker-compose logs promtail
   ```

3. **Check Loki logs**:
   ```bash
   docker-compose logs loki
   ```

4. **Verify Promtail configuration**:
   ```bash
   curl http://localhost:9080/config
   ```

### Logs Not Being Collected?

1. **Check file permissions**:
   ```bash
   chmod 644 logs/*.log
   ```

2. **Restart Promtail**:
   ```bash
   docker-compose restart promtail
   ```

3. **Check if files are being written**:
   ```bash
   tail -f logs/review-producer.log
   ```

## 9. Useful Commands

```bash
# View all logs in real-time
docker-compose logs -f

# Check service status
docker-compose ps

# Restart log services
docker-compose restart loki promtail

# View Promtail targets
curl http://localhost:9080/targets

# Test Loki query
curl -G -s "http://localhost:3100/loki/api/v1/query_range" \
  --data-urlencode 'query={job="apps"}' \
  --data-urlencode 'start=2024-01-01T00:00:00Z' \
  --data-urlencode 'end=2024-12-31T23:59:59Z'
```

## 10. Next Steps

1. **Start your Spring Boot applications** and watch logs appear in Grafana
2. **Create custom dashboards** using the queries in `GRAFANA_CONFIG.md`
3. **Set up alerts** for error log rates
4. **Configure log retention** in `loki-config.yaml` if needed

Your Loki setup is now ready! Follow the detailed guide in `GRAFANA_CONFIG.md` for advanced configuration and dashboard creation. 
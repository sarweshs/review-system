# Grafana Log Monitoring Setup (Loki + Promtail)

This guide explains how to set up log aggregation and visualization for your Spring Boot services using Grafana, Loki, and Promtail. It covers both local development and Docker/Kubernetes environments.

---

## 1. Loki & Promtail Setup (Docker Compose)

### **A. Add Loki and Promtail to `docker-compose.yaml`**

```yaml
loki:
  image: grafana/loki:2.9.2
  ports:
    - "3100:3100"
  command: -config.file=/etc/loki/local-config.yaml
  volumes:
    - ./loki-config.yaml:/etc/loki/local-config.yaml

promtail:
  image: grafana/promtail:2.9.2
  volumes:
    - ./logs:/logs
    - ./promtail-config.yaml:/etc/promtail/config.yaml
  command: -config.file=/etc/promtail/config.yaml
  depends_on:
    - loki
```

### **B. Example `promtail-config.yaml`**

```yaml
server:
  http_listen_port: 9080
  grpc_listen_port: 0

positions:
  filename: /tmp/positions.yaml

clients:
  - url: http://loki:3100/loki/api/v1/push

scrape_configs:
  - job_name: springboot
    static_configs:
      - targets:
          - localhost
        labels:
          job: springboot
          __path__: /logs/*.log
```

- Make sure your Spring Boot log files are written to `./logs/` (local) or `/logs/` (in container).

### **C. Example `loki-config.yaml`**

You can use the default config or customize as needed. For most dev/test setups, the default is fine.

---

## 2. Start Loki and Promtail

```bash
docker-compose up -d loki promtail
```

---

## 3. Configure Grafana to View Logs

1. **Access Grafana:**
   - URL: [http://localhost:3000](http://localhost:3000)
   - Default login: `admin` / `admin`

2. **Add Loki as a Data Source:**
   - Go to **Configuration > Data Sources**
   - Click **Add data source**
   - Choose **Loki**
   - Set URL to `http://loki:3100`
   - Click **Save & Test**

3. **Explore Logs:**
   - Go to **Explore** in Grafana
   - Select **Loki** as the data source
   - Query logs, e.g.:
     ```
     {job="springboot"}
     ```
   - You should see your Spring Boot logs appear in real time.

---

## 4. Kubernetes/Production Notes

- Mount your log directory as a volume in your pod.
- Use a DaemonSet for Promtail or Fluent Bit to ship logs from `/logs/*.log` to Loki.
- Update Promtail config to match your log file paths in the cluster.

---

## 5. Switching to ClickStack/ClickHouse

- When you move to ClickStack, update your log shipper (Promtail, Fluent Bit, etc.) to send logs to ClickHouse instead of Loki.
- The rest of your logging setup (log file output, log levels, etc.) remains the same.

---

## 6. Troubleshooting

- Ensure your log files are being written to the directory Promtail is watching.
- Check Promtail and Loki logs for errors (`docker-compose logs promtail loki`).
- In Grafana, verify the Loki data source is healthy.

---

## 7. References
- [Grafana Loki Docs](https://grafana.com/docs/loki/latest/)
- [Promtail Docs](https://grafana.com/docs/loki/latest/clients/promtail/)
- [Spring Boot Logging](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.logging) 
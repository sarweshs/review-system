
# 🛠️ SETUP_INSTRUCTIONS

This guide outlines the prerequisites and steps to set up and run the **Review System**, a multi-module Maven project.

---

## 📦 Clone the Repository
### ✅ CI Enabled: This repository uses Continuous Integration via GitHub Actions to automatically build and test the code on every push and pull request.
#### CI workflows are defined under .github/workflows/ and ensure code quality through automated builds and checks.
Clone the repository using either SSH or HTTPS:

- **SSH**  
  ```bash
  git clone git@github.com:sarweshs/review-system.git
  ```

- **HTTPS**  
  ```bash
  git clone https://github.com/sarweshs/review-system.git
  ```
---

## ✅ Prerequisites

Ensure the following tools are installed and properly configured on your system:

- **Java 17+**
- **Apache Maven 3.9.9+**
- **Docker** (preferably with Docker Dashboard for GUI-based container monitoring)

---

## 🔌 Port Requirements

Make sure the following ports are free on your system. If any port is occupied, update the relevant ports in:

- `docker-compose-common.yml`
- `application.yml` of the respective services

### ✳️ Microservices

| Service           | Port |
|-------------------|------|
| review-consumer   | 7073 |
| review-dashboard  | 8081 |
| review-producer   | 7072 |
| review-service    | 7070 |

### 🔧 Common Infrastructure Services

| Service     | Port | Description |
|-------------|------|-------------|
| keycloak    | 8080 | Role-based access for `review-dashboard` |
| kafka       | 9092 | Message broker for event streaming |
| loki        | 3100 | Log collector for Grafana |
| prometheus  | 9090 | Metrics and log storage |
| minio       | 9000 | S3-compatible storage (MinIO / Storj) |
| vault       | 8200 | Secrets manager (stores AES key) |
| redis       | 6379 | In-memory cache |
| postgres    | 5432 | Review data storage |
| grafana     | 3000 | Monitoring dashboard |

---

## 🔐 Environment Variables

The system uses **MinIO** and **Storj (S3-compatible)** to fetch review records. Environment variables must be defined in a `.env` file at the root of the repository.

> 📄 Copy `env-template` to `.env` and populate it with the actual values.

```env
# AES Encryption Key
# Generate with: openssl rand -hex 16 | tr -d '\n'
AES_KEY=

# S3/Storj Credentials
AWS_ACCESS_KEY_ID=your-access-key
AWS_SECRET_ACCESS_KEY=your-secret-key
AWS_ENDPOINT=https://gateway.storjshare.io

# Vault Configuration (for secret management)
VAULT_ADDR=
VAULT_TOKEN=
VAULT_SECRET_PATH=

# Python integration (optional)
PYTHON_DATABSE_URL=
```

---

## ▶️ Start Common Services

1. Run the common services using the script:
   ```bash
   ./start-common-services.sh
   ```

2. Open Docker Dashboard to validate that all services are running on their respective ports.

3. Access the Keycloak console at [http://localhost:8080](http://localhost:8080) using:
   - Username: `admin`
   - Password: `admin`
   - Confirm `review-realm` is present in the top-left corner.

4. Access the Vault console at [http://localhost:8200](http://localhost:8200):
   - Auth type: `Token`
   - Token: `devroot`
   - You should see a configured secret/key.

> If any container is not up, fix it before proceeding.

To stop the services, run:
```bash
./stop-common-services.sh
```

---

## 🧱 Build and Run the Review System

This project is composed of **5 Maven modules**:

### 1. `review-core`
Contains common models shared across other modules.

### 2. `review-producer`
- Reads active review sources from the database.
- Fetches and validates review records.
- Sends valid records to Kafka topic: `good_review_records`.
- Sends invalid records to Kafka topic: `bad_review_records`.
- Exports metrics to Prometheus.

### 3. `review-consumer`
- Listens to Kafka topics `good_review_records` and `bad_review_records`.
- Processes and stores them in the database.
- Exports metrics to Prometheus.

### 4. `review-service`
- **Must be run first** as it uses **Flyway** to run DB migration scripts.
- Exposes various APIs to fetch review data.
- Postman collection is available in the root directory for testing.

### 5. `review-dashboard`
- Web dashboard for configuring review sources.
- Provides insights and visualizations.

---

## 🛠️ Build and Run

1. Build the Maven project:
   ```bash
   ./start-review-system.sh
   
   # For stopping and cleanup use
    ./stop-review-system.sh
   ```
## Alternatively, you can run the services individually but review-service must be started first:
2. Run the services individually:
   ```bash
   mvn spring-boot:run -pl <module-name>
   ```

   Replace `<module-name>` with one of:
   - `review-service`
   - `review-producer`
   - `review-consumer`
   - `review-dashboard`

---

## 🧪 Testing and Monitoring

- Use **Postman** collection in the root directory (Review_System_API_v2.2.postman_collection.json) to test APIs from `review-service`.
- View logs and metrics on **Grafana** (`http://localhost:3000`).
- **Loki** is used for logging, and **Prometheus** for metrics.

---

## 📝 Utilities
review-python contains Python utilities for testing and integration.

## 📘 Additional Resources

- 📄 [ZUZU-Review System – High-Level Design (PDF)](./ZUZU-Review-System-High-Level-Design.pdf)

## Future Improvements
- Real-time streaming when using AWS/GCP/Azure(e.g., AWS Lambda + SQS/Kinesis integration, GCP Cloud Functions + Pub/Sub, Azure Functions + Event Grid).
- For PoC webhooks can be used to trigger events. (MinIO supports webhooks, but not Storj)
- Integration tests for end-to-end testing.
- Disaster recovery and backup strategies.
- Enhanced security measures (e.g., OAuth2, JWT ) for APIs.
- Blue/Green deployments for zero-downtime updates.
# Review System Monorepo

This repository is a monorepo for the Review System, supporting both Java (Spring Boot, multi-module Maven) and Python projects.

## Structure

- `review-core/`        — Shared Java models and logic
- `review-service/`     — Main Spring Boot microservice (now runs on port 7070)
- `review-dashboard/`   — Dashboard/UI module (runs on port 8081)
- `review-python/`      — Python utilities mainly for testing

## Java (Maven Multi-Module)
- Build all modules:
  ```sh
  mvn clean install
  ```
- Each module has its own `pom.xml` and can be built/tested independently.

## Python
- See `review-python/README.md` for setup instructions.

## Ports
- **Keycloak**: 8080 (for authentication/SSO)
- **review-service**: 7070 (Spring Boot API)
- **review-dashboard**: 8081 (UI/dashboard)

## Keycloak Integration
- The system uses Keycloak (running on port 8080) for authentication and SSO.
- Make sure Keycloak is running and configured with the appropriate realms, clients, and users for your environment.
- The review-service and review-dashboard modules are configured to use Keycloak for OAuth2/OIDC login and security.

## Adding More Modules
- To add a new Java module: create a new directory, add a `pom.xml`, and add it to the `<modules>` section in the root `pom.xml`.
- To add a new Python service: create a new directory under `review-python/`.

## Monorepo Best Practices
- Keep each module self-contained.
- Use clear documentation in each module.
- Use CI/CD to test/build all modules on each commit.

# Review System Microservice

A production-grade, modular, scalable Spring Boot microservice for ingesting, processing, and storing hotel reviews from S3/MinIO/Cloud Storage, using Kafka, PostgreSQL, Redis, Prometheus, and Grafana.

## Features
- Periodically pulls review files from S3/MinIO/Cloud Storage
- Parses JSONL (.jl) review files
- Publishes reviews to Kafka for decoupled processing
- Consumes reviews from Kafka, validates, transforms, and stores in PostgreSQL
- Idempotent processing (no duplicate reviews)
- Caching support (Redis)
- Observability with Prometheus and Grafana
- Dockerized infrastructure for local development

## Architecture
```
[Storage (S3/MinIO/GCS)]
        |
        v
[Review Fetcher Service] --Kafka--> [Review Processor Service] --DB--> [PostgreSQL]
        |                                                    |
        |                                                    v
        |                                                [Redis]
        v
   [Prometheus/Grafana]
```

## Directory Structure
- `src/main/java/` - Java source code (modularized by concern)
- `src/main/resources/` - Config, schema, and data
- `docker-compose.yaml` - Orchestrates all services
- `minio/` - Sample .jl files and bucket config
- `grafana/`, `prometheus/` - Monitoring setup

## Getting Started

### Prerequisites
- Docker & Docker Compose
- Java 17+
- Maven or Gradle

### 1. Clone the Repository
```
git clone <repo-url>
cd review-system
```

### 2. Start Infrastructure
```
docker-compose up -d
```
- PostgreSQL: `localhost:5432`
- Kafka: `localhost:9092`
- MinIO: `localhost:9000` (UI: `localhost:9001`, user/pass: `minioadmin`)
- Redis: `localhost:6379`
- Prometheus: `localhost:9090`
- Grafana: `localhost:3000` (admin/admin)

### 3. Build and Run the Spring Boot App
```
./mvnw clean package
java -jar target/review-system-0.0.1-SNAPSHOT.jar
```

### 4. Configure Storage Backend
Edit `src/main/resources/application.yml`:
```
storage:
  backend: minio # or reviewstore or gcs
```

### 5. Add Review Files
- Upload `.jl` files to the `reviews/` folder in your storage backend (e.g., MinIO bucket).

### 6. Monitor
- Grafana dashboards: [http://localhost:3000](http://localhost:3000)
- Prometheus metrics: [http://localhost:9090](http://localhost:9090)

## Extending
- Implement the TODOs in storage service classes for S3, MinIO, and GCS.
- Add more validation/transformation logic in `ReviewService`.
- Add alerting, dead-letter queue, etc. as needed.

## License
MIT 

## Troubleshooting
docker-compose down -v
docker-compose build --no-cache
docker-compose up -d

## Connect to kafka running on port 9092
```sh
docker run -d -p 6060:8080 \
  -e KAFKA_CLUSTERS_0_NAME=local \
  -e KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS=host.docker.internal:9092 \
  provectuslabs/kafka-ui
  
 docker run -p 6060:8080 \
  -e KAFKA_BROKERS=host.docker.internal:9092 \
  docker.redpanda.com/redpandadata/console:latest
# If above doesn't work, try this:
docker network create kafka-net

docker network connect kafka-net kafka  # if not already in that network

docker run -d -p 6060:8080 \
  --network kafka-net \
  -e KAFKA_BROKERS=kafka:29092 \
  -e CONSOLE_AUTHENTICATION_ENABLED=false \
  docker.redpanda.com/redpandadata/console:latest
# If above doesn't work, try this:
docker network create kafka-net  # if not already created

docker network connect kafka-net kafka  # attach kafka container if needed

docker run -d -p 6060:8080 --network kafka-net \
  -e AKHQ_CONFIGURATION='
akhq:
  connections:
    my-cluster:
      properties:
        bootstrap.servers: "kafka:29092"
' \
  tchiotludo/akhq

# Reset KAFKA topic
./kafka-topics.sh --bootstrap-server localhost:9092 --list

./kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
     --group review-consumer-group \
     --topic bad_review_records \
     --reset-offsets --to-earliest --execute
````
## Clear Kafka Topic
```sh
./kafka-configs.sh --bootstrap-server localhost:9092 \
  --entity-type topics --entity-name bad_review_records \
  --alter --add-config retention.ms=1000
  
./kafka-configs.sh --bootstrap-server localhost:9092 \
  --entity-type topics --entity-name reviews \
  --alter --add-config retention.ms=1000
```
## Check Kafka Topics
```sh
kafka-topics.sh --bootstrap-server localhost:9092 --list
./kafka-run-class.sh kafka.tools.GetOffsetShell --broker-list localhost:9092 --topic bad_review_records
./kafka-run-class.sh kafka.tools.GetOffsetShell --broker-list localhost:9092 --topic good_review_records
```

## Delete Kafka Topic
```sh
# Delete
./kafka-topics.sh --bootstrap-server localhost:9092 --delete --topic bad_review_records
./kafka-topics.sh --bootstrap-server localhost:9092 --delete --topic good_review_records

# Recreate
./kafka-topics.sh --bootstrap-server localhost:9092 --create --topic bad_review_records --partitions 1 --replication-factor 1
./kafka-topics.sh --bootstrap-server localhost:9092 --create --topic good_review_records --partitions 1 --replication-factor 1

````
## check kafka messages
```sh
docker exec -it review-system-kafka-1 kafka-console-consumer --bootstrap-server localhost:9092 --topic good_review_records --from-beginning --max-messages 5
docker exec -it review-system-kafka-1 kafka-console-consumer --bootstrap-server localhost:9092 --topic bad_review_records --from-beginning --max-messages 5
```
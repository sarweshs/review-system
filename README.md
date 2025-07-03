# Review System Monorepo

This repository is a monorepo for the Review System, supporting both Java (Spring Boot, multi-module Maven) and Python projects.

## Structure

- `review-core/`        — Shared Java models and logic
- `review-service/`     — Main Spring Boot microservice
- `review-python/`      — Python microservices/utilities

## Java (Maven Multi-Module)
- Build all modules:
  ```sh
  mvn clean install
  ```
- Each module has its own `pom.xml` and can be built/tested independently.

## Python
- See `review-python/README.md` for setup instructions.

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

#!/bin/bash

echo "🔄 Rebuilding Spring Boot applications with fixed logging configuration..."

# Step 1: Build JARs locally
echo "🛠️ Running Maven build..."
mvn clean package -DskipTests

# Check if Maven build succeeded
if [ $? -ne 0 ]; then
    echo "❌ Maven build failed. Aborting Docker rebuild."
    exit 1
else
    echo "✅ Maven build succeeded. Continuing..."
fi

# Step 2: Stop only the Spring Boot services
echo "📦 Stopping Spring Boot services..."
docker-compose stop review-service review-dashboard review-producer review-consumer

# Step 3: Remove only the Spring Boot service containers
echo "🧹 Removing Spring Boot service containers..."
docker-compose rm -f review-service review-dashboard review-producer review-consumer

# Step 4: Rebuild only the Spring Boot applications
echo "🔨 Rebuilding Spring Boot applications..."
docker-compose build --no-cache review-service review-dashboard review-producer review-consumer

# Step 5: Start all services (infrastructure services will be reused)
echo "🚀 Starting all services..."
docker-compose up -d

# Step 6: Wait a moment for services to start
echo "⏳ Waiting for services to start..."
sleep 10

# Step 7: Check service status
echo "📊 Checking service status..."
docker-compose ps

# Step 8: Final output
echo "✅ Rebuild complete! Services should now be running with fixed logging configuration."
echo ""
echo "📋 Service URLs:"
echo "  - Review Dashboard: http://localhost:8081"
echo "  - Review Service API: http://localhost:7070"
echo "  - Review Producer: http://localhost:8082"
echo "  - Review Consumer: http://localhost:7073"
echo "  - Keycloak: http://localhost:8080"
echo "  - MinIO Console: http://localhost:9001"
echo "  - Grafana: http://localhost:3000"
echo "  - Prometheus: http://localhost:9090"
echo "  - Loki: http://localhost:3100"
echo ""
echo "📝 To view logs:"
echo "  - All logs: docker-compose logs -f"
echo "  - Specific service: docker-compose logs -f review-service"
echo "  - Log files are also persisted in Docker volume 'logs'"

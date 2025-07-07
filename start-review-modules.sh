#!/bin/bash

# Build all services
echo "Building all services..."
mvn clean package -DskipTests

# Define service names and ports
services=("review-service" "review-dashboard" "review-consumer" "review-producer")
ports=("7070" "8081" "7073" "7072")

# Function to check if port is in use and handle user consent
check_and_handle_port() {
  local port=$1

  if lsof -i :$port -sTCP:LISTEN -t >/dev/null; then
    echo "⚠️  Port $port is already in use."
    read -p "Do you want to kill the process using port $port? [y/N]: " choice
    case "$choice" in
      y|Y )
        pid=$(lsof -ti tcp:$port)
        echo "Killing process $pid using port $port..."
        kill -9 "$pid"
        sleep 1  # Give the system a moment to release the port
        ;;
      * )
        echo "❌ Please stop the service using port $port or change the port and try again."
        exit 1
        ;;
    esac
  fi
}

# Start services
echo "Starting services..."
for i in "${!services[@]}"; do
  service="${services[$i]}"
  port="${ports[$i]}"
  check_and_handle_port "$port"

  nohup java -jar "$service/target/$service-0.0.1-SNAPSHOT.jar" > "$service.log" 2>&1 &
  echo $! > "$service.pid"
  echo "Started $service (PID: $(cat $service.pid)) on port $port"
done

# Health check function
health_check() {
  local service=$1
  local port=$2
  local max_attempts=10
  local attempt=0

  echo -n "Waiting for $service to be healthy..."

  until [[ $(curl -s -o /dev/null -w "%{http_code}" "http://localhost:$port/actuator/health") -eq 200 ]]; do
    attempt=$((attempt + 1))
    if [[ $attempt -ge $max_attempts ]]; then
      echo -e "\n❌ $service failed to start"
      return 1
    fi
    echo -n "."
    sleep 5
  done
  echo -e "\n✅ $service is healthy"
}

# Perform health checks
for i in "${!services[@]}"; do
  service="${services[$i]}"
  port="${ports[$i]}"
  health_check "$service" "$port" || exit 1
done

echo -e "\nAll services started successfully!"
echo "Service logs:"
for service in "${services[@]}"; do
  echo "- $service: tail -f $(pwd)/$service.log"
done

echo -e "\nTo stop services: ./stop-review-system.sh"

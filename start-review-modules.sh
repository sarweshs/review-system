#!/bin/bash

# Build all services
echo "Building all services..."
mvn clean package -DskipTests

# Define service names and ports
# Prioritize review-service to be started and checked first
declare -A service_ports
service_ports["review-service"]="7070"
service_ports["review-dashboard"]="8081"
service_ports["review-consumer"]="7073"
service_ports["review-producer"]="7072"

# Define the order of service startup
startup_order=("review-service" "review-dashboard" "review-consumer" "review-producer")

# Function to check if port is in use and handle user consent
check_and_handle_port() {
  local port=$1
  local service_name=$2 # Added for better messaging

  if lsof -i :$port -sTCP:LISTEN -t >/dev/null; then
    echo "⚠️  Port $port (for $service_name) is already in use."
    read -p "Do you want to kill the process using port $port? [y/N]: " choice
    case "$choice" in
      y|Y )
        pid=$(lsof -ti tcp:$port)
        echo "Killing process $pid using port $port..."
        kill -9 "$pid"
        sleep 1  # Give the system a moment to release the port
        ;;
      * )
        echo "❌ Please stop the service using port $port or change the port for $service_name and try again."
        exit 1
        ;;
    esac
  fi
}

# Health check function
health_check() {
  local service=$1
  local port=$2
  local max_attempts=30 # Increased attempts for robustness
  local attempt=0
  local interval=2    # Check every 2 seconds

  echo -n "Waiting for $service to be healthy..."

  until [[ $(curl -s -o /dev/null -w "%{http_code}" "http://localhost:$port/actuator/health") -eq 200 ]]; do
    attempt=$((attempt + 1))
    if [[ $attempt -ge $max_attempts ]]; then
      echo -e "\n❌ $service failed to start after $max_attempts attempts."
      return 1
    fi
    echo -n "."
    sleep "$interval"
  done
  echo -e "\n✅ $service is healthy"
}

# --- Main Logic for Starting Services ---

# 1. Start review-service first
review_service_name="review-service"
review_service_port="${service_ports[$review_service_name]}"

echo "--- Starting $review_service_name ---"
check_and_handle_port "$review_service_port" "$review_service_name"

nohup java -jar "$review_service_name/target/$review_service_name-0.0.1-SNAPSHOT.jar" > "$review_service_name.log" 2>&1 &
echo $! > "$review_service_name.pid"
echo "Started $review_service_name (PID: $(cat $review_service_name.pid)) on port $review_service_port"

# 2. Perform health check for review-service
health_check "$review_service_name" "$review_service_port" || {
  echo "Critical: $review_service_name did not become healthy. Exiting."
  exit 1
}

echo -e "\n--- $review_service_name is healthy. Proceeding with other services. ---\n"

# 3. Start other services sequentially after review-service is healthy
for service in "${startup_order[@]}"; do
  if [[ "$service" == "$review_service_name" ]]; then
    continue # Skip review-service as it's already started and checked
  fi

  local_port="${service_ports[$service]}"
  echo "--- Starting $service ---"
  check_and_handle_port "$local_port" "$service"

  nohup java -jar "$service/target/$service-0.0.1-SNAPSHOT.jar" > "$service.log" 2>&1 &
  echo $! > "$service.pid"
  echo "Started $service (PID: $(cat $service.pid)) on port $local_port"

  # 4. Perform health check for each subsequent service before starting the next
  health_check "$service" "$local_port" || {
    echo "❌ $service did not become healthy. Stopping remaining services and exiting."
    # Optional: Add logic to stop already started services here if needed
    exit 1
  }
done

echo -e "\nAll services started successfully!"
echo "Service logs:"
for service in "${startup_order[@]}"; do
  echo "- $service: tail -f $(pwd)/$service.log"
done

echo -e "\nTo stop services: ./stop-review-system.sh"
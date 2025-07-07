#!/bin/bash

# Colors for better output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Build all services
echo -e "${BLUE}Building all services...${NC}"
mvn clean package -DskipTests

# Define service ports (compatible with older bash versions)
REVIEW_SERVICE_PORT="7070"
REVIEW_DASHBOARD_PORT="8081"
REVIEW_CONSUMER_PORT="7073"
REVIEW_PRODUCER_PORT="7072"

# Define the order of service startup
startup_order=("review-service" "review-dashboard" "review-consumer" "review-producer")

# Function to get port for a service
get_service_port() {
  local service=$1
  case $service in
    "review-service")
      echo $REVIEW_SERVICE_PORT
      ;;
    "review-dashboard")
      echo $REVIEW_DASHBOARD_PORT
      ;;
    "review-consumer")
      echo $REVIEW_CONSUMER_PORT
      ;;
    "review-producer")
      echo $REVIEW_PRODUCER_PORT
      ;;
    *)
      echo "0"
      ;;
  esac
}

# Function to check if port is in use and handle user consent
check_and_handle_port() {
  local port=$1
  local service_name=$2

  if lsof -i :$port -sTCP:LISTEN -t >/dev/null; then
    echo -e "${YELLOW}⚠️  Port $port (for $service_name) is already in use.${NC}"
    read -p "Do you want to kill the process using port $port? [y/N]: " choice
    case "$choice" in
      y|Y )
        pid=$(lsof -ti tcp:$port)
        echo -e "${YELLOW}Killing process $pid using port $port...${NC}"
        kill -9 "$pid"
        sleep 2  # Give the system more time to release the port
        ;;
      * )
        echo -e "${RED}❌ Please stop the service using port $port or change the port for $service_name and try again.${NC}"
        exit 1
        ;;
    esac
  fi
}

# Enhanced health check function
health_check() {
  local service=$1
  local port=$2
  local max_attempts=60  # Increased attempts for robustness
  local attempt=0
  local interval=3      # Check every 3 seconds

  echo -e "${BLUE}Waiting for $service to be healthy on port $port...${NC}"

  until [[ $(curl -s -o /dev/null -w "%{http_code}" "http://localhost:$port/actuator/health") -eq 200 ]]; do
    attempt=$((attempt + 1))
    if [[ $attempt -ge $max_attempts ]]; then
      echo -e "\n${RED}❌ $service failed to start after $max_attempts attempts (${max_attempts}s).${NC}"
      echo -e "${YELLOW}Last health check response:${NC}"
      curl -s "http://localhost:$port/actuator/health" || echo "Connection failed"
      return 1
    fi
    echo -n "."
    sleep "$interval"
  done
  echo -e "\n${GREEN}✅ $service is healthy${NC}"
  return 0
}

# Function to start a service
start_service() {
  local service=$1
  local port=$2
  
  echo -e "${BLUE}--- Starting $service on port $port ---${NC}"
  check_and_handle_port "$port" "$service"

  # Start the service
  mkdir -p ./logs

  nohup java -jar "$service/target/$service-0.0.1-SNAPSHOT.jar" > "./logs/$service.log" 2>&1 &
  local pid=$!
  echo $pid > "$service.pid"
  echo -e "${GREEN}Started $service (PID: $pid) on port $port${NC}"
  
  # Wait a moment for the service to begin starting
  sleep 3
}

# Function to stop all services
stop_all_services() {
  echo -e "${YELLOW}Stopping all services...${NC}"
  for service in "${startup_order[@]}"; do
    if [[ -f "$service.pid" ]]; then
      local pid=$(cat "$service.pid")
      if kill -0 "$pid" 2>/dev/null; then
        echo -e "${YELLOW}Stopping $service (PID: $pid)...${NC}"
        kill "$pid"
        sleep 2
        if kill -0 "$pid" 2>/dev/null; then
          echo -e "${YELLOW}Force killing $service (PID: $pid)...${NC}"
          kill -9 "$pid"
        fi
      fi
      rm -f "$service.pid"
    fi
  done
}

# Trap to stop services on script exit
trap stop_all_services EXIT

# --- Main Logic for Starting Services ---

echo -e "${BLUE}=== Starting Review System Modules ===${NC}"

# Debug: Show port assignments
echo -e "${YELLOW}Port assignments:${NC}"
for service in "${startup_order[@]}"; do
  local port=$(get_service_port "$service")
  echo -e "  ${service}: ${port}"
done
echo ""

# 1. Start review-service first and ensure it's healthy
review_service_name="review-service"
review_service_port=$(get_service_port "$review_service_name")

echo -e "${BLUE}Step 1: Starting $review_service_name (required to start first) on port $review_service_port${NC}"
start_service "$review_service_name" "$review_service_port"

# 2. Perform health check for review-service - CRITICAL STEP
echo -e "${BLUE}Step 2: Verifying $review_service_name health${NC}"
if ! health_check "$review_service_name" "$review_service_port"; then
  echo -e "${RED}Critical: $review_service_name did not become healthy. Stopping and exiting.${NC}"
  stop_all_services
  exit 1
fi

echo -e "${GREEN}--- $review_service_name is healthy. Proceeding with other services. ---${NC}\n"

# 3. Start other services sequentially after review-service is healthy
for service in "${startup_order[@]}"; do
  if [[ "$service" == "$review_service_name" ]]; then
    continue # Skip review-service as it's already started and checked
  fi

  local_port=$(get_service_port "$service")
  
  echo -e "${BLUE}Step 3: Starting $service${NC}"
  start_service "$service" "$local_port"

  # 4. Perform health check for each subsequent service
  echo -e "${BLUE}Step 4: Verifying $service health${NC}"
  if ! health_check "$service" "$local_port"; then
    echo -e "${RED}❌ $service did not become healthy. Stopping all services and exiting.${NC}"
    stop_all_services
    exit 1
  fi
  
  echo -e "${GREEN}✅ $service is ready${NC}\n"
done

echo -e "${GREEN}=== All services started successfully! ===${NC}"
echo -e "${BLUE}Service logs:${NC}"
for service in "${startup_order[@]}"; do
  echo -e "${YELLOW}- $service:${NC} tail -f $(pwd)/logs/$service.log"
done

echo -e "\n${BLUE}To stop services:${NC} ./stop-review-system.sh"
echo -e "${BLUE}To view logs:${NC} tail -f review-service.log review-consumer.log review-producer.log review-dashboard.log"

# Remove the trap since we want services to keep running
trap - EXIT
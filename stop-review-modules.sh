#!/bin/bash

# Colors for better output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}Stopping all review system services...${NC}"

declare -a services=("review-service" "review-dashboard" "review-consumer" "review-producer")

for service in "${services[@]}"; do
  if [ -f "$service.pid" ]; then
    pid=$(cat "$service.pid")
    if ps -p $pid > /dev/null; then
      echo -e "${YELLOW}Stopping $service (PID: $pid)...${NC}"
      
      # Try graceful shutdown first
      kill $pid
      
      # Wait up to 10 seconds for graceful shutdown
      for i in {1..10}; do
        if ! ps -p $pid > /dev/null; then
          echo -e "${GREEN}✅ $service stopped gracefully${NC}"
          break
        fi
        sleep 1
      done
      
      # Force kill if still running
      if ps -p $pid > /dev/null; then
        echo -e "${YELLOW}Force killing $service (PID: $pid)...${NC}"
        kill -9 $pid
        sleep 1
        if ! ps -p $pid > /dev/null; then
          echo -e "${GREEN}✅ $service force stopped${NC}"
        else
          echo -e "${RED}❌ Failed to stop $service (PID: $pid)${NC}"
        fi
      fi
      
      rm -f "$service.pid"
    else
      echo -e "${YELLOW}⚠️ $service not running (PID: $pid)${NC}"
      rm -f "$service.pid"
    fi
  else
    echo -e "${YELLOW}⚠️ No PID file found for $service${NC}"
  fi
done

echo -e "\n${BLUE}All services stopped. Log files preserved:${NC}"
if ls -1 *.log 2>/dev/null; then
  echo -e "${YELLOW}Available log files:${NC}"
  ls -1 *.log | sed 's/^/- /'
else
  echo -e "${YELLOW}No log files found${NC}"
fi

echo -e "\n${GREEN}Review system shutdown complete!${NC}"
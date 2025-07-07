#!/bin/bash

echo "Stopping all services..."

declare -a services=("review-service" "review-dashboard" "review-consumer" "review-producer")

for service in "${services[@]}"; do
  if [ -f "$service.pid" ]; then
    pid=$(cat "$service.pid")
    if ps -p $pid > /dev/null; then
      echo "Stopping $service (PID: $pid)..."
      kill $pid
      rm "$service.pid"
      echo "✅ $service stopped"
    else
      echo "⚠️ $service not running (PID: $pid)"
      rm "$service.pid"
    fi
  else
    echo "⚠️ No PID file found for $service"
  fi
done

echo -e "\nAll services stopped. Log files preserved:"
ls -1 *.log 2>/dev/null || echo "No log files found"
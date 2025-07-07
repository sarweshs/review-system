#!/bin/bash

echo "🔍 Testing Redis connection in Docker environment..."

# Check if Redis container is running
echo "📦 Checking Redis container status..."
if docker-compose ps redis | grep -q "Up"; then
    echo "✅ Redis container is running"
else
    echo "❌ Redis container is not running"
    echo "Starting Redis..."
    docker-compose up -d redis
    sleep 5
fi

# Test Redis connection
echo "🔗 Testing Redis connection..."
if docker-compose exec redis redis-cli ping | grep -q "PONG"; then
    echo "✅ Redis is responding correctly"
else
    echo "❌ Redis is not responding"
    echo "Redis logs:"
    docker-compose logs redis
    exit 1
fi

echo "✅ Redis connection test passed!" 
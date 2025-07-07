#!/bin/bash

echo "🔍 Testing Redis connectivity in Docker environment..."

# Test if Redis container is running
echo "1. Checking if Redis container is running..."
if docker ps | grep -q redis; then
    echo "✅ Redis container is running"
else
    echo "❌ Redis container is not running"
    exit 1
fi

# Test Redis connectivity from host
echo "2. Testing Redis connectivity from host..."
if redis-cli -h localhost -p 6379 ping > /dev/null 2>&1; then
    echo "✅ Redis is accessible from host"
else
    echo "❌ Redis is not accessible from host"
fi

# Test Redis connectivity from within Docker network
echo "3. Testing Redis connectivity from Docker network..."
if docker exec review-service redis-cli -h redis -p 6379 ping > /dev/null 2>&1; then
    echo "✅ Redis is accessible from Docker network"
else
    echo "❌ Redis is not accessible from Docker network"
fi

# Test Redis connectivity from review-service container
echo "4. Testing Redis connectivity from review-service container..."
if docker exec review-service curl -f http://localhost:7070/actuator/health > /dev/null 2>&1; then
    echo "✅ review-service is running and healthy"
else
    echo "❌ review-service is not running or not healthy"
fi

# Check review-service logs for Redis connection errors
echo "5. Checking review-service logs for Redis connection errors..."
if docker logs review-service 2>&1 | grep -q "Unable to connect to localhost"; then
    echo "❌ Found Redis connection errors in review-service logs"
    echo "   This indicates the application is still using localhost instead of redis"
else
    echo "✅ No Redis connection errors found in review-service logs"
fi

echo ""
echo "📋 Summary:"
echo "- If Redis is accessible from Docker network but review-service still fails,"
echo "  the issue is with Spring Boot configuration loading."
echo "- The application should use 'redis:6379' in Docker environment, not 'localhost:6379'."
echo "- Check that SPRING_PROFILES_ACTIVE=docker is set correctly." 
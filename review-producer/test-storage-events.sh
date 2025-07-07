#!/bin/bash

# Test script for storage event endpoints
# Make sure the Review Producer is running on localhost:7070

BASE_URL="http://localhost:7070/api/producer"
API_KEY="default-webhook-key"

echo "Testing Storage Event Endpoints"
echo "================================"
echo "Using API Key: $API_KEY"
echo ""

# Test 1: MinIO Event Endpoint
echo "1. Testing MinIO Event Endpoint..."
curl -X POST "$BASE_URL/storage/event/minio" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: $API_KEY" \
  -d @examples/minio-event-example.json \
  -w "\nHTTP Status: %{http_code}\n" \
  -s

echo -e "\n"

# Test 2: Generic Storage Event Endpoint
echo "2. Testing Generic Storage Event Endpoint..."
curl -X POST "$BASE_URL/storage/event" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: $API_KEY" \
  -d @examples/storage-event-example.json \
  -w "\nHTTP Status: %{http_code}\n" \
  -s

echo -e "\n"

# Test 3: Raw Storage Event Endpoint
echo "3. Testing Raw Storage Event Endpoint..."
curl -X POST "$BASE_URL/storage/event/raw" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: $API_KEY" \
  -d '"{\"provider\":\"minio\",\"eventType\":\"ObjectCreated:Put\",\"bucket\":\"test-bucket\",\"key\":\"test.jl\"}"' \
  -w "\nHTTP Status: %{http_code}\n" \
  -s

echo -e "\n"

# Test 4: Health Check (No auth required)
echo "4. Testing Health Check (No auth required)..."
curl -X GET "$BASE_URL/health" \
  -w "\nHTTP Status: %{http_code}\n" \
  -s

echo -e "\n"

# Test 5: Authentication Failure Test
echo "5. Testing Authentication Failure..."
curl -X POST "$BASE_URL/storage/event/minio" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: invalid-key" \
  -d @examples/minio-event-example.json \
  -w "\nHTTP Status: %{http_code}\n" \
  -s

echo -e "\n"

# Test 6: Missing API Key Test
echo "6. Testing Missing API Key..."
curl -X POST "$BASE_URL/storage/event/minio" \
  -H "Content-Type: application/json" \
  -d @examples/minio-event-example.json \
  -w "\nHTTP Status: %{http_code}\n" \
  -s

echo -e "\n"
echo "Storage Event Endpoint Tests Completed!" 
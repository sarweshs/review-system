#!/bin/bash

echo "🔍 Testing Keycloak connectivity..."

# Test if Keycloak container is running
echo "1. Checking if Keycloak container is running..."
if docker ps | grep -q keycloak; then
    echo "✅ Keycloak container is running"
else
    echo "❌ Keycloak container is not running"
    exit 1
fi

# Test Keycloak health endpoint
echo "2. Testing Keycloak health endpoint..."
if wget --no-verbose --tries=1 --spider http://localhost:8080/ > /dev/null 2>&1; then
    echo "✅ Keycloak health endpoint is accessible"
else
    echo "❌ Keycloak health endpoint is not accessible"
    echo "   This might be normal if Keycloak is still starting up..."
fi

# Test Keycloak realm endpoint
echo "3. Testing Keycloak realm endpoint..."
if wget --no-verbose --tries=1 --spider http://localhost:8080/realms/review-realm/.well-known/openid-configuration > /dev/null 2>&1; then
    echo "✅ Keycloak realm endpoint is accessible"
else
    echo "❌ Keycloak realm endpoint is not accessible"
    echo "   This might be normal if the realm hasn't been created yet..."
fi

# Test from within Docker network
echo "4. Testing Keycloak from Docker network..."
if docker exec review-dashboard wget --no-verbose --tries=1 --spider http://keycloak:8080/ > /dev/null 2>&1; then
    echo "✅ Keycloak is accessible from Docker network"
else
    echo "❌ Keycloak is not accessible from Docker network"
fi

echo ""
echo "📋 Summary:"
echo "- If Keycloak container is running but endpoints are not accessible,"
echo "  it might still be starting up. Wait a few minutes and try again."
echo "- If the realm endpoint is not accessible, you may need to create"
echo "  the 'review-realm' in Keycloak admin console."
echo "- The review-dashboard will wait for Keycloak to be healthy before starting." 
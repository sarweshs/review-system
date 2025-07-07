#!/bin/bash

# Start Keycloak with realm import in silent mode
start_keycloak() {
    echo "Starting Keycloak with realm import..."
    docker run -d --name keycloak \
      -p 8080:8080 \
      -v $(pwd)/config_files:/opt/keycloak/data/import \
      -e KEYCLOAK_ADMIN=admin \
      -e KEYCLOAK_ADMIN_PASSWORD=admin \
      quay.io/keycloak/keycloak:24.0.0 \
      start-dev --import-realm >/dev/null 2>&1

   # Wait for Keycloak to start using docker ps check
   echo -n "Waiting for Keycloak to start"
   while true; do
       container_status=$(docker ps -f name=keycloak --format "{{.Status}}")

       if [[ $container_status == Up* ]]; then
           echo -e "\n✅ Keycloak container is running"
           break
       elif [[ $container_status == "" ]]; then
           echo -e "\n❌ Keycloak container not found"
           exit 1
       else
           echo -n "."
           sleep 2
       fi
   done
}

# Verify all common services are healthy (excluding Keycloak)
verify_common_services() {
    echo "Verifying common services are healthy..."

    # Check Postgres
    if ! docker-compose -f docker-compose-common.yml exec postgres pg_isready -U postgres; then
        echo "❌ Postgres is not ready"
        return 1
    fi

    # Check Redis
    if ! docker-compose -f docker-compose-common.yml exec redis redis-cli ping | grep -q "PONG"; then
        echo "❌ Redis is not ready"
        return 1
    fi

    # Check Kafka
    if ! docker-compose -f docker-compose-common.yml exec kafka kafka-topics.sh --bootstrap-server localhost:9092 --list >/dev/null 2>&1; then
        echo "❌ Kafka is not ready"
        return 1
    fi

    # Check Vault
    if ! curl -s http://localhost:8200/v1/sys/health | jq -e '.initialized' >/dev/null 2>&1; then
        echo "❌ Vault is not ready"
        return 1
    fi

    echo "✅ All common services are healthy"
    return 0
}

# Verify Keycloak has review-realm configured
verify_keycloak_realm() {
    echo "Checking if Keycloak has review-realm configured..."

    # Retry token fetch with timeout
    local RETRY=0
    local MAX_RETRIES=5
    local TOKEN=""

    while [ $RETRY -lt $MAX_RETRIES ]; do
        TOKEN=$(curl -s -X POST \
            -H "Content-Type: application/x-www-form-urlencoded" \
            -d "username=admin&password=admin&grant_type=password&client_id=admin-cli" \
            http://localhost:8080/realms/master/protocol/openid-connect/token | jq -r '.access_token')

        if [ -n "$TOKEN" ]; then
            break
        fi

        echo "⚠️ Retrying token fetch... ($((RETRY+1))/$MAX_RETRIES)"
        sleep 5
        ((RETRY++))
    done

    if [ -z "$TOKEN" ]; then
        echo "❌ Failed to get admin token from Keycloak after $MAX_RETRIES attempts"
        echo "Possible causes:"
        echo "1. Keycloak isn't fully initialized yet"
        echo "2. Incorrect admin credentials"
        echo "3. Keycloak not running on port 8080"
        echo "4. Network issues"

        # Debugging help
        echo -e "\nDebug info:"
        docker logs keycloak | tail -n 20
        return 1
    fi

    # Check if realm exists with timeout
    RETRY=0
    while [ $RETRY -lt $MAX_RETRIES ]; do
        if curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/admin/realms | jq -e '.[] | select(.realm == "review-realm")' >/dev/null 2>&1; then
            echo "✅ review-realm exists in Keycloak"
            return 0
        fi

        echo "⚠️ Realm not found, retrying... ($((RETRY+1))/$MAX_RETRIES)"
        sleep 5
        ((RETRY++))
    done

    echo "❌ review-realm not found in Keycloak after $MAX_RETRIES attempts"
    echo "Possible solutions:"
    echo "1. Check your realm.json file exists in config_files/"
    echo "2. Verify the file has correct JSON formatting"
    echo "3. Check Keycloak import logs: docker logs keycloak"

    # Show import directory contents
    echo -e "\nContents of config_files/:"
    ls -la config_files/
    return 1
}

# Cleanup function
cleanup() {
    echo "Cleaning up..."
    docker stop keycloak >/dev/null 2>&1
    docker rm keycloak >/dev/null 2>&1
}

# Main execution
main() {
    # Set trap for cleanup
    trap cleanup EXIT

    # Verify common services (excluding Keycloak)
    if ! verify_common_services; then
        echo "Some common services are not ready. Please check and try again."
        exit 1
    fi

    # Start Keycloak
    start_keycloak

    # Verify Keycloak realm
    if ! verify_keycloak_realm; then
        echo "Keycloak is missing the required realm. Please check your realm.json file."
        exit 1
    fi

    # Start review system services
    echo "Starting review system services..."
    docker-compose -f docker-compose-review-system.yml up -d

    echo "✅ Review system services started successfully!"
    echo "Keycloak is running at http://localhost:8080"
    echo "Press Ctrl+C to stop Keycloak when done"

    # Keep the script running until Ctrl+C
    while true; do sleep 1; done
}

main
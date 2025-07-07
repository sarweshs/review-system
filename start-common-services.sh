#!/bin/bash

# Start common services and wait for them
start_common_services() {
    echo "🚀 Starting common services..."
    docker-compose -f docker-compose-common.yml up -d

    echo -n "⏳ Waiting for services"
    for service in postgres redis kafka vault; do
        while ! check_service_health $service; do
            echo -n "."
            sleep 2
        done
    done
    echo -e "\n✅ All common services ready"
}

check_service_health() {
    case $1 in
        postgres)
            docker-compose -f docker-compose-common.yml exec postgres pg_isready -U postgres &>/dev/null
            ;;
        redis)
            docker-compose -f docker-compose-common.yml exec redis redis-cli ping | grep -q "PONG"
            ;;
        kafka)
            docker-compose -f docker-compose-common.yml exec kafka \
              kafka-topics.sh --bootstrap-server localhost:9092 --list &>/dev/null
            ;;
        vault)
            if curl -s http://localhost:8200/v1/sys/health | jq -e '.initialized' &>/dev/null; then
                    ./init_vault_aes_key.sh
                    true  # Return success for service health
                else
                    false  # Vault is not initialized yet
                fi
            ;;
    esac
}

start_keycloak() {
    echo "🔐 Starting Keycloak..."
    if docker inspect keycloak &>/dev/null; then
        echo "ℹ️ Keycloak container already exists, starting it..."
        docker start keycloak
    else
        docker run -d --name keycloak \
          --network review-network \
          -p 8080:8080 \
          -v $(pwd)/config_files:/opt/keycloak/data/import \
          -e KEYCLOAK_ADMIN=admin \
          -e KEYCLOAK_ADMIN_PASSWORD=admin \
          quay.io/keycloak/keycloak:24.0.0 \
          start-dev --import-realm
    fi

    echo -n "⏳ Waiting for Keycloak"
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
    echo -e "\n✅ Keycloak ready at http://localhost:8080"
}

verify_realm() {
    echo "🔍 Checking for review-realm..."
    local attempts=0
    local max_attempts=5

    while [ $attempts -lt $max_attempts ]; do
        if check_realm_exists; then
            echo "✅ review-realm found"
            return 0
        fi
        echo "⚠️ Attempt $((attempts+1))/$max_attempts - retrying..."
        sleep 5
        ((attempts++))
    done

    echo "❌ Failed to find review-realm after $max_attempts attempts"
    return 1
}

check_realm_exists() {
    local token=$(curl -s -X POST \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "username=admin&password=admin&grant_type=password&client_id=admin-cli" \
        http://localhost:8080/realms/master/protocol/openid-connect/token | jq -r '.access_token')

    [ -z "$token" ] && return 1

    curl -s -H "Authorization: Bearer $token" \
        http://localhost:8080/admin/realms/review-realm | jq -e '.realm == "review-realm"' &>/dev/null
}

start_review_services() {
    echo "🚀 Starting review services..."
    docker-compose -f docker-compose-review-system.yml up -d
    echo "✅ Review services started"
}

show_success() {
    cat <<EOF

🎉 Deployment successful!

Access URLs:
- Keycloak Admin: http://localhost:8080/admin
- Review Dashboard: http://localhost:8081

Management commands:
- Stop services: ./stop-services.sh
- View logs: docker-compose logs -f

EOF
}

main() {
    start_common_services
    start_keycloak
    verify_realm || exit 1
    #start_review_services - Review service not working on docker need to be fixed
    show_success
}

main
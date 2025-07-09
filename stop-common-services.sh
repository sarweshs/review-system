#!/bin/bash

# Default docker-compose file
DOCKER_COMPOSE_FILE="docker-compose-common.yml"

# Parse command line arguments
if [ $# -eq 1 ]; then
    if [ "$1" = "clickstack" ]; then
        DOCKER_COMPOSE_FILE="docker-compose-clickstack.yml"
        echo "🔧 Using clickstack configuration: $DOCKER_COMPOSE_FILE"
    else
        echo "⚠️ Unknown argument: $1"
        echo "Usage: $0 [clickstack]"
        echo "  - No argument: Use docker-compose-common.yml (default)"
        echo "  - clickstack: Use docker-compose-clickstack.yml"
        exit 1
    fi
else
    echo "🔧 Using default configuration: $DOCKER_COMPOSE_FILE"
fi

echo "Stopping all services..."

# Stop Keycloak
docker stop keycloak &>/dev/null && echo "✅ Stopped Keycloak"

# Stop review system
#docker-compose -f docker-compose-review-system.yml down &>/dev/null && echo "✅ Stopped review system"

# Stop common services
docker-compose -f $DOCKER_COMPOSE_FILE down &>/dev/null && echo "✅ Stopped common services"

echo -e "\nAll services stopped."
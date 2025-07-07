#!/bin/bash

echo "Stopping all services..."

# Stop Keycloak
docker stop keycloak &>/dev/null && echo "✅ Stopped Keycloak"

# Stop review system
#docker-compose -f docker-compose-review-system.yml down &>/dev/null && echo "✅ Stopped review system"

# Stop common services
docker-compose -f docker-compose-common.yml down &>/dev/null && echo "✅ Stopped common services"

echo -e "\nAll services stopped."
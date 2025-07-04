#!/bin/bash

set -e

# CONFIGURATION
REALM_NAME="review-realm"
EXPORT_DIR="$(pwd)/export"
KEYCLOAK_CONTAINER="review-system-keycloak-1"
IN_CONTAINER_EXPORT_PATH="/opt/keycloak/data/export"

# Create export dir if not exists
mkdir -p "$EXPORT_DIR"

echo "🔍 Checking if Keycloak container '$KEYCLOAK_CONTAINER' is running..."

if ! docker ps --format '{{.Names}}' | grep -q "^$KEYCLOAK_CONTAINER$"; then
  echo "❌ Container '$KEYCLOAK_CONTAINER' is not running. Please start it with docker-compose."
  exit 1
fi

echo "✅ Container is running. Running export command inside it..."
docker exec -u 0 "$KEYCLOAK_CONTAINER" /opt/keycloak/bin/kc.sh export \
  --dir "$IN_CONTAINER_EXPORT_PATH" \
  --realm "$REALM_NAME" \
  --users realm_file

# Copy export file from container to host
echo "📦 Copying exported realm to host..."
docker cp "$KEYCLOAK_CONTAINER:$IN_CONTAINER_EXPORT_PATH" "$EXPORT_DIR"

echo "✅ Realm '$REALM_NAME' exported to: $EXPORT_DIR/export/$REALM_NAME-realm.json"
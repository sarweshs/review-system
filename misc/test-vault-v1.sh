#!/bin/bash

# === Vault Config (from env vars or default values) ===
VAULT_HOST="${vault_host:-localhost}"
VAULT_PORT="${vault_port:-8200}"
VAULT_TOKEN="${vault_token:-devroot}"
VAULT_SCHEME="${vault_scheme:-http}"
VAULT_SECRET_PATH="${vault_secret_path:-/v1/secret/data/aes-key}"

# === Construct Vault URL ===
VAULT_URL="${VAULT_SCHEME}://${VAULT_HOST}:${VAULT_PORT}${VAULT_SECRET_PATH}"

echo "🔐 Testing Vault at: $VAULT_URL"
echo "➡️  Using token: ${VAULT_TOKEN:0:4}******"

# === Make request ===
response=$(curl -s -w "\n%{http_code}" -H "X-Vault-Token: $VAULT_TOKEN" "$VAULT_URL")

# === Split response and status ===
http_body=$(echo "$response" | sed '$d')
http_code=$(echo "$response" | tail -n1)

# === Validate ===
if [ "$http_code" -eq 200 ]; then
    echo "✅ Vault secret read succeeded!"
    echo "$http_body" | jq .
    exit 0
else
    echo "❌ Vault access failed with status $http_code"
    echo "Response:"
    echo "$http_body"
    exit 1
fi

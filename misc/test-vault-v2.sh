#!/bin/bash

# === Config ===
VAULT_HOST="${vault_host:-localhost}"
VAULT_PORT="${vault_port:-8200}"
VAULT_TOKEN="${vault_token:-devroot}"
VAULT_SCHEME="${vault_scheme:-http}"
VAULT_SECRET_PATH="${vault_secret_path:-aes-key}"  # logical path only

# === URL for KV v2 ===
VAULT_URL="${VAULT_SCHEME}://${VAULT_HOST}:${VAULT_PORT}/v1/secret/data/${VAULT_SECRET_PATH}"

echo "🔐 Testing Vault at: $VAULT_URL"
echo "➡️  Using token: ${VAULT_TOKEN:0:4}******"

response=$(curl -s -w "\n%{http_code}" -H "X-Vault-Token: $VAULT_TOKEN" "$VAULT_URL")

body=$(echo "$response" | sed '$d')
code=$(echo "$response" | tail -n1)

if [[ "$code" == "200" ]]; then
  echo "✅ Vault secret read succeeded!"
  echo "$body" | jq .
  exit 0
else
  echo "❌ Vault access failed with status $code"
  echo "Response: $body"
  echo "Hint: If you're on KV v1, update the script to use /v1/secret/<name> instead of /v1/secret/data/<name>"
  exit 1
fi

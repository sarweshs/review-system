#!/bin/bash

# Configuration
export VAULT_ADDR='http://localhost:8200'
export VAULT_TOKEN='devroot'  # Dev mode token
ENV_FILE=".env"

# Validate Vault connection
if ! vault status > /dev/null 2>&1; then
  echo "ERROR: Could not connect to Vault at $VAULT_ADDR"
  echo "Make sure Vault is running in dev mode: docker-compose up vault -d"
  exit 1
fi

# Verify Vault is unsealed (should be auto-unsealed in dev mode)
SEALED=$(vault status -format=json | jq -r '.sealed')
if [ "$SEALED" = "true" ]; then
  echo "ERROR: Vault is sealed! In dev mode, Vault should auto-unseal."
  echo "Check if Vault is running properly: docker-compose logs vault"
  exit 1
fi

# === KV v2 secret path (correct usage via CLI) ===
# Do NOT include `/data/` in CLI path for KV v2
SECRET_PATH="secret/aes-key"
echo "=== Secret Path ==="
echo "$SECRET_PATH"

# Check if key already exists
if vault kv get "$SECRET_PATH" > /dev/null 2>&1; then
  echo "AES key already exists in Vault"
  echo "Current key value:"
  vault kv get "$SECRET_PATH" | grep -E "^value\s*:" | sed 's/^value\s*:\s*//'
  exit 0
fi

# Step 1: Use AES_KEY from environment if set
if [ -n "${AES_KEY}" ]; then
  echo "Using AES key from environment variable AES_KEY"
  KEY="${AES_KEY}"

# Step 2: If not set in env, check .env file
elif grep -q '^AES_KEY=' "$ENV_FILE"; then
  echo "Loading AES_KEY from .env file"
  KEY=$(grep '^AES_KEY=' "$ENV_FILE" | cut -d '=' -f2-)
  export AES_KEY="${KEY}"

# Step 3: If not in .env, generate and append
else
  echo "AES_KEY not set. Generating and saving to .env"
  KEY=$(openssl rand -hex 16 | tr -d '\n')
  echo "AES_KEY=${KEY}" >> "$ENV_FILE"
  export AES_KEY="${KEY}"
fi

echo "AES key to be stored: $KEY"

# Write to Vault
if ! vault kv put "$SECRET_PATH" value="$KEY" > /dev/null; then
  echo "ERROR: Failed to write AES key to Vault"
  echo "Possible causes:"
  echo "1. Invalid permissions for token"
  echo "2. KV v2 secret engine not enabled at 'secret/'"
  exit 1
fi

# Verify write was successful
echo "======= Metadata ======="
if ! OUTPUT=$(vault kv get "$SECRET_PATH"); then
  echo "ERROR: Failed to read AES key from Vault"
  exit 1
fi

echo "$OUTPUT"

# Security recommendations
echo
echo "!!! SECURITY NOTICE !!!"
echo "1. Store the AES key securely outside this script"
echo "2. In production, use a non-root token with limited permissions"
echo "3. Consider using Vault's transit engine for key generation"
echo "4. Rotate this key periodically using 'vault kv patch'"

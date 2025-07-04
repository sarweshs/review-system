#!/bin/bash

# Configuration
export VAULT_ADDR='http://localhost:8200'
export VAULT_TOKEN='devroot'  # Dev mode token

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

# Check if AES key already exists
echo "=== Secret Path ==="
SECRET_PATH="secret/data/aes-key"
echo "$SECRET_PATH"

# Check if key already exists
if vault kv get "$SECRET_PATH" > /dev/null 2>&1; then
  echo "AES key already exists in Vault"
  echo "Current key value:"
  vault kv get "$SECRET_PATH" | grep -E "^value\s*:" | sed 's/^value\s*:\s*//'
  exit 0
fi

# Check if AES_KEY environment variable is set
if [ -n "${AES_KEY}" ]; then
  echo "Using AES key from environment variable AES_KEY"
  KEY="${AES_KEY}"
else
  echo "AES_KEY environment variable not set, generating new key"
  KEY=$(openssl rand -hex 16 | tr -d '\n')
fi

echo "AES key to be stored: $KEY"

# Write to Vault with validation
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
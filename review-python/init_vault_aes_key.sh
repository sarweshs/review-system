#!/bin/bash

export VAULT_ADDR=http://localhost:8200
export VAULT_TOKEN=root

# Check if the key already exists
echo "=== Secret Path ==="
echo "secret/data/aes-key"

# Generate and store new AES-128 key (hex)
KEY=$(openssl rand -hex 16)

vault kv put secret/aes-key value=$KEY

echo "======= Metadata ======="
vault kv get secret/aes-key

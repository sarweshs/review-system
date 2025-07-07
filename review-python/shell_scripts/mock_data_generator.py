import os
import json
import requests
import base64
import logging
import traceback
from sqlalchemy import create_engine, text
from sqlalchemy.orm import sessionmaker
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.backends import default_backend

# Logging setup
LOG_DIR = "logs"
LOG_FILE = os.path.join(LOG_DIR, "mock_data_generator.log")
os.makedirs(LOG_DIR, exist_ok=True)
logging.basicConfig(
    level=logging.INFO,
    format="[%(asctime)s] %(levelname)s - %(message)s",
    handlers=[
        logging.FileHandler(LOG_FILE),
        logging.StreamHandler()
    ]
)

# Config
DB_URL = os.getenv("PYTHON_DTBSE_URL", "postgresql://postgres:postgres@localhost:5432/reviews")
VAULT_ADDR = os.getenv("VAULT_ADDR", "http://localhost:8200")
VAULT_TOKEN = os.getenv("VAULT_TOKEN", "devroot")  # Dev mode token
VAULT_SECRET_PATH = os.getenv("VAULT_SECRET_PATH", "/v1/secret/data/aes-key")

# Fetch AES key from Vault
def fetch_aes_key_from_vault():
    url = VAULT_ADDR + VAULT_SECRET_PATH
    headers = {"X-Vault-Token": VAULT_TOKEN}
    resp = requests.get(url, headers=headers)
    resp.raise_for_status()
    data = resp.json()
    hex_key = data["data"]["data"]["value"]
    return bytes.fromhex(hex_key)

# AES decryption (AES-128/192/256 ECB PKCS7)
def decrypt_aes_ecb(ciphertext_b64, key_bytes):
    ciphertext = base64.b64decode(ciphertext_b64)
    cipher = Cipher(algorithms.AES(key_bytes), modes.ECB(), backend=default_backend())
    decryptor = cipher.decryptor()
    padded = decryptor.update(ciphertext) + decryptor.finalize()
    # Remove PKCS7 padding
    pad_len = padded[-1]
    return padded[:-pad_len].decode()

# Fetch active review sources and decrypt credentials
def fetch_and_decrypt_sources():
    try:
        engine = create_engine(DB_URL)
        Session = sessionmaker(bind=engine)
        session = Session()
        key_bytes = fetch_aes_key_from_vault()
        logging.info(f"Fetched AES key from Vault: {key_bytes.hex()}")
        rows = session.execute(text("SELECT id, name, uri, credential_json FROM review_sources WHERE active = true")).fetchall()
        for row in rows:
            logging.info(f"Source: id={row.id}, name={row.name}, uri={row.uri}")
            if row.credential_json:
                try:
                    decrypted = decrypt_aes_ecb(row.credential_json, key_bytes)
                    logging.info(f"  Decrypted credential: {decrypted}")
                except Exception as e:
                    logging.error(f"  Failed to decrypt credential for source id={row.id}: {e}")
                    logging.error(traceback.format_exc())
            else:
                logging.info("  No credential.")
    except Exception as e:
        logging.error(f"Fatal error in fetch_and_decrypt_sources: {e}")
        logging.error(traceback.format_exc())
        exit(1)

if __name__ == "__main__":
    try:
        fetch_and_decrypt_sources()
    except Exception as e:
        logging.error(f"Unhandled error: {e}")
        logging.error(traceback.format_exc())
        exit(1) 
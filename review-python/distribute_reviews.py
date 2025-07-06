import os
import json
import requests
import base64
import logging
import traceback
import boto3
from botocore.exceptions import ClientError
from sqlalchemy import create_engine, text
from sqlalchemy.orm import sessionmaker
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.backends import default_backend
import glob
import random

# Platform mapping based on filename prefixes
PLATFORM_MAPPING = {
    "agoda_": "agoda",
    "bookingcom_": "bookingcom", 
    "expedia_": "expedia"
}

def get_platform_from_filename(filename):
    """Extract platform from filename prefix"""
    filename_lower = filename.lower()
    
    for prefix, platform in PLATFORM_MAPPING.items():
        if filename_lower.startswith(prefix):
            return platform
    
    return "other"

# Logging setup
LOG_DIR = "logs"
LOG_FILE = os.path.join(LOG_DIR, "distribute_reviews.log")
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

# Upload to AWS S3
def upload_to_s3(file_path, bucket_name, object_key, access_key, secret_key, endpoint_url=None):
    try:
        s3_client = boto3.client(
            's3',
            aws_access_key_id=access_key,
            aws_secret_access_key=secret_key,
            endpoint_url=endpoint_url
        )
        
        with open(file_path, 'rb') as file:
            s3_client.upload_fileobj(file, bucket_name, object_key)
        
        logging.info(f"Successfully uploaded {file_path} to S3: {bucket_name}/{object_key}")
        return True
    except ClientError as e:
        logging.error(f"Failed to upload to S3: {e}")
        return False

# Upload to MinIO (using S3 client with MinIO endpoint)
def upload_to_minio(file_path, bucket_name, object_key, username, password, endpoint_url):
    try:
        s3_client = boto3.client(
            's3',
            aws_access_key_id=username,
            aws_secret_access_key=password,
            endpoint_url=endpoint_url
        )
        
        # Create bucket if it doesn't exist
        try:
            s3_client.head_bucket(Bucket=bucket_name)
        except ClientError:
            s3_client.create_bucket(Bucket=bucket_name)
            logging.info(f"Created bucket: {bucket_name}")
        
        with open(file_path, 'rb') as file:
            s3_client.upload_fileobj(file, bucket_name, object_key)
        
        logging.info(f"Successfully uploaded {file_path} to MinIO: {bucket_name}/{object_key}")
        return True
    except ClientError as e:
        logging.error(f"Failed to upload to MinIO: {e}")
        return False

# Get review files to distribute
def get_review_files():
    review_dir = "generated_reviews"
    processed_dir = os.path.join(review_dir, "processed")
    
    # Create processed directory if it doesn't exist
    os.makedirs(processed_dir, exist_ok=True)
    
    if not os.path.exists(review_dir):
        logging.error(f"Review directory {review_dir} does not exist")
        return []
    
    # Get files from generated_reviews, excluding any that might be in processed
    files = glob.glob(os.path.join(review_dir, "*.jl"))
    logging.info(f"Found {len(files)} review files to distribute")
    return files

# Move file to processed folder
def move_to_processed(file_path):
    processed_dir = os.path.join("generated_reviews", "processed")
    file_name = os.path.basename(file_path)
    processed_path = os.path.join(processed_dir, file_name)
    
    try:
        os.rename(file_path, processed_path)
        logging.info(f"Moved {file_name} to processed folder")
        return True
    except Exception as e:
        logging.error(f"Failed to move {file_name} to processed folder: {e}")
        return False

# Distribute review files to storage destinations
def distribute_reviews():
    try:
        # Get AES key
        key_bytes = fetch_aes_key_from_vault()
        logging.info(f"Fetched AES key from Vault: {key_bytes.hex()}")
        
        # Get active sources from database
        engine = create_engine(DB_URL)
        Session = sessionmaker(bind=engine)
        session = Session()
        
        rows = session.execute(text("SELECT id, name, uri, credential_json FROM review_sources WHERE active = true")).fetchall()
        
        if not rows:
            logging.error("No active sources found")
            return
        
        # Decrypt credentials for each source
        sources = []
        for row in rows:
            if row.credential_json:
                try:
                    decrypted = decrypt_aes_ecb(row.credential_json, key_bytes)
                    creds = json.loads(decrypted)
                    sources.append({
                        'id': row.id,
                        'name': row.name,
                        'uri': row.uri,
                        'credentials': creds
                    })
                    logging.info(f"Source {row.name}: {creds['type']} credentials decrypted")
                except Exception as e:
                    logging.error(f"Failed to decrypt credentials for source {row.name}: {e}")
            else:
                logging.warning(f"Source {row.name} has no credentials")
        
        if not sources:
            logging.error("No sources with valid credentials found")
            return
        
        # Get review files
        review_files = get_review_files()
        if not review_files:
            logging.error("No review files found to distribute")
            return
        
        # Distribute files alternately
        logging.info(f"Distributing {len(review_files)} files to {len(sources)} sources")
        
        for i, file_path in enumerate(review_files):
            # Alternate between sources
            source = sources[i % len(sources)]
            file_name = os.path.basename(file_path)
            
            logging.info(f"Distributing {file_name} to {source['name']}")
            
            if source['credentials']['type'] == 'aws':
                # Upload to AWS S3/Storj
                access_key = source['credentials']['accessKeyId']
                secret_key = source['credentials']['secretAccessKey']
                
                # For Storj, use the endpoint and default bucket
                endpoint_url = "https://gateway.storjshare.io"
                bucket_name = "review-data"  # Default bucket name
                platform = get_platform_from_filename(file_name)
                object_key = f"{platform}/{file_name}"
                
                success = upload_to_s3(file_path, bucket_name, object_key, access_key, secret_key, endpoint_url)
                if success:
                    logging.info(f"✅ Successfully distributed {file_name} to AWS S3 via {source['name']}")
                    # Move file to processed folder after successful upload
                    move_to_processed(file_path)
                else:
                    logging.error(f"❌ Failed to distribute {file_name} to AWS S3 via {source['name']}")
                    
            elif source['credentials']['type'] == 'basic':
                # Upload to MinIO
                username = source['credentials']['username']
                password = source['credentials']['password']
                
                # Extract bucket and path from URI
                uri_parts = source['uri'].replace('http://', '').split('/')
                endpoint_url = f"http://{uri_parts[0]}"
                bucket_name = "review-data"  # Default bucket name
                platform = get_platform_from_filename(file_name)
                object_key = f"{platform}/{file_name}"
                
                success = upload_to_minio(file_path, bucket_name, object_key, username, password, endpoint_url)
                if success:
                    logging.info(f"✅ Successfully distributed {file_name} to MinIO via {source['name']}")
                    # Move file to processed folder after successful upload
                    move_to_processed(file_path)
                else:
                    logging.error(f"❌ Failed to distribute {file_name} to MinIO via {source['name']}")
            else:
                logging.warning(f"Unsupported credential type: {source['credentials']['type']}")
        
        session.close()
        logging.info("Distribution completed!")
        
    except Exception as e:
        logging.error(f"Fatal error in distribute_reviews: {e}")
        logging.error(traceback.format_exc())
        exit(1)

if __name__ == "__main__":
    try:
        distribute_reviews()
    except Exception as e:
        logging.error(f"Unhandled error: {e}")
        logging.error(traceback.format_exc())
        exit(1) 
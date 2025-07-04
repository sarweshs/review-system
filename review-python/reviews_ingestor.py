import json
import logging
import psycopg2
from kafka import KafkaProducer
from pathlib import Path

logging.basicConfig(level=logging.INFO, format="[%(asctime)s] %(levelname)s - %(message)s")
FAILED_LOG = "failed_reviews.log"

DB_CONFIG = {
    "dbname": "reviews",
    "user": "user",
    "password": "pass",
    "host": "localhost",
    "port": 5432
}

KAFKA_TOPIC = "hotel_reviews"
KAFKA_BOOTSTRAP_SERVERS = ["localhost:9092"]

REQUIRED_FIELDS = ["hotelId", "comment.hotelReviewId", "comment.providerId"]

def get_active_sources():
    conn = psycopg2.connect(**DB_CONFIG)
    cursor = conn.cursor()
    cursor.execute("SELECT source_id, file_path FROM review_sources WHERE active = TRUE")
    sources = cursor.fetchall()
    cursor.close()
    conn.close()
    return sources

def is_valid(review):
    try:
        if not isinstance(review.get("hotelId"), int):
            return False, "Missing or invalid hotelId"
        if not isinstance(review["comment"]["hotelReviewId"], int):
            return False, "Missing or invalid comment.hotelReviewId"
        if not isinstance(review["comment"]["providerId"], int):
            return False, "Missing or invalid comment.providerId"
    except KeyError as e:
        return False, f"Missing key: {str(e)}"
    return True, ""

def log_invalid(line, reason):
    with open(FAILED_LOG, "a") as f:
        f.write(f"{reason} => {line.strip()}\n")

def push_reviews_to_kafka(source_id, file_path):
    producer = KafkaProducer(
        bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS,
        value_serializer=lambda v: json.dumps(v).encode("utf-8")
    )

    success, failure = 0, 0
    for line in Path(file_path).read_text().splitlines():
        try:
            review = json.loads(line)
            valid, reason = is_valid(review)
            if valid:
                producer.send(KAFKA_TOPIC, review)
                success += 1
            else:
                log_invalid(line, reason)
                failure += 1
        except Exception as e:
            log_invalid(line, f"ParseError: {e}")
            failure += 1

    producer.flush()
    logging.info(f"[Source ID: {source_id}] Success: {success}, Failed: {failure}")

if __name__ == "__main__":
    sources = get_active_sources()
    for source_id, file_path in sources:
        if Path(file_path).exists():
            push_reviews_to_kafka(source_id, file_path)
        else:
            logging.warning(f"File not found: {file_path}")

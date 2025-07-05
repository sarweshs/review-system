import json
import uuid
import os
import logging
import random
from faker import Faker
from datetime import datetime
import sys
import argparse
import time

faker = Faker()

LOG_DIR = "logs"
LOG_FILE = os.path.join(LOG_DIR, "mock_review_generator.log")

# Ensure logs directory exists
try:
    os.makedirs(LOG_DIR, exist_ok=True)
except Exception as e:
    print(f"Could not create or access log directory '{LOG_DIR}': {e}")
    sys.exit(1)

# Configure logging to file and console
logging.basicConfig(
    level=logging.INFO,
    format="[%(asctime)s] %(levelname)s - %(message)s",
    handlers=[
        logging.FileHandler(LOG_FILE),
        logging.StreamHandler(sys.stdout)
    ]
)

OUTPUT_DIR = "generated_reviews"
try:
    os.makedirs(OUTPUT_DIR, exist_ok=True)
except Exception as e:
    logging.error(f"Could not create or access output directory '{OUTPUT_DIR}': {e}")
    sys.exit(1)

if not os.path.isfile("sample.jl"):
    logging.error("sample.jl not found in the current directory. Please provide a sample.jl file with a valid JSON object on the first line.")
    sys.exit(1)

try:
    with open("sample.jl", "r") as f:
        TEMPLATE = json.loads(f.readline())
except Exception as e:
    logging.error(f"Failed to read or parse sample.jl: {e}")
    sys.exit(1)

def randomize_review():
    review = json.loads(json.dumps(TEMPLATE))  # deep copy

    review["hotelId"] = random.randint(10000, 99999)
    review["hotelName"] = faker.company()
    review["platform"] = random.choice(["Agoda", "Booking.com", "Expedia"])
    review["comment"]["hotelReviewId"] = random.randint(1_000_000_000, 9_999_999_999)
    review["comment"]["rating"] = round(random.uniform(5, 10), 1)
    review["comment"]["formattedRating"] = str(review["comment"]["rating"])
    review["comment"]["reviewDate"] = datetime.now().isoformat()
    review["comment"]["formattedReviewDate"] = datetime.now().strftime("%B %d, %Y")
    review["comment"]["reviewComments"] = faker.text(200)
    review["comment"]["reviewerInfo"]["countryName"] = faker.country()
    review["comment"]["reviewerInfo"]["countryId"] = random.randint(1, 300)
    review["comment"]["reviewerInfo"]["displayMemberName"] = faker.first_name()

    return review

def corrupt_review():
    record = randomize_review()
    # Randomly delete hotelId, hotelName, or both
    choice = random.choice(['hotelId', 'hotelName', 'both'])
    if choice == 'hotelId':
        del record["hotelId"]
    elif choice == 'hotelName':
        del record["hotelName"]
    else:  # both
        del record["hotelId"]
        del record["hotelName"]
    return record

def generate_file(num_records=25):
    timestamp = datetime.now().strftime("%Y%m%d%H%M%S")
    path = os.path.join(OUTPUT_DIR, f"reviews_{timestamp}.jl")

    try:
        with open(path, "w") as f:
            for i in range(num_records):
                record = corrupt_review() if (i > 0 and i % 20 == 0) else randomize_review()
                f.write(json.dumps(record) + "\n")
        logging.info(f"Generated {num_records} reviews in: {path}")
    except Exception as e:
        logging.error(f"Failed to write reviews to {path}: {e}")
        sys.exit(1)
    return path

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Mock Review Generator")
    parser.add_argument("--interval", type=int, default=300, help="Interval in seconds between file generations (default: 300)")
    parser.add_argument("--count", type=int, default=25, help="Number of reviews per file (default: 25)")
    args = parser.parse_args()

    logging.info(f"Starting review generator: interval={args.interval}s, count={args.count}")
    try:
        while True:
            generate_file(args.count)
            logging.info(f"Sleeping for {args.interval} seconds...")
            time.sleep(args.interval)
    except KeyboardInterrupt:
        logging.info("Exiting on user request.")
        sys.exit(0)

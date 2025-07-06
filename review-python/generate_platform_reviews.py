import json
import random
import os
import argparse
import logging
import time
import sys
from faker import Faker
from datetime import datetime, timedelta

faker = Faker()

# Setup logging
logging.basicConfig(
    level=logging.INFO,
    format="[%(asctime)s] %(levelname)s - %(message)s",
    handlers=[
        logging.StreamHandler(sys.stdout)
    ]
)

OUTPUT_DIR = "generated_reviews"
os.makedirs(OUTPUT_DIR, exist_ok=True)

PLATFORMS = ["Agoda", "Booking.com", "Expedia"]

def random_reviewer_info():
    country_name = faker.country()
    return {
        "countryName": country_name,
        "displayMemberName": faker.first_name(),
        "flagName": country_name[:2].lower(),
        "reviewGroupName": random.choice(["Solo traveler", "Couple", "Business traveler", "Family with young children"]),
        "roomTypeName": random.choice(["Deluxe Room", "Standard Twin Room", "Premium Deluxe Double Room"]),
        "countryId": random.randint(1, 300),
        "lengthOfStay": random.randint(1, 10),
        "reviewGroupId": random.randint(1, 5),
        "roomTypeId": random.randint(0, 10),
        "reviewerReviewedCount": random.randint(0, 50),
        "isExpertReviewer": random.choice([True, False]),
        "isShowGlobalIcon": random.choice([True, False]),
        "isShowReviewedCount": random.choice([True, False])
    }

def random_grades():
    categories = ["Cleanliness", "Facilities", "Location", "Room comfort and quality", "Service", "Value for money"]
    return {cat: round(random.uniform(6.0, 10.0), 1) for cat in categories}

def randomize_review(platform):
    now = datetime.now()
    review_date = now - timedelta(days=random.randint(1, 730))  # within last 2 years

    rating = round(random.uniform(5.0, 10.0), 1)
    rating_text = random.choice(["Excellent", "Very good", "Good", "Fair", "Poor"])

    return {
        "hotelId": random.randint(10000, 99999),
        "hotelName": faker.company(),
        "platform": platform,
        "comment": {
            "isShowReviewResponse": random.choice([True, False]),
            "hotelReviewId": random.randint(1_000_000_000, 9_999_999_999),
            "providerId": random.randint(300, 400),
            "rating": rating,
            "checkInDateMonthAndYear": review_date.strftime("%B %Y"),
            "encryptedReviewData": faker.sha1(),
            "formattedRating": f"{rating:.1f}",
            "formattedReviewDate": review_date.strftime("%B %d, %Y"),
            "ratingText": rating_text,
            "responderName": faker.company(),
            "responseDateText": "",
            "responseTranslateSource": "en",
            "reviewComments": faker.text(200),
            "reviewNegatives": faker.sentence(),
            "reviewPositives": faker.sentence(),
            "reviewProviderLogo": "",
            "reviewProviderText": platform,
            "reviewTitle": faker.sentence(nb_words=6),
            "translateSource": "en",
            "translateTarget": "en",
            "reviewDate": review_date.isoformat(),
            "reviewerInfo": random_reviewer_info(),
            "originalTitle": "",
            "originalComment": "",
            "formattedResponseDate": ""
        },
        "overallByProviders": [{
            "providerId": random.randint(300, 400),
            "provider": platform,
            "overallScore": round(random.uniform(6.0, 9.5), 1),
            "reviewCount": random.randint(100, 10000),
            "grades": random_grades()
        }]
    }

def corrupt_review(platform):
    record = randomize_review(platform)
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

def main(num_reviews=20):
    platform = random.choice(PLATFORMS)
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    filename = f"{platform.lower().replace('.', '')}_reviews_{timestamp}.jl"
    filepath = os.path.join(OUTPUT_DIR, filename)

    try:
        with open(filepath, "w") as f:
            for i in range(num_reviews):
                # Generate corrupt record every 5th record (20% corruption rate)
                record = corrupt_review(platform) if (i > 0 and i % 5 == 0) else randomize_review(platform)
                f.write(json.dumps(record) + "\n")
        
        logging.info(f"✅ Generated {num_reviews} reviews for '{platform}' → {filename}")
        return filepath
    except Exception as e:
        logging.error(f"Failed to write reviews to {filepath}: {e}")
        sys.exit(1)

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Platform-specific Review Generator")
    parser.add_argument("--interval", type=int, default=300, help="Interval in seconds between file generations (default: 300)")
    parser.add_argument("--count", type=int, default=25, help="Number of reviews per file (default: 25)")
    args = parser.parse_args()

    logging.info(f"Starting platform review generator: interval={args.interval}s, count={args.count}")
    try:
        while True:
            main(args.count)
            logging.info(f"Sleeping for {args.interval} seconds...")
            time.sleep(args.interval)
    except KeyboardInterrupt:
        logging.info("Exiting on user request.")
        sys.exit(0)
   

#!/usr/bin/env python3
"""
Test Kafka Producer Script
Sends one good review and one bad review to respective Kafka topics
Matching the exact format used by review-producer
"""

import json
import random
from datetime import datetime, timedelta
from faker import Faker
from confluent_kafka import Producer
import logging

# Setup logging
logging.basicConfig(
    level=logging.INFO,
    format="[%(asctime)s] %(levelname)s - %(message)s"
)

# Kafka configuration
KAFKA_BOOTSTRAP_SERVERS = ['localhost:9092']
GOOD_REVIEWS_TOPIC = 'good_review_records'
BAD_REVIEWS_TOPIC = 'bad_review_records'

faker = Faker()

def create_reviewer_info():
    """Create reviewer information"""
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

def create_grades():
    """Create hotel grades"""
    categories = ["Cleanliness", "Facilities", "Location", "Room comfort and quality", "Service", "Value for money"]
    return {cat: round(random.uniform(6.0, 10.0), 1) for cat in categories}

def create_good_review():
    """Create a valid review (good data)"""
    now = datetime.now()
    review_date = now - timedelta(days=random.randint(1, 730))
    
    rating = round(random.uniform(5.0, 10.0), 1)
    rating_text = random.choice(["Excellent", "Very good", "Good", "Fair", "Poor"])
    platform = random.choice(["Agoda", "Booking.com", "Expedia"])
    
    review = {
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
            "reviewerInfo": create_reviewer_info(),
            "originalTitle": "",
            "originalComment": "",
            "formattedResponseDate": ""
        },
        "overallByProviders": [{
            "providerId": random.randint(300, 400),
            "provider": platform,
            "overallScore": round(random.uniform(6.0, 9.5), 1),
            "reviewCount": random.randint(100, 10000),
            "grades": create_grades()
        }]
    }
    
    return review

def create_bad_review():
    """Create a bad review record (matching producer's BadReviewRecord format)"""
    # First create a valid review
    valid_review = create_good_review()
    
    # Create the bad review record in the same format as review-producer
    bad_review_record = {
        "jsonData": json.dumps(valid_review),
        "platform": valid_review["platform"],
        "reason": "HOTEL_ID_NULL"  # Example validation failure
    }
    
    return bad_review_record

def delivery_report(err, msg):
    """Delivery report callback"""
    if err is not None:
        logging.error(f"❌ Message delivery failed: {err}")
    else:
        logging.info(f"✅ Message delivered to {msg.topic()} [{msg.partition()}] at offset {msg.offset()}")

def send_to_kafka():
    """Send one good review and one bad review to Kafka"""
    try:
        # Create Kafka producer
        producer = Producer({
            'bootstrap.servers': ','.join(KAFKA_BOOTSTRAP_SERVERS),
            'client.id': 'test-producer'
        })
        
        # Create and send good review
        good_review = create_good_review()
        good_review_json = json.dumps(good_review)
        
        logging.info(f"Sending good review to topic: {GOOD_REVIEWS_TOPIC}")
        logging.info(f"Good review: hotelId={good_review['hotelId']}, platform={good_review['platform']}")
        
        producer.produce(
            topic=GOOD_REVIEWS_TOPIC,
            key=str(good_review['hotelId']),
            value=good_review_json,
            callback=delivery_report
        )
        
        # Create and send bad review
        bad_review = create_bad_review()
        bad_review_json = json.dumps(bad_review)
        
        logging.info(f"Sending bad review to topic: {BAD_REVIEWS_TOPIC}")
        logging.info(f"Bad review: platform={bad_review['platform']}, reason={bad_review['reason']}")
        
        producer.produce(
            topic=BAD_REVIEWS_TOPIC,
            key=bad_review['platform'],
            value=bad_review_json,
            callback=delivery_report
        )
        
        # Wait for all messages to be delivered
        producer.flush()
        logging.info("🎉 Test messages sent successfully!")
        
    except Exception as e:
        logging.error(f"❌ Failed to send messages to Kafka: {e}")
        raise

if __name__ == "__main__":
    logging.info("Starting test Kafka producer...")
    send_to_kafka()
    logging.info("Test completed!") 
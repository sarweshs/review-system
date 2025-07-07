#!/bin/bash

# Test script for DLQ functionality
# This script demonstrates how records with missing critical fields are handled

BASE_URL="http://localhost:7072/api/producer"

echo "🧪 Testing DLQ Functionality for Review Producer"
echo "================================================"

# Test 1: Valid Review (should go to good_review_records)
echo ""
echo "✅ Test 1: Valid Review"
curl -X POST "${BASE_URL}/review" \
  -H "Content-Type: application/json" \
  -d '{
    "reviewJson": "{\"hotelId\": 12345, \"hotelName\": \"Test Hotel\", \"platform\": \"Booking.com\", \"comment\": {\"hotelReviewId\": 987654321, \"providerId\": 334, \"rating\": 4.5, \"reviewComments\": \"Great hotel!\"}}",
    "platform": "Booking.com"
  }'
echo ""

# Test 2: Missing Review ID (should go to DLQ)
echo ""
echo "🚨 Test 2: Missing Review ID (DLQ)"
curl -X POST "${BASE_URL}/review" \
  -H "Content-Type: application/json" \
  -d '{
    "reviewJson": "{\"hotelId\": 12345, \"hotelName\": \"Test Hotel\", \"platform\": \"Booking.com\", \"comment\": {\"providerId\": 334, \"rating\": 4.5, \"reviewComments\": \"Great hotel!\"}}",
    "platform": "Booking.com"
  }'
echo ""

# Test 3: Missing Provider ID (should go to DLQ)
echo ""
echo "🚨 Test 3: Missing Provider ID (DLQ)"
curl -X POST "${BASE_URL}/review" \
  -H "Content-Type: application/json" \
  -d '{
    "reviewJson": "{\"hotelId\": 12345, \"hotelName\": \"Test Hotel\", \"platform\": \"Booking.com\", \"comment\": {\"hotelReviewId\": 987654321, \"rating\": 4.5, \"reviewComments\": \"Great hotel!\"}}",
    "platform": "Booking.com"
  }'
echo ""

# Test 4: Missing Comment Section (should go to DLQ)
echo ""
echo "🚨 Test 4: Missing Comment Section (DLQ)"
curl -X POST "${BASE_URL}/review" \
  -H "Content-Type: application/json" \
  -d '{
    "reviewJson": "{\"hotelId\": 12345, \"hotelName\": \"Test Hotel\", \"platform\": \"Booking.com\", \"rating\": 4.5}",
    "platform": "Booking.com"
  }'
echo ""

# Test 5: Invalid Review ID Value (should go to DLQ)
echo ""
echo "🚨 Test 5: Invalid Review ID Value (DLQ)"
curl -X POST "${BASE_URL}/review" \
  -H "Content-Type: application/json" \
  -d '{
    "reviewJson": "{\"hotelId\": 12345, \"hotelName\": \"Test Hotel\", \"platform\": \"Booking.com\", \"comment\": {\"hotelReviewId\": 0, \"providerId\": 334, \"rating\": 4.5}}",
    "platform": "Booking.com"
  }'
echo ""

# Test 6: Missing Hotel Name (should go to bad_review_records)
echo ""
echo "⚠️ Test 6: Missing Hotel Name (Bad Review)"
curl -X POST "${BASE_URL}/review" \
  -H "Content-Type: application/json" \
  -d '{
    "reviewJson": "{\"hotelId\": 12345, \"platform\": \"Booking.com\", \"comment\": {\"hotelReviewId\": 987654321, \"providerId\": 334, \"rating\": 4.5}}",
    "platform": "Booking.com"
  }'
echo ""

# Test 7: Direct DLQ Send
echo ""
echo "🎯 Test 7: Direct DLQ Send"
curl -X POST "${BASE_URL}/review/dlq" \
  -H "Content-Type: application/json" \
  -d '{
    "reviewJson": "{\"hotelId\": 12345, \"hotelName\": \"Test Hotel\", \"platform\": \"Booking.com\", \"comment\": {\"providerId\": 334, \"rating\": 4.5}}",
    "platform": "Booking.com"
  }'
echo ""

echo ""
echo "🎉 DLQ Testing Complete!"
echo ""
echo "📊 Expected Results:"
echo "  - Tests 1: Sent to good_review_records"
echo "  - Tests 2-5,7: Sent to dlq topic"
echo "  - Test 6: Sent to bad_review_records"
echo ""
echo "🔍 To verify results, check Kafka topics:"
echo "  kafka-console-consumer.sh --topic good_review_records --bootstrap-server localhost:9092 --from-beginning"
echo "  kafka-console-consumer.sh --topic dlq --bootstrap-server localhost:9092 --from-beginning"
echo "  kafka-console-consumer.sh --topic bad_review_records --bootstrap-server localhost:9092 --from-beginning" 
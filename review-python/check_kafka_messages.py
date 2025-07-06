#!/usr/bin/env python3
"""
Check Kafka Messages Script
Reads and displays messages from both good_review_records and bad_review_records topics
"""

import json
from confluent_kafka import Consumer
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

def check_topic_messages(topic_name, max_messages=10):
    """Check messages in a specific topic"""
    consumer = Consumer({
        'bootstrap.servers': ','.join(KAFKA_BOOTSTRAP_SERVERS),
        'group.id': f'check-messages-{topic_name}',
        'auto.offset.reset': 'earliest',
        'enable.auto.commit': False
    })
    
    consumer.subscribe([topic_name])
    
    messages = []
    try:
        # Wait a bit for the consumer to connect
        consumer.poll(timeout=2.0)
        
        while len(messages) < max_messages:
            msg = consumer.poll(timeout=2.0)
            if msg is None:
                break
            if msg.error():
                logging.error(f"Consumer error: {msg.error()}")
                continue
                
            try:
                # Try to parse as JSON
                value = json.loads(msg.value().decode('utf-8'))
                messages.append({
                    'key': msg.key().decode('utf-8') if msg.key() else None,
                    'value': value,
                    'partition': msg.partition(),
                    'offset': msg.offset()
                })
            except json.JSONDecodeError:
                # If not JSON, show as string
                messages.append({
                    'key': msg.key().decode('utf-8') if msg.key() else None,
                    'value': msg.value().decode('utf-8'),
                    'partition': msg.partition(),
                    'offset': msg.offset()
                })
                
    except KeyboardInterrupt:
        pass
    finally:
        consumer.close()
    
    return messages

def main():
    print("=" * 80)
    print("CHECKING KAFKA TOPICS")
    print("=" * 80)
    
    # Check good_review_records topic
    print(f"\n📋 Checking topic: {GOOD_REVIEWS_TOPIC}")
    print("-" * 60)
    good_messages = check_topic_messages(GOOD_REVIEWS_TOPIC, max_messages=5)
    
    if good_messages:
        print(f"Found {len(good_messages)} messages:")
        for i, msg in enumerate(good_messages, 1):
            print(f"\n--- Message {i} ---")
            print(f"Key: {msg['key']}")
            print(f"Partition: {msg['partition']}, Offset: {msg['offset']}")
            print("Value:")
            print(json.dumps(msg['value'], indent=2))
    else:
        print("❌ No messages found in this topic")
    
    # Check bad_review_records topic
    print(f"\n📋 Checking topic: {BAD_REVIEWS_TOPIC}")
    print("-" * 60)
    bad_messages = check_topic_messages(BAD_REVIEWS_TOPIC, max_messages=5)
    
    if bad_messages:
        print(f"Found {len(bad_messages)} messages:")
        for i, msg in enumerate(bad_messages, 1):
            print(f"\n--- Message {i} ---")
            print(f"Key: {msg['key']}")
            print(f"Partition: {msg['partition']}, Offset: {msg['offset']}")
            print("Value:")
            print(json.dumps(msg['value'], indent=2))
    else:
        print("❌ No messages found in this topic")
    
    print("\n" + "=" * 80)
    print("SUMMARY")
    print("=" * 80)
    print(f"Good review records: {len(good_messages)} messages")
    print(f"Bad review records: {len(bad_messages)} messages")
    print("=" * 80)

if __name__ == "__main__":
    main() 
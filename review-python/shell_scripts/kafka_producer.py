#!/usr/bin/env python3
"""
Kafka Producer for Review System
Reads JSONL file and sends each line to Kafka topic
"""

import json
import time
import argparse
import logging
from typing import Iterator
from confluent_kafka import Producer, KafkaError
import os

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


class ReviewKafkaProducer:
    """Kafka producer for sending review data"""
    
    def __init__(self, bootstrap_servers: str, topic: str):
        """
        Initialize Kafka producer
        
        Args:
            bootstrap_servers: Kafka bootstrap servers (e.g., 'localhost:9092')
            topic: Kafka topic name
        """
        self.bootstrap_servers = bootstrap_servers
        self.topic = topic
        self.producer = None
        self.connect()
    
    def connect(self):
        """Connect to Kafka"""
        try:
            self.producer = Producer({
                'bootstrap.servers': self.bootstrap_servers,
                'client.id': 'review-producer',
                'acks': 'all',  # Wait for all replicas
                'retries': 3,
                'max.in.flight.requests.per.connection': 1
            })
            logger.info(f"Connected to Kafka at {self.bootstrap_servers}")
        except Exception as e:
            logger.error(f"Failed to connect to Kafka: {e}")
            raise
    
    def delivery_report(self, err, msg):
        """Delivery report callback"""
        if err is not None:
            logger.error(f"Message delivery failed: {err}")
        else:
            logger.info(f"Message delivered to {msg.topic()} [{msg.partition()}] at offset {msg.offset()}")
    
    def send_message(self, message: dict, key: str = None) -> bool:
        """
        Send a message to Kafka
        
        Args:
            message: JSON message to send
            key: Message key (optional)
            
        Returns:
            bool: True if message was sent successfully
        """
        try:
            # Serialize message to JSON
            value = json.dumps(message).encode('utf-8')
            key_bytes = key.encode('utf-8') if key else None
            
            # Send message
            self.producer.produce(
                topic=self.topic,
                value=value,
                key=key_bytes,
                callback=self.delivery_report
            )
            
            # Flush to ensure message is sent
            self.producer.flush()
            return True
            
        except Exception as e:
            logger.error(f"Failed to send message: {e}")
            return False
    
    def close(self):
        """Close the producer connection"""
        if self.producer:
            self.producer.flush()
            logger.info("Kafka producer closed")


def read_jsonl_file(file_path: str) -> Iterator[dict]:
    """
    Read JSONL file line by line
    
    Args:
        file_path: Path to JSONL file
        
    Yields:
        dict: Parsed JSON object from each line
    """
    try:
        with open(file_path, 'r', encoding='utf-8') as file:
            for line_num, line in enumerate(file, 1):
                line = line.strip()
                if not line:
                    continue
                
                try:
                    data = json.loads(line)
                    yield data
                except json.JSONDecodeError as e:
                    logger.error(f"Invalid JSON on line {line_num}: {e}")
                    continue
                    
    except FileNotFoundError:
        logger.error(f"File not found: {file_path}")
        raise
    except Exception as e:
        logger.error(f"Error reading file {file_path}: {e}")
        raise


def main():
    """Main function"""
    parser = argparse.ArgumentParser(description='Kafka Producer for Review System')
    parser.add_argument(
        '--file', '-f',
        default='sample.jl',
        help='JSONL file to read (default: sample.jl)'
    )
    parser.add_argument(
        '--bootstrap-servers', '-b',
        default='localhost:9092',
        help='Kafka bootstrap servers (default: localhost:9092)'
    )
    parser.add_argument(
        '--topic', '-t',
        default='reviews',
        help='Kafka topic name (default: reviews)'
    )
    parser.add_argument(
        '--sleep', '-s',
        type=int,
        default=300,  # 5 minutes
        help='Sleep interval between loops in seconds (default: 300)'
    )
    parser.add_argument(
        '--loop', '-l',
        action='store_true',
        help='Run in continuous loop'
    )
    parser.add_argument(
        '--key-field',
        default='hotelId',
        help='Field to use as message key (default: hotelId)'
    )
    parser.add_argument(
        '--dry-run',
        action='store_true',
        help='Print messages without sending to Kafka'
    )
    
    args = parser.parse_args()
    
    # Validate file exists
    if not os.path.exists(args.file):
        logger.error(f"File not found: {args.file}")
        return 1
    
    # Initialize producer
    producer = None
    if not args.dry_run:
        try:
            producer = ReviewKafkaProducer(args.bootstrap_servers, args.topic)
        except Exception as e:
            logger.error(f"Failed to initialize Kafka producer: {e}")
            return 1
    
    try:
        while True:
            logger.info(f"Reading file: {args.file}")
            messages_sent = 0
            messages_failed = 0
            
            for data in read_jsonl_file(args.file):
                try:
                    # Extract key from data
                    key = str(data.get(args.key_field, ''))
                    
                    if args.dry_run:
                        logger.info(f"DRY RUN - Would send: {data}")
                        messages_sent += 1
                    else:
                        if producer.send_message(data, key):
                            messages_sent += 1
                        else:
                            messages_failed += 1
                            
                except Exception as e:
                    logger.error(f"Error processing message: {e}")
                    messages_failed += 1
            
            logger.info(f"Loop completed - Sent: {messages_sent}, Failed: {messages_failed}")
            
            if not args.loop:
                break
            
            logger.info(f"Sleeping for {args.sleep} seconds before next loop...")
            time.sleep(args.sleep)
            
    except KeyboardInterrupt:
        logger.info("Interrupted by user")
    except Exception as e:
        logger.error(f"Unexpected error: {e}")
        return 1
    finally:
        if producer:
            producer.close()
    
    return 0


if __name__ == '__main__':
    exit(main()) 
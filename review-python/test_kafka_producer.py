#!/usr/bin/env python3
"""
Test script for Kafka Producer
Runs the producer with a short sleep interval for testing
"""

import subprocess
import sys
import os

def main():
    """Run the Kafka producer with test settings"""
    
    # Get the directory of this script
    script_dir = os.path.dirname(os.path.abspath(__file__))
    
    # Command to run the Kafka producer
    cmd = [
        sys.executable,  # Use the same Python interpreter
        os.path.join(script_dir, 'kafka_producer.py'),
        '--sleep', '10',  # 10 seconds sleep for testing
        '--loop',  # Run in loop
        '--dry-run'  # Dry run for testing
    ]
    
    print("Starting Kafka producer test...")
    print(f"Command: {' '.join(cmd)}")
    print("Press Ctrl+C to stop")
    print("-" * 50)
    
    try:
        # Run the command
        subprocess.run(cmd, check=True)
    except KeyboardInterrupt:
        print("\nTest stopped by user")
    except subprocess.CalledProcessError as e:
        print(f"Error running Kafka producer: {e}")
        return 1
    
    return 0

if __name__ == '__main__':
    exit(main()) 
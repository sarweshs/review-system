#!/bin/bash

# DLQ Retention Monitoring Script
# This script monitors DLQ topic retention, size, and health

set -e

# Configuration
TOPIC="dlq"
BROKER="${KAFKA_BROKER:-localhost:9092}"
RETENTION_DAYS="${DLQ_RETENTION_DAYS:-7}"
MAX_SIZE_GB="${DLQ_MAX_SIZE_GB:-1}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Helper functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if Kafka tools are available
check_kafka_tools() {
    if ! command -v kafka-topics.sh &> /dev/null; then
        log_error "Kafka tools not found. Please ensure Kafka is installed and in PATH."
        exit 1
    fi
}

# Check topic exists
check_topic_exists() {
    if ! kafka-topics.sh --bootstrap-server $BROKER --list | grep -q "^$TOPIC$"; then
        log_error "Topic '$TOPIC' does not exist!"
        exit 1
    fi
}

# Get topic configuration
get_topic_config() {
    log_info "Getting topic configuration..."
    kafka-configs.sh --bootstrap-server $BROKER \
        --entity-type topics --entity-name $TOPIC --describe
}

# Get topic size and message count
get_topic_stats() {
    log_info "Getting topic statistics..."
    
    # Get offsets for all partitions
    local offsets=$(kafka-run-class.sh kafka.tools.GetOffsetShell \
        --broker-list $BROKER --topic $TOPIC --time -1)
    
    # Calculate total messages
    local total_messages=0
    while IFS=: read -r topic partition offset; do
        if [[ "$topic" == "$TOPIC" ]]; then
            total_messages=$((total_messages + offset))
        fi
    done <<< "$offsets"
    
    echo "Total messages: $total_messages"
    
    # Get topic size (approximate)
    local topic_size_bytes=$(kafka-log-dirs.sh --bootstrap-server $BROKER \
        --describe --topic-list $TOPIC | grep -o '"size": [0-9]*' | awk '{sum += $2} END {print sum}')
    
    if [[ -n "$topic_size_bytes" && "$topic_size_bytes" -gt 0 ]]; then
        local topic_size_mb=$((topic_size_bytes / 1024 / 1024))
        echo "Topic size: ${topic_size_mb}MB"
    else
        echo "Topic size: Unable to determine"
    fi
}

# Check retention settings
check_retention_settings() {
    log_info "Checking retention settings..."
    
    local retention_ms=$(kafka-configs.sh --bootstrap-server $BROKER \
        --entity-type topics --entity-name $TOPIC --describe | \
        grep "retention.ms" | awk '{print $2}')
    
    local retention_bytes=$(kafka-configs.sh --bootstrap-server $BROKER \
        --entity-type topics --entity-name $TOPIC --describe | \
        grep "retention.bytes" | awk '{print $2}')
    
    if [[ -n "$retention_ms" ]]; then
        local retention_days=$((retention_ms / 1000 / 60 / 60 / 24))
        echo "Time retention: ${retention_days} days (${retention_ms}ms)"
        
        if [[ $retention_days -lt $RETENTION_DAYS ]]; then
            log_warning "Retention period (${retention_days} days) is less than recommended (${RETENTION_DAYS} days)"
        else
            log_success "Retention period is within recommended range"
        fi
    else
        log_warning "Time retention not configured"
    fi
    
    if [[ -n "$retention_bytes" ]]; then
        local retention_gb=$((retention_bytes / 1024 / 1024 / 1024))
        echo "Size retention: ${retention_gb}GB (${retention_bytes} bytes)"
        
        if [[ $retention_gb -gt $MAX_SIZE_GB ]]; then
            log_warning "Size retention (${retention_gb}GB) exceeds recommended limit (${MAX_SIZE_GB}GB)"
        else
            log_success "Size retention is within recommended range"
        fi
    else
        log_warning "Size retention not configured"
    fi
}

# Check message age distribution
check_message_age() {
    log_info "Checking message age distribution..."
    
    # Get oldest and newest offsets
    local oldest_offsets=$(kafka-run-class.sh kafka.tools.GetOffsetShell \
        --broker-list $BROKER --topic $TOPIC --time -2)
    
    local newest_offsets=$(kafka-run-class.sh kafka.tools.GetOffsetShell \
        --broker-list $BROKER --topic $TOPIC --time -1)
    
    echo "Message age analysis:"
    echo "  Oldest messages: $(echo "$oldest_offsets" | head -1)"
    echo "  Newest messages: $(echo "$newest_offsets" | head -1)"
    
    # Calculate approximate age (this is a rough estimate)
    local total_oldest=0
    local total_newest=0
    
    while IFS=: read -r topic partition offset; do
        if [[ "$topic" == "$TOPIC" ]]; then
            total_oldest=$((total_oldest + offset))
        fi
    done <<< "$oldest_offsets"
    
    while IFS=: read -r topic partition offset; do
        if [[ "$topic" == "$TOPIC" ]]; then
            total_newest=$((total_newest + offset))
        fi
    done <<< "$newest_offsets"
    
    local message_count=$((total_newest - total_oldest))
    if [[ $message_count -gt 0 ]]; then
        echo "  Approximate message count: $message_count"
        
        if [[ $message_count -gt 1000 ]]; then
            log_warning "High message count detected: $message_count"
        else
            log_success "Message count is within normal range"
        fi
    fi
}

# Check consumer lag (if any consumers exist)
check_consumer_lag() {
    log_info "Checking consumer lag..."
    
    local consumer_groups=$(kafka-consumer-groups.sh --bootstrap-server $BROKER --list | grep dlq || true)
    
    if [[ -n "$consumer_groups" ]]; then
        echo "Consumer groups for DLQ:"
        echo "$consumer_groups"
        
        while IFS= read -r group; do
            if [[ -n "$group" ]]; then
                echo "Consumer group: $group"
                kafka-consumer-groups.sh --bootstrap-server $BROKER --group "$group" --describe
            fi
        done <<< "$consumer_groups"
    else
        log_info "No consumer groups found for DLQ topic"
    fi
}

# Generate health report
generate_health_report() {
    log_info "Generating health report..."
    
    echo ""
    echo "=========================================="
    echo "DLQ Retention Health Report"
    echo "=========================================="
    echo "Topic: $TOPIC"
    echo "Broker: $BROKER"
    echo "Timestamp: $(date)"
    echo ""
    
    # Check if topic exists
    if kafka-topics.sh --bootstrap-server $BROKER --list | grep -q "^$TOPIC$"; then
        log_success "Topic exists"
        
        # Get basic stats
        get_topic_stats
        
        # Check retention
        check_retention_settings
        
        # Check message age
        check_message_age
        
        # Check consumer lag
        check_consumer_lag
        
    else
        log_error "Topic does not exist"
    fi
    
    echo ""
    echo "=========================================="
    echo "End of Report"
    echo "=========================================="
}

# Main execution
main() {
    echo "DLQ Retention Monitoring"
    echo "========================"
    echo "Topic: $TOPIC"
    echo "Broker: $BROKER"
    echo "Recommended retention: ${RETENTION_DAYS} days"
    echo "Max size: ${MAX_SIZE_GB}GB"
    echo ""
    
    # Check prerequisites
    check_kafka_tools
    check_topic_exists
    
    # Generate comprehensive report
    generate_health_report
}

# Handle command line arguments
case "${1:-report}" in
    "config")
        check_kafka_tools
        check_topic_exists
        get_topic_config
        ;;
    "stats")
        check_kafka_tools
        check_topic_exists
        get_topic_stats
        ;;
    "retention")
        check_kafka_tools
        check_topic_exists
        check_retention_settings
        ;;
    "age")
        check_kafka_tools
        check_topic_exists
        check_message_age
        ;;
    "consumers")
        check_kafka_tools
        check_topic_exists
        check_consumer_lag
        ;;
    "report"|*)
        main
        ;;
esac 
#!/bin/bash

# DLQ Retention Configuration Script
# This script allows dynamic configuration of DLQ retention settings

set -e

# Configuration
TOPIC="dlq"
BROKER="${KAFKA_BROKER:-localhost:9092}"

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
    if ! command -v kafka-configs.sh &> /dev/null; then
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

# Show current retention settings
show_current_settings() {
    log_info "Current DLQ retention settings:"
    kafka-configs.sh --bootstrap-server $BROKER \
        --entity-type topics --entity-name $TOPIC --describe | \
        grep -E "(retention|cleanup|segment)" || echo "No retention settings configured"
}

# Set time-based retention
set_time_retention() {
    local days=$1
    local ms=$((days * 24 * 60 * 60 * 1000))
    
    log_info "Setting time retention to ${days} days (${ms}ms)..."
    
    kafka-configs.sh --bootstrap-server $BROKER \
        --entity-type topics --entity-name $TOPIC \
        --alter --add-config retention.ms=$ms
    
    log_success "Time retention set to ${days} days"
}

# Set size-based retention
set_size_retention() {
    local gb=$1
    local bytes=$((gb * 1024 * 1024 * 1024))
    
    log_info "Setting size retention to ${gb}GB (${bytes} bytes)..."
    
    kafka-configs.sh --bootstrap-server $BROKER \
        --entity-type topics --entity-name $TOPIC \
        --alter --add-config retention.bytes=$bytes
    
    log_success "Size retention set to ${gb}GB"
}

# Set segment rotation
set_segment_rotation() {
    local hours=$1
    local ms=$((hours * 60 * 60 * 1000))
    
    log_info "Setting segment rotation to ${hours} hours (${ms}ms)..."
    
    kafka-configs.sh --bootstrap-server $BROKER \
        --entity-type topics --entity-name $TOPIC \
        --alter --add-config segment.ms=$ms
    
    log_success "Segment rotation set to ${hours} hours"
}

# Set cleanup policy
set_cleanup_policy() {
    local policy=$1
    
    log_info "Setting cleanup policy to '$policy'..."
    
    kafka-configs.sh --bootstrap-server $BROKER \
        --entity-type topics --entity-name $TOPIC \
        --alter --add-config cleanup.policy=$policy
    
    log_success "Cleanup policy set to '$policy'"
}

# Apply preset configurations
apply_preset() {
    local preset=$1
    
    case $preset in
        "development")
            log_info "Applying development preset (1 day retention, 128MB size)..."
            set_time_retention 1
            set_size_retention 0.125  # 128MB
            set_segment_rotation 1
            set_cleanup_policy "delete"
            ;;
        "staging")
            log_info "Applying staging preset (3 days retention, 256MB size)..."
            set_time_retention 3
            set_size_retention 0.25   # 256MB
            set_segment_rotation 24
            set_cleanup_policy "delete"
            ;;
        "production")
            log_info "Applying production preset (7 days retention, 1GB size)..."
            set_time_retention 7
            set_size_retention 1
            set_segment_rotation 24
            set_cleanup_policy "delete"
            ;;
        "compliance")
            log_info "Applying compliance preset (30 days retention, 2GB size)..."
            set_time_retention 30
            set_size_retention 2
            set_segment_rotation 24
            set_cleanup_policy "delete"
            ;;
        *)
            log_error "Unknown preset: $preset"
            echo "Available presets: development, staging, production, compliance"
            exit 1
            ;;
    esac
    
    log_success "Preset '$preset' applied successfully"
}

# Show usage
show_usage() {
    echo "DLQ Retention Configuration Script"
    echo "=================================="
    echo ""
    echo "Usage: $0 [COMMAND] [OPTIONS]"
    echo ""
    echo "Commands:"
    echo "  show                    Show current retention settings"
    echo "  preset <name>           Apply preset configuration"
    echo "  time <days>             Set time-based retention (in days)"
    echo "  size <gb>               Set size-based retention (in GB)"
    echo "  segment <hours>         Set segment rotation (in hours)"
    echo "  cleanup <policy>        Set cleanup policy (delete/compact)"
    echo ""
    echo "Presets:"
    echo "  development             1 day, 128MB"
    echo "  staging                 3 days, 256MB"
    echo "  production              7 days, 1GB"
    echo "  compliance              30 days, 2GB"
    echo ""
    echo "Examples:"
    echo "  $0 show"
    echo "  $0 preset production"
    echo "  $0 time 14"
    echo "  $0 size 0.5"
    echo "  $0 segment 12"
    echo ""
    echo "Environment Variables:"
    echo "  KAFKA_BROKER            Kafka broker address (default: localhost:9092)"
}

# Main execution
main() {
    # Check prerequisites
    check_kafka_tools
    check_topic_exists
    
    case "${1:-help}" in
        "show")
            show_current_settings
            ;;
        "preset")
            if [[ -z "$2" ]]; then
                log_error "Preset name required"
                show_usage
                exit 1
            fi
            apply_preset "$2"
            ;;
        "time")
            if [[ -z "$2" ]]; then
                log_error "Days required"
                show_usage
                exit 1
            fi
            set_time_retention "$2"
            ;;
        "size")
            if [[ -z "$2" ]]; then
                log_error "Size in GB required"
                show_usage
                exit 1
            fi
            set_size_retention "$2"
            ;;
        "segment")
            if [[ -z "$2" ]]; then
                log_error "Hours required"
                show_usage
                exit 1
            fi
            set_segment_rotation "$2"
            ;;
        "cleanup")
            if [[ -z "$2" ]]; then
                log_error "Policy required"
                show_usage
                exit 1
            fi
            set_cleanup_policy "$2"
            ;;
        "help"|*)
            show_usage
            ;;
    esac
}

# Run main function
main "$@" 
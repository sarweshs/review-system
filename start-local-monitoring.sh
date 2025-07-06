#!/bin/bash

echo "🚀 Starting Local Monitoring Stack..."
echo "This will start Loki, Promtail, Prometheus, and Grafana for local development"
echo ""

# Create logs directory if it doesn't exist
mkdir -p logs

# Start the monitoring stack
echo "📊 Starting monitoring services..."
docker-compose -f docker-compose-local.yml up -d

echo ""
echo "✅ Monitoring stack started!"
echo ""
echo "📋 Services:"
echo "  • Loki (logs): http://localhost:3100"
echo "  • Prometheus (metrics): http://localhost:9090"
echo "  • Grafana (dashboard): http://localhost:3000 (admin/admin)"
echo ""
echo "📝 Next steps:"
echo "  1. Start your Spring Boot applications locally"
echo "  2. Check Prometheus targets: http://localhost:9090/targets"
echo "  3. Check Loki: http://localhost:3100/ready"
echo "  4. Login to Grafana and add Prometheus & Loki data sources"
echo ""
echo "🛑 To stop: docker-compose -f docker-compose-local.yml down" 
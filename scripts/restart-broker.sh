#!/bin/bash
# Emergency RabbitMQ Restart
# Usage: ./scripts/restart-broker.sh

echo "⚠️  WARNING: This will disconnect all active WebSocket sessions!"
read -p "Are you sure? (y/N) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]
then
    exit 1
fi

echo "🛑 Stopping RabbitMQ..."
docker compose -f docker-compose.production.yml stop rabbitmq

echo "🧹 Clearing temporary state..."
# Optional: docker compose -f docker-compose.production.yml rm -f rabbitmq

echo "🚀 Starting RabbitMQ..."
docker compose -f docker-compose.production.yml up -d rabbitmq

echo "⏳ Waiting for health check..."
# Loop until RabbitMQ management port is up
until curl -s -f -o /dev/null "http://localhost:15672"; do
    echo "   ...waiting for Broker to be ready"
    sleep 2
done

echo "✅ RabbitMQ is back online. Students will auto-reconnect."

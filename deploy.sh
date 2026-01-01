#!/bin/bash

# Production deployment script
# Run this script to deploy the exam portal

set -e

echo "🚀 Exam Portal Production Deployment"
echo "===================================="

# Check if .env exists
if [ ! -f .env ]; then
    echo "❌ .env file not found!"
    echo "📝 Copy .env.example to .env and fill in the values"
    exit 1
fi

# Source environment variables
source .env

# Check required variables
if [ "$DB_PASSWORD" == "CHANGE_ME_STRONG_PASSWORD_HERE" ]; then
    echo "❌ Please update passwords in .env file"
    exit 1
fi

echo "✅ Environment variables loaded"

# Build backend
echo "📦 Building backend..."
cd backend
./mvnw clean package -DskipTests
cd ..

# Build frontend
echo "📦 Building frontend..."
cd frontend
npm ci
npm run build
cd ..

echo "✅ Applications built successfully"

# Start infrastructure services first
echo "🔧 Starting infrastructure services..."
docker-compose -f docker-compose.production.yml up -d \
    postgres redis rabbitmq elasticsearch

# Wait for services to be healthy
echo "⏳ Waiting for services to be ready..."
sleep 30

# Check PostgreSQL
until docker exec exam-portal-postgres pg_isready -U $DB_USERNAME; do
    echo "⏳ Waiting for PostgreSQL..."
    sleep 5
done
echo "✅ PostgreSQL ready"

# Check Redis
until docker exec exam-portal-redis redis-cli ping; do
    echo "⏳ Waiting for Redis..."
    sleep 5
done
echo "✅ Redis ready"

# Check RabbitMQ
until docker exec exam-portal-rabbitmq rabbitmq-diagnostics ping; do
    echo "⏳ Waiting for RabbitMQ..."
    sleep 5
done
echo "✅ RabbitMQ ready"

# Start application services
echo "🚀 Starting application services..."
docker-compose -f docker-compose.production.yml up -d \
    backend judge0

# Wait for backend
echo "⏳ Waiting for backend to be ready..."
sleep 20
until curl -f http://localhost:8080/actuator/health; do
    echo "⏳ Waiting for backend..."
    sleep 5
done
echo "✅ Backend ready"

# Start frontend and monitoring
echo "🚀 Starting frontend and monitoring..."
docker-compose -f docker-compose.production.yml up -d

echo ""
echo "✅ Deployment complete!"
echo ""
echo "📊 Services:"
echo "  - Application: https://examportal.com (or http://localhost)"
echo "  - Backend API: http://localhost:8080"
echo "  - Grafana: http://localhost:3000 (admin/$GRAFANA_ADMIN_PASSWORD)"
echo "  - Prometheus: http://localhost:9090"
echo "  - Kibana: http://localhost:5601"
echo "  - RabbitMQ: http://localhost:15672 ($RABBITMQ_USERNAME/$RABBITMQ_PASSWORD)"
echo ""
echo "🔍 Check logs with: docker-compose -f docker-compose.production.yml logs -f [service]"
echo "🛑 Stop with: docker-compose -f docker-compose.production.yml down"
echo ""

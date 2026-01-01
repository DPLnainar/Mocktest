#!/bin/bash
# Pre-Exam System Health Check
# Usage: ./scripts/health-check.sh
# Run this 1 hour before exam starts. If anything is Red, DO NOT START THE EXAM.

echo "🔍 Running System Diagnostics..."
echo "================================"

ERRORS=0

check_service() {
    if docker ps --format '{{.Names}}' | grep -q "$1"; then
        echo "✅ Service [$1] is RUNNING"
    else
        echo "❌ Service [$1] is DOWN"
        ((ERRORS++))
    fi
}

# 1. Check Containers
echo ""
echo "🐳 Docker Services:"
check_service "exam-portal-backend"
check_service "exam-portal-nginx"
check_service "exam-portal-postgres"
check_service "exam-portal-redis"
check_service "exam-portal-rabbitmq"
check_service "exam-portal-judge0"
check_service "exam-portal-prometheus"
check_service "exam-portal-grafana"

# 2. Check Disk Space (Need at least 20% free for logs)
echo ""
echo "💽 Disk Space:"
DISK_USAGE=$(df / | grep / | awk '{ print $5 }' | sed 's/%//g')
if [ "$DISK_USAGE" -gt 80 ]; then
    echo "❌ CRITICAL: Disk space is low ($DISK_USAGE% used)!"
    ((ERRORS++))
else
    echo "✅ Disk Space OK ($DISK_USAGE% used)"
fi

# 3. Check Memory (Ensure no OOM risk)
echo ""
echo "🧠 Memory:"
FREE_MEM=$(free -m | grep Mem | awk '{print $4}')
TOTAL_MEM=$(free -m | grep Mem | awk '{print $2}')
MEM_PERCENT=$((100 - (FREE_MEM * 100 / TOTAL_MEM)))

if [ "$FREE_MEM" -lt 2000 ]; then
    echo "⚠️  WARNING: Low Free Memory (${FREE_MEM}MB / ${TOTAL_MEM}MB total). Monitoring recommended."
    if [ "$FREE_MEM" -lt 1000 ]; then
        echo "❌ CRITICAL: Very low memory!"
        ((ERRORS++))
    fi
else
    echo "✅ Memory OK (${FREE_MEM}MB free, ${MEM_PERCENT}% used)"
fi

# 4. Check Database Connection
echo ""
echo "🗄️  Database:"
if docker exec exam-portal-postgres pg_isready -U exam_portal_user > /dev/null 2>&1; then
    echo "✅ PostgreSQL is accepting connections"
    
    # Check connection count
    CONNECTIONS=$(docker exec exam-portal-postgres psql -U exam_portal_user -d exam_portal_db -t -c "SELECT COUNT(*) FROM pg_stat_activity WHERE state = 'active';" | xargs)
    echo "   Active connections: $CONNECTIONS/20"
    
    if [ "$CONNECTIONS" -gt 18 ]; then
        echo "⚠️  WARNING: Connection pool nearly exhausted!"
    fi
else
    echo "❌ PostgreSQL is NOT accepting connections"
    ((ERRORS++))
fi

# 5. Check Redis
echo ""
echo "💾 Redis:"
if docker exec exam-portal-redis redis-cli ping > /dev/null 2>&1; then
    echo "✅ Redis is responding"
    
    # Check memory
    REDIS_MEM=$(docker exec exam-portal-redis redis-cli info memory | grep used_memory_human | cut -d: -f2 | tr -d '\r')
    echo "   Memory usage: $REDIS_MEM"
else
    echo "❌ Redis is NOT responding"
    ((ERRORS++))
fi

# 6. Check RabbitMQ
echo ""
echo "🐰 RabbitMQ:"
if docker exec exam-portal-rabbitmq rabbitmq-diagnostics ping > /dev/null 2>&1; then
    echo "✅ RabbitMQ is responding"
else
    echo "❌ RabbitMQ is NOT responding"
    ((ERRORS++))
fi

# 7. Check Judge0
echo ""
echo "⚖️  Judge0:"
JUDGE0_STATUS=$(curl -s http://localhost:2358/workers 2>/dev/null || echo "ERROR")
if [[ $JUDGE0_STATUS != "ERROR" ]]; then
    echo "✅ Judge0 is responding"
else
    echo "❌ Judge0 is NOT responding"
    ((ERRORS++))
fi

# 8. Check Backend API
echo ""
echo "🌐 Backend API:"
BACKEND_HEALTH=$(curl -s http://localhost:8080/actuator/health 2>/dev/null | jq -r '.status' 2>/dev/null || echo "ERROR")
if [ "$BACKEND_HEALTH" = "UP" ]; then
    echo "✅ Backend health: UP"
else
    echo "❌ Backend health: $BACKEND_HEALTH"
    ((ERRORS++))
fi

# 9. Check CPU Usage
echo ""
echo "🔥 CPU Usage:"
CPU_USAGE=$(top -bn1 | grep "Cpu(s)" | sed "s/.*, *\([0-9.]*\)%* id.*/\1/" | awk '{print 100 - $1}' | cut -d. -f1)
if [ "$CPU_USAGE" -lt 70 ]; then
    echo "✅ CPU usage: $CPU_USAGE%"
elif [ "$CPU_USAGE" -lt 85 ]; then
    echo "⚠️  WARNING: CPU usage: $CPU_USAGE%"
else
    echo "❌ CRITICAL: CPU usage: $CPU_USAGE%"
    ((ERRORS++))
fi

# 10. Check Network Connectivity
echo ""
echo "🌐 Network:"
if ping -c 1 8.8.8.8 > /dev/null 2>&1; then
    echo "✅ Internet connectivity OK"
else
    echo "⚠️  WARNING: Internet connectivity issue (Judge0 may fail)"
fi

# Final Report
echo ""
echo "================================"
if [ "$ERRORS" -eq 0 ]; then
    echo "🚀 SYSTEM READY FOR EXAM"
    echo "✅ All checks passed"
    exit 0
else
    echo "🚨 SYSTEM NOT READY"
    echo "❌ $ERRORS critical issues detected"
    echo ""
    echo "DO NOT START THE EXAM until issues are resolved!"
    echo "Check logs: docker compose -f docker-compose.production.yml logs [service]"
    exit 1
fi

# Production Operations - Quick Reference

## 🚨 Emergency Scripts

### Before Exam (1 hour before)
```bash
cd /path/to/exam-portal
./scripts/health-check.sh
```
**Expected Output:** `🚀 SYSTEM READY FOR EXAM`

**If errors detected:** DO NOT START THE EXAM until resolved!

---

## Emergency Procedures

### 1. RabbitMQ Frozen (WebSocket Issues)
**Symptoms:** Students see "Reconnecting...", moderator dashboard frozen

```bash
./scripts/restart-broker.sh
```
**Impact:** Disconnects all WebSocket clients (students must refresh)
**Duration:** ~30 seconds

---

### 2. Database Locked/Slow
**Symptoms:** API timeouts, 500 errors

```bash
# Check for blocking queries
docker exec exam-portal-postgres psql -U exam_portal_user -d exam_portal_db -c "
SELECT pid, usename, state, query 
FROM pg_stat_activity 
WHERE state != 'idle' 
ORDER BY query_start;
"

# Kill specific connection if needed
docker exec exam-portal-postgres psql -U exam_portal_user -d exam_portal_db -c "SELECT pg_terminate_backend(PID);"
```

---

### 3. Disk Space Full
**Symptoms:** Elasticsearch red, PostgreSQL can't write

```bash
# Check disk usage
df -h
docker system df

# Emergency cleanup
./scripts/cleanup-disk.sh  # (create this script)

# Remove old Elasticsearch logs (older than 7 days)
curl -XDELETE "http://localhost:9200/app-logs-$(date -d '7 days ago' +%Y.%m.%d)"
```

---

### 4. Judge0 Workers Exhausted
**Symptoms:** Code submissions stuck in "PROCESSING"

```bash
# Check queue depth
curl http://localhost:2358/queue/length

# Restart Judge0
docker compose -f docker-compose.production.yml restart judge0
```

---

## Monitoring Dashboards

| Service | URL | Credentials |
|---------|-----|-------------|
| Grafana | http://localhost:3000 | admin / ${GRAFANA_ADMIN_PASSWORD} |
| Prometheus | http://localhost:9090 | None |
| Kibana | http://localhost:5601 | None |
| RabbitMQ | http://localhost:15672 | ${RABBITMQ_USERNAME} / ${RABBITMQ_PASSWORD} |

---

## Daily Backup

**Automated via cron:**
```bash
0 2 * * * /path/to/scripts/backup.sh >> /var/log/exam-portal-backup.log 2>&1
```

**Manual backup:**
```bash
./scripts/backup.sh
```

**Backup location:** `./backups/postgres/exam_portal_db_YYYYMMDD_HHMMSS.sql.gz`

**Retention:** Last 7 days locally

---

## Restore from Backup

```bash
# List available backups
ls -lh ./backups/postgres/

# Restore specific backup
BACKUP_FILE="exam_portal_db_20251231_020000.sql.gz"
gunzip -c ./backups/postgres/$BACKUP_FILE | docker exec -i exam-portal-postgres psql -U exam_portal_user -d exam_portal_db
```

---

## Service Management

```bash
# View all services
docker compose -f docker-compose.production.yml ps

# View logs (last 100 lines, follow)
docker compose -f docker-compose.production.yml logs -f --tail=100 [service_name]

# Restart specific service
docker compose -f docker-compose.production.yml restart [service_name]

# Stop all services
docker compose -f docker-compose.production.yml down

# Start all services
docker compose -f docker-compose.production.yml up -d
```

---

## Hardware Requirements

**Minimum (100 students):**
- 4 vCPUs
- 16 GB RAM
- 50 GB SSD
- Cost: ~$140/month (AWS t3.xlarge)

**Recommended (500 students):**
- 8 vCPUs
- 32 GB RAM
- 100 GB SSD
- Cost: ~$280/month (AWS t3.2xlarge)

---

## Critical Volume Mappings

Verified in `docker-compose.production.yml`:

✅ **postgres-data** → `/var/lib/postgresql/data`
✅ **redis-data** → `/data`
✅ **elasticsearch-data** → `/usr/share/elasticsearch/data`
✅ **judge0-data** → `/var/lib/judge0/data`
✅ **evidence** → `./evidence:/app/evidence` (host-mounted)

**All exam data survives container restarts!**

---

## Support Contacts

- **DevOps Lead:** devops@example.com
- **Database Admin:** dba@example.com
- **On-Call (24/7):** +1-555-0100

---

## Pre-Go-Live Checklist

- [ ] Run `./scripts/health-check.sh` → All green
- [ ] Verify backup cron job configured
- [ ] Test backup/restore cycle
- [ ] Verify SSL certificates installed
- [ ] Check disk space > 30% free
- [ ] Verify Grafana dashboards accessible
- [ ] Test emergency restart scripts
- [ ] Document escalation procedure
- [ ] Configure alert notifications
- [ ] Load test with 500 concurrent users

---

**Last Updated:** 2025-12-31

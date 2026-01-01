# Backend Setup Guide

## Phase 1: Infrastructure Setup Complete ✓

### What's Configured:

1. **PostgreSQL** (port 5432)
   - HikariCP connection pool (max 20 connections)
   - ACID-compliant transactions

2. **Redis** (port 6379)
   - Atomic violation counters
   - Session storage
   - Execution queue

3. **RabbitMQ** (ports 5672, 15672)
   - STOMP broker relay for WebSockets
   - Horizontal scaling support
   - Management UI at http://localhost:15672

4. **Judge0** (port 2358)
   - 2 worker instances (4 workers each)
   - Security sandboxing configured
   - Resource limits: 256MB RAM, 5s CPU, 10s wall time

5. **Spring Boot 3.2**
   - Web, JPA, Security, WebSocket
   - Resilience4j circuit breaker
   - Feign client for Judge0
   - ANTLR 4 runtime

---

## Quick Start

### 1. Start Infrastructure
```bash
cd backend
docker-compose up -d
```

**Verify services:**
```bash
# Check all containers are running
docker-compose ps

# Expected output: postgres, redis, rabbitmq, judge0-server, judge0-workers (healthy)
```

### 2. Access Management UIs

- **RabbitMQ Management**: http://localhost:15672
  - Username: `exam_user`
  - Password: `exam_password`
  - Check queues and connections

- **Judge0 Health**: http://localhost:2358/health
  - Should return: `{"status":"ok"}`

### 3. Build Backend
```bash
mvn clean install
```

**This will:**
- Download dependencies
- Compile ANTLR grammars (when we add them in Phase 3)
- Run tests

### 4. Run Application
```bash
mvn spring-boot:run
```

**Verify:**
- Application starts on port 8080
- Prometheus metrics: http://localhost:8080/actuator/prometheus
- Health check: http://localhost:8080/actuator/health

---

## Architecture Highlights

### RabbitMQ for WebSocket Scaling
```
Student (Server A) ─┐
                    ├─> RabbitMQ ─> Broadcast to all servers
Student (Server B) ─┘
```
**Power Move:** No in-memory session coupling. Scale to 10 instances seamlessly.

### Redis Atomic Counters
```java
// Race-condition free violation counting
redisTemplate.opsForValue().increment("exam:session:123:violations");
```
**Power Move:** 2 simultaneous violations = 2 strikes, not 1 (no race condition).

### Resilience4j Circuit Breaker
```
Judge0 fails 5/10 times → Circuit opens → 
Fallback: "Execution service temporarily unavailable"
```
**Power Move:** Backend stays alive even if Judge0 crashes.

---

## Next Steps (Phase 2)

- [ ] JWT authentication with department claims
- [ ] Spring Security configuration
- [ ] Role hierarchy (STUDENT/MODERATOR/ADMIN)
- [ ] Department-level RBAC with row-level security

---

## Troubleshooting

### Docker Compose Issues
```bash
# View logs
docker-compose logs -f <service-name>

# Restart specific service
docker-compose restart judge0-workers

# Clean restart
docker-compose down -v
docker-compose up -d
```

### Port Conflicts
If ports are in use, edit `docker-compose.yml`:
```yaml
ports:
  - "5433:5432"  # Change 5432 to 5433
```

### Database Connection Issues
Check `application.yml` connection string matches docker-compose:
```yaml
spring.datasource.url: jdbc:postgresql://localhost:5432/exam_portal_db
```

---

**Status:** Phase 1 Complete - Infrastructure Ready 🚀

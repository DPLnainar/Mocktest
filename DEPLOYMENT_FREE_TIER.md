# Free-Tier Cloud Deployment Guide

## Overview

Deploy the complete exam portal using **100% FREE** cloud services. Perfect for development, testing, or small-scale production (up to 50 concurrent students).

**Total Cost: $0/month** (within free tier limits)

---

## Architecture: Cloud-Native Free Tier

```
┌─────────────────────────────────────────────────────────────┐
│                      INTERNET                               │
└───────────────────────┬─────────────────────────────────────┘
                        │
         ┌──────────────┼──────────────┐
         │              │              │
    ┌────▼────┐   ┌────▼────┐   ┌────▼────┐
    │ Vercel  │   │ Render  │   │RapidAPI │
    │ (React) │   │(Spring) │   │(Judge0) │
    └─────────┘   └────┬────┘   └─────────┘
                       │
         ┌─────────────┼─────────────┐
         │             │             │
    ┌────▼────┐   ┌───▼────┐   ┌───▼────┐
    │  Neon   │   │Upstash │   │Upstash │
    │(Postgres)│  │(Redis) │   │(Kafka) │
    └─────────┘   └────────┘   └────────┘
```

---

## Service Comparison

| Component | Free Provider | Limits | Self-Hosted Equivalent |
|-----------|---------------|--------|------------------------|
| **Frontend** | Vercel / Netlify | Unlimited bandwidth | Nginx + React |
| **Backend** | Render / Railway | 512MB RAM, sleeps after 15min inactivity | Spring Boot Docker |
| **Database** | Neon.tech / Supabase | 500MB storage, 10 connections | PostgreSQL 15 |
| **Cache** | Upstash Redis | 10,000 commands/day | Redis 7 |
| **Queue** | Upstash Kafka | 10,000 messages/day | RabbitMQ 3.12 |
| **Code Execution** | RapidAPI Judge0 | 50 requests/day | Judge0 self-hosted |
| **Monitoring** | Vercel Analytics | Basic metrics | Prometheus + Grafana |

---

## 🚀 Deployment Steps

### 1. Frontend Deployment (Vercel)

**Provider:** Vercel (Recommended) or Netlify

**Setup:**

1. **Push to GitHub:**
```bash
cd frontend
git init
git add .
git commit -m "Initial frontend"
git remote add origin https://github.com/yourusername/exam-portal-frontend.git
git push -u origin main
```

2. **Deploy on Vercel:**
   - Go to [vercel.com](https://vercel.com)
   - Click "Import Project"
   - Select your GitHub repository
   - Configure build settings:
     - **Framework Preset:** Vite
     - **Build Command:** `npm run build`
     - **Output Directory:** `dist`
     - **Install Command:** `npm install`

3. **Environment Variables** (Vercel Dashboard):
```env
VITE_API_URL=https://your-backend.onrender.com
VITE_WS_URL=wss://your-backend.onrender.com/ws
```

4. **Deploy:** Click "Deploy" - live in ~2 minutes!

**Limits:**
- ✅ Unlimited bandwidth (100GB/month fair use)
- ✅ Automatic HTTPS with SSL
- ✅ Global CDN (instant worldwide access)
- ✅ Automatic deployments on Git push

**URL:** `https://exam-portal.vercel.app`

---

### 2. Backend Deployment (Render)

**Provider:** Render (Recommended) or Railway

**Setup:**

1. **Create `render.yaml` in backend root:**
```yaml
services:
  - type: web
    name: exam-portal-backend
    runtime: java
    buildCommand: "./mvnw clean package -DskipTests"
    startCommand: "java -jar target/*.jar"
    envVars:
      - key: SPRING_PROFILES_ACTIVE
        value: production
      - key: SPRING_DATASOURCE_URL
        sync: false
      - key: SPRING_DATASOURCE_USERNAME
        sync: false
      - key: SPRING_DATASOURCE_PASSWORD
        sync: false
      - key: SPRING_REDIS_HOST
        sync: false
      - key: SPRING_REDIS_PORT
        value: 6379
      - key: JWT_SECRET
        generateValue: true
    healthCheckPath: /actuator/health
```

2. **Deploy on Render:**
   - Go to [render.com](https://render.com)
   - Click "New +" → "Web Service"
   - Connect GitHub repository
   - Render auto-detects `render.yaml`
   - Click "Create Web Service"

3. **Configure Environment Variables** (Render Dashboard):
```env
SPRING_DATASOURCE_URL=jdbc:postgresql://your-db.neon.tech/exam_portal_db?sslmode=require
SPRING_DATASOURCE_USERNAME=your_neon_username
SPRING_DATASOURCE_PASSWORD=your_neon_password
SPRING_REDIS_HOST=your-redis.upstash.io
SPRING_REDIS_PASSWORD=your_upstash_token
JUDGE0_BASE_URL=https://judge0-ce.p.rapidapi.com
RAPIDAPI_KEY=your_rapidapi_key
```

**Limits:**
- ✅ 512MB RAM (sufficient for 50 concurrent students)
- ⚠️ Sleeps after 15 minutes of inactivity (30s cold start)
- ✅ Automatic HTTPS with SSL
- ✅ Automatic deployments on Git push

**Workaround for sleep:** Use [UptimeRobot](https://uptimerobot.com) (free) to ping `/actuator/health` every 5 minutes

**URL:** `https://exam-portal-backend.onrender.com`

---

### 3. Database (Neon.tech)

**Provider:** Neon.tech (Recommended) or Supabase

**Setup:**

1. **Create Database:**
   - Go to [neon.tech](https://neon.tech)
   - Click "Create Project"
   - Name: `exam-portal-db`
   - Region: Choose closest to your backend

2. **Get Connection String:**
```
postgresql://username:password@ep-xxx.region.aws.neon.tech/exam_portal_db?sslmode=require
```

3. **Run Migrations:**

Create `application-cloud.yml` in `backend/src/main/resources/`:
```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
  flyway:
    enabled: true
    locations: classpath:db/migration
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
```

Migrations will run automatically on first deployment.

**Limits:**
- ✅ 500MB storage (stores ~100,000 violations)
- ✅ 10 concurrent connections (sufficient with HikariCP pool)
- ✅ Automatic daily backups (7-day retention)
- ✅ 1 database, unlimited tables

**Migration Path:** Upgrade to $19/month for 10GB storage when needed

---

### 4. Redis Cache (Upstash)

**Provider:** Upstash Redis

**Setup:**

1. **Create Database:**
   - Go to [upstash.com](https://upstash.com)
   - Click "Create Database"
   - Name: `exam-portal-cache`
   - Type: Regional (faster) or Global
   - Enable TLS: Yes

2. **Get Connection Details:**
```
Host: your-redis.upstash.io
Port: 6379
Password: <your-token>
```

3. **Configure Backend** (`application-cloud.yml`):
```yaml
spring:
  redis:
    host: ${SPRING_REDIS_HOST}
    port: 6379
    password: ${SPRING_REDIS_PASSWORD}
    ssl: true
    timeout: 2000ms
  cache:
    type: redis
    redis:
      time-to-live: 3600000
```

**Limits:**
- ✅ 10,000 commands/day (320 commands/hour)
- ✅ 256MB max dataset size
- ✅ TLS encryption
- ⚠️ Rate limiting (use for session cache only, not strike counters)

**Optimization:** Use PostgreSQL for strike counters instead of Redis to avoid hitting command limits

---

### 5. Message Queue (Upstash Kafka)

**Provider:** Upstash Kafka (replaces RabbitMQ)

**Setup:**

1. **Create Cluster:**
   - Go to [upstash.com/kafka](https://console.upstash.com/kafka)
   - Click "Create Cluster"
   - Name: `exam-portal-events`

2. **Create Topics:**
   - `violation-events`
   - `termination-events`
   - `student-status`

3. **Update Backend Dependencies** (`pom.xml`):
```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

4. **Configure Kafka** (`application-cloud.yml`):
```yaml
spring:
  kafka:
    bootstrap-servers: ${UPSTASH_KAFKA_BOOTSTRAP}
    properties:
      sasl.mechanism: SCRAM-SHA-256
      security.protocol: SASL_SSL
      sasl.jasl.config: org.apache.kafka.common.security.scram.ScramLoginModule required username="${UPSTASH_KAFKA_USERNAME}" password="${UPSTASH_KAFKA_PASSWORD}";
```

**Limits:**
- ✅ 10,000 messages/day
- ✅ 1 MB message size
- ⚠️ 1 day retention

**Alternative:** Use WebSocket only (no queue) to avoid message limits for small deployments

---

### 6. Code Execution (RapidAPI Judge0)

**Provider:** RapidAPI Judge0 CE

**Setup:**

1. **Subscribe to API:**
   - Go to [rapidapi.com/judge0-official/api/judge0-ce](https://rapidapi.com/judge0-official/api/judge0-ce)
   - Click "Subscribe to Test"
   - Select "Basic" plan (free)

2. **Get API Key:**
```
X-RapidAPI-Key: your_rapidapi_key
X-RapidAPI-Host: judge0-ce.p.rapidapi.com
```

3. **Update Judge0Service.java:**
```java
@Service
public class Judge0Service {
    @Value("${judge0.base-url}")
    private String baseUrl;
    
    @Value("${rapidapi.key}")
    private String rapidApiKey;
    
    public SubmissionResult executeCode(CodeSubmission submission) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-RapidAPI-Key", rapidApiKey);
        headers.set("X-RapidAPI-Host", "judge0-ce.p.rapidapi.com");
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // Create submission
        String submissionUrl = baseUrl + "/submissions?base64_encoded=true&wait=false";
        // ... rest of implementation
    }
}
```

4. **Environment Variables:**
```env
JUDGE0_BASE_URL=https://judge0-ce.p.rapidapi.com
RAPIDAPI_KEY=your_rapidapi_key
```

**Limits:**
- ⚠️ 50 requests/day (free tier)
- ⚠️ 2 second CPU time limit
- ✅ Supports Java, Python, C, C++

**Upgrade:** $10/month for 500 requests/day (sufficient for 100 students with 5 submissions each)

**Alternative:** Self-host Judge0 on Render for unlimited executions (uses 256MB RAM)

---

## 📊 Cost Analysis

### Free Tier Capacity

| Metric | Free Tier Limit | Usage per Student | Max Students |
|--------|----------------|-------------------|--------------|
| **Backend RAM** | 512MB | 5MB avg | 100 students |
| **Database Storage** | 500MB | 2MB avg | 250 students |
| **Redis Commands** | 10,000/day | 20/hour | 50 concurrent |
| **Judge0 Requests** | 50/day | 5 per exam | 10 students/day |
| **Kafka Messages** | 10,000/day | 50/exam | 200 students/day |

**Recommendation:** Free tier supports **up to 50 concurrent students** or **10 exams per day** (500 students/month)

---

## 🔧 Configuration Changes for Cloud

### 1. Update `application-cloud.yml`

```yaml
spring:
  profiles: cloud
  
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
    hikari:
      maximum-pool-size: 10  # Neon limit
      minimum-idle: 2
      connection-timeout: 30000
  
  redis:
    host: ${SPRING_REDIS_HOST}
    port: 6379
    password: ${SPRING_REDIS_PASSWORD}
    ssl: true
    lettuce:
      pool:
        max-active: 5  # Reduced for free tier
  
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        jdbc:
          batch_size: 10  # Reduced batch size
        order_inserts: true
        
judge0:
  base-url: ${JUDGE0_BASE_URL}
  
rapidapi:
  key: ${RAPIDAPI_KEY}
  
server:
  port: ${PORT:8080}  # Render sets PORT automatically
```

### 2. Remove Redis Strike Counter (Use PostgreSQL)

**Problem:** Upstash free tier = 10,000 commands/day. Strike counter uses INCR (1 command per violation).

**Solution:** Use PostgreSQL atomic updates instead:

```java
@Service
public class ViolationTrackingService {
    
    @Transactional
    public int incrementStrikes(Long sessionId) {
        // Use PostgreSQL instead of Redis
        int strikes = violationRepository.countBySessionIdAndConfirmed(sessionId, true);
        
        if (strikes >= 5) {
            terminateSession(sessionId);
        }
        
        return strikes;
    }
}
```

### 3. Update Frontend Environment Variables

**Create `.env.production`:**
```env
VITE_API_URL=https://exam-portal-backend.onrender.com
VITE_WS_URL=wss://exam-portal-backend.onrender.com/ws
```

### 4. Handle Cold Starts (Render Sleep)

**Problem:** Render free tier sleeps after 15 minutes → 30s cold start

**Solution 1:** UptimeRobot Ping (Recommended)
1. Go to [uptimerobot.com](https://uptimerobot.com)
2. Create monitor: `https://exam-portal-backend.onrender.com/actuator/health`
3. Interval: 5 minutes
4. Keeps backend awake 24/7

**Solution 2:** Frontend Loading State
```javascript
// In React App.js
const [backendWaking, setBackendWaking] = useState(false);

useEffect(() => {
  fetch(`${API_URL}/actuator/health`)
    .then(() => setBackendWaking(false))
    .catch(() => {
      setBackendWaking(true);
      // Retry in 5 seconds
      setTimeout(() => checkHealth(), 5000);
    });
}, []);

if (backendWaking) {
  return <LoadingSpinner message="Waking up backend (cold start)..." />;
}
```

---

## 🚀 Complete Deployment Checklist

### Phase 1: Database Setup
- [ ] Create Neon.tech account
- [ ] Create database `exam_portal_db`
- [ ] Copy connection string
- [ ] Test connection: `psql <connection-string>`

### Phase 2: Cache Setup
- [ ] Create Upstash Redis database
- [ ] Copy host and password
- [ ] Test connection: `redis-cli -h <host> -p 6379 --tls -a <password>`

### Phase 3: Backend Deployment
- [ ] Push backend to GitHub
- [ ] Create Render web service
- [ ] Configure environment variables
- [ ] Wait for build (~10 minutes)
- [ ] Test health: `curl https://your-backend.onrender.com/actuator/health`
- [ ] Setup UptimeRobot monitor

### Phase 4: Frontend Deployment
- [ ] Push frontend to GitHub
- [ ] Create Vercel project
- [ ] Configure environment variables (`VITE_API_URL`, `VITE_WS_URL`)
- [ ] Deploy (~2 minutes)
- [ ] Test: Open `https://your-app.vercel.app`

### Phase 5: Judge0 Setup
- [ ] Subscribe to RapidAPI Judge0 CE
- [ ] Copy API key
- [ ] Update backend environment variable `RAPIDAPI_KEY`
- [ ] Test code execution

### Phase 6: Testing
- [ ] Register test user
- [ ] Create test exam
- [ ] Submit code (check Judge0 works)
- [ ] Test violation detection
- [ ] Verify WebSocket updates

---

## 🔍 Monitoring (Free Tier)

### 1. Vercel Analytics
- Built-in page views, performance metrics
- Free tier: 100,000 events/month

### 2. Render Metrics
- Built-in CPU, memory, response time graphs
- Free tier: 7 days retention

### 3. Upstash Console
- Redis command rate, memory usage
- Kafka message throughput

### 4. Neon.tech Dashboard
- Database size, connection count
- Query performance

**No Prometheus/Grafana needed** - each provider has built-in monitoring!

---

## ⚠️ Limitations & Workarounds

| Limitation | Impact | Workaround |
|------------|--------|-----------|
| **Render Sleep** | 30s cold start | UptimeRobot ping every 5 min |
| **Judge0 50/day** | Only 10 students/day | Upgrade to $10/month (500/day) or self-host |
| **Redis 10k commands/day** | ~400 commands/hour | Use PostgreSQL for strike counters |
| **Kafka 10k messages/day** | ~400 messages/hour | Use WebSocket only (no queue) |
| **Neon 500MB** | ~100k violations | Archive old data monthly |
| **No RabbitMQ** | N/A | Use Upstash Kafka or WebSocket broadcasting |

---

## 💰 Upgrade Path

When you outgrow free tier (>50 concurrent students):

| Component | Upgrade To | Cost |
|-----------|-----------|------|
| Backend | Render Standard (1GB RAM) | $7/month |
| Database | Neon Pro (10GB) | $19/month |
| Redis | Upstash Pro (1M commands/day) | $10/month |
| Judge0 | RapidAPI Pro (500/day) | $10/month |
| **Total** | | **$46/month** |

Still 84% cheaper than self-hosted VPS ($280/month for 500 students)!

---

## 🎯 Free vs Self-Hosted Comparison

| Criteria | Free Tier Cloud | Self-Hosted Docker |
|----------|----------------|-------------------|
| **Setup Time** | 2 hours | 8 hours |
| **Cost** | $0 | $140-280/month |
| **Capacity** | 50 concurrent students | 500 concurrent students |
| **Maintenance** | Zero (managed) | High (OS updates, security) |
| **Scaling** | Automatic | Manual (add servers) |
| **Monitoring** | Built-in dashboards | Setup Prometheus/Grafana |
| **Backups** | Automatic | Manual scripts |
| **SSL** | Automatic | Configure Let's Encrypt |
| **Cold Starts** | 30s (Render sleep) | None |
| **Uptime** | 99.9% SLA | Depends on VPS |

**Recommendation:**
- **Development/Testing:** Free tier cloud (faster setup, zero cost)
- **Production (<50 students):** Free tier cloud (sufficient, managed)
- **Production (50-200 students):** Paid cloud ($46/month)
- **Production (200+ students):** Self-hosted Docker ($280/month)

---

## 📚 Additional Resources

- **Vercel Docs:** https://vercel.com/docs
- **Render Docs:** https://render.com/docs
- **Neon Docs:** https://neon.tech/docs
- **Upstash Docs:** https://upstash.com/docs
- **RapidAPI Judge0:** https://rapidapi.com/judge0-official/api/judge0-ce

---

## 🆘 Troubleshooting

### Backend Won't Start
```bash
# Check Render logs
# Dashboard → Logs tab

# Common issues:
# 1. Missing environment variables
# 2. Database connection failed (check Neon connection string)
# 3. Build timeout (upgrade to paid tier for faster builds)
```

### Database Connection Timeout
```yaml
# Update application-cloud.yml
spring:
  datasource:
    hikari:
      connection-timeout: 60000  # Increase to 60s
      validation-timeout: 5000
```

### Redis Connection Failed
```bash
# Verify SSL/TLS enabled
spring:
  redis:
    ssl: true  # MUST be true for Upstash
```

### Judge0 Rate Limit
```java
// Add rate limiting in ViolationController
@RateLimiter(name = "judge0", fallbackMethod = "judgeFallback")
public ResponseEntity<?> executeCode(@RequestBody CodeSubmission submission) {
    // ...
}

public ResponseEntity<?> judgeFallback(CodeSubmission submission, Exception e) {
    return ResponseEntity.status(429).body("Daily Judge0 limit reached. Try again tomorrow.");
}
```

---

**Ready to deploy?** Start with Phase 1 (Database Setup) and work through the checklist!

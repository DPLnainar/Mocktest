# Backend Startup Guide

## Quick Answer: Why Backend Doesn't Work After Shutdown

Your backend runs in **Docker containers**, not directly on Windows. When you shutdown your computer, the Docker containers stop and need to be restarted manually.

**Maven is NOT installed on your system** - it only exists inside the Docker containers.

---

## How to Start Backend After Reboot

### Option 1: Use the Startup Script (Easiest)

Simply run this script:

```powershell
.\start-backend.ps1
```

### Option 2: Manual Docker Command

```powershell
cd "c:\Users\ganes\OneDrive\Desktop\mock test\backend"
docker-compose up -d
```

---

## What's Running in Docker?

Your backend consists of multiple services:

| Service | Port | Purpose |
|---------|------|---------|
| Backend API | 8080 | Main Spring Boot application |
| PostgreSQL | 5432 | Database |
| Redis | 6379 | Caching & sessions |
| RabbitMQ | 5672, 15672 | Message broker for WebSockets |
| Judge0 | 2358 | Code execution engine |

---

## Common Commands

### Start all services
```powershell
docker-compose up -d
```

### Stop all services
```powershell
docker-compose down
```

### View logs
```powershell
docker-compose logs -f backend
```

### Restart backend only
```powershell
docker restart exam-portal-backend
```

### Check running containers
```powershell
docker ps
```

---

## Do You Need Maven Installed?

**No!** Maven is already inside the Docker container. You only need:

1. ✅ **Docker Desktop** (already installed)
2. ✅ **Node.js & npm** (for frontend)

---

## Troubleshooting

### Backend won't start after reboot?

1. Make sure Docker Desktop is running
2. Run: `docker-compose up -d`
3. Wait 20-30 seconds for all services to start
4. Check logs: `docker logs exam-portal-backend`

### Port 8080 already in use?

Another instance might be running:
```powershell
docker-compose down
docker-compose up -d
```

### Database connection errors?

Wait for PostgreSQL to be healthy:
```powershell
docker ps  # Check if postgres shows "healthy"
```

---

## Auto-Start on Reboot (Optional)

If you want Docker containers to start automatically when you boot your computer:

1. Make sure Docker Desktop is set to start on Windows startup
2. The containers are already configured with `restart: unless-stopped`
3. They will auto-start when Docker Desktop starts

---

## Frontend Startup

The frontend is separate and runs with npm:

```powershell
cd "c:\Users\ganes\OneDrive\Desktop\mock test\frontend"
npm run dev
```

---

## Full Startup Sequence After Reboot

1. **Start Docker Desktop** (if not auto-started)
2. **Start Backend**: Run `.\start-backend.ps1` or `docker-compose up -d`
3. **Start Frontend**: Run `npm run dev` in the frontend folder
4. **Access Application**: Open http://localhost:5173

---

## Need to Install Maven Locally?

**Not recommended**, but if you really want to run without Docker:

1. Install Maven from https://maven.apache.org/download.cgi
2. Install PostgreSQL, Redis, RabbitMQ locally
3. Update `application.properties` with local connection strings
4. Run: `mvn spring-boot:run`

**Docker is much easier!** 🐳

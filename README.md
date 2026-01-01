# Enterprise Examination & Placement Portal

A fortress-grade, real-time examination platform with logic verification, live proctoring, and zero-tolerance security.

## ✅ Phase 7 Complete - Full-Stack Implementation

**Student Exam Interface:** Monaco Editor + TensorFlow.js Camera + IndexedDB Auto-Save + WebSocket Real-Time + Offline Support

See [PHASE_7_REACT_INTERFACE.md](PHASE_7_REACT_INTERFACE.md) for complete frontend documentation.

## 🏗️ Architecture

### Backend Stack
- **Spring Boot 3.2** (Java 17+) - High concurrency & stability
- **Spring Security + JWT** - Stateless authentication
- **ANTLR 4** - Code logic verification (USP)
- **WebSocket (STOMP + RabbitMQ)** - Real-time monitoring
- **PostgreSQL** - ACID-compliant data integrity
- **Redis** - Atomic counters & session storage
- **Judge0** - Sandboxed code execution

### Frontend Stack
- **React 18 + Vite** - Fast, responsive UI
- **Monaco Editor** - VS Code-like coding experience
- **TensorFlow.js (Coco-SSD)** - Client-side AI detection
- **IndexedDB** - Offline-first resilience

## 🚀 Key Features

### 1. Logic Integrity Verification
Uses ANTLR parsers to detect forbidden constructs (e.g., `.sort()` when Bubble Sort required)

### 2. Real-Time Proctoring
- Live moderator dashboard with <1s latency
- Color-coded student status (Green/Yellow/Red)
- Automated violation detection

### 3. Security Fortress
- Tab switching detection
- AI-powered phone/multiple face detection
- Automated exam termination (5-strike rule)

### 4. Scalability
- Horizontal scaling with RabbitMQ
- Redis atomic counters for race-free violation tracking
- Connection pooling for 500+ concurrent students

### 5. Network Resilience
- IndexedDB offline-first architecture
- Auto-save every 5 seconds
- Zero data loss during Wi-Fi flickers

## 📂 Project Structure

```
exam-portal/
├── backend/           # Spring Boot application
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   └── antlr4/
│   │   └── resources/
│   ├── docker-compose.yml
│   └── pom.xml
│
└── frontend/          # React + Vite application
    ├── src/
    │   ├── components/
    │   ├── services/
    │   ├── hooks/
    │   └── utils/
    └── package.json
```

## 🔧 Setup Instructions

### Prerequisites
- Java 17+
- Node.js 18+
- Docker & Docker Compose
- PostgreSQL 15+

### Backend Setup
```bash
cd backend
mvn clean install
docker-compose up -d
mvn spring-boot:run
```

### Frontend Setup
```bash
cd frontend
npm install
npm run dev
```

## 🎯 Performance Targets

- **Parsing**: <200ms per submission
- **Monitoring Latency**: <1s for status updates
- **Auto-Save**: 5s intervals
- **Network Recovery**: <3s reconnection

## 🔐 Security Highlights

- Sandboxed code execution (seccomp, cgroups)
- Department-level data isolation
- Evidence-based violation snapshots (GDPR-compliant)
- Debounced false-positive detection

## 📊 Monitoring

- Prometheus + Grafana for metrics
- ELK Stack for centralized logging
- Real-time violation rate tracking

---

**Built for Universities and Companies demanding exam integrity at scale.**

# Phase 6: Violation System Architecture

## Complete System Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                             FRONTEND (Phase 7)                                  │
│  ┌─────────────────┐  ┌──────────────────┐  ┌────────────────────────────┐     │
│  │  Camera Monitor │  │  Visibility API  │  │  TensorFlow.js Detector   │     │
│  │  (10 FPS)       │  │  (Tab Switches)  │  │  (Object Detection)       │     │
│  └────────┬────────┘  └─────────┬────────┘  └─────────┬──────────────────┘     │
│           │                     │                      │                        │
│           └─────────────────────┴──────────────────────┘                        │
│                                 │                                               │
│                      Violation Detected                                         │
│                                 │                                               │
└─────────────────────────────────┼───────────────────────────────────────────────┘
                                  │
                                  ▼ HTTP POST
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         BACKEND - Spring Boot 3.2                               │
│                                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │  ViolationController                                                    │   │
│  │  POST /api/violations/report                                            │   │
│  │  - Validate JWT                                                         │   │
│  │  - Extract student ID                                                   │   │
│  │  - Call ViolationService                                                │   │
│  └────────────────────────────┬────────────────────────────────────────────┘   │
│                               │                                                 │
│                               ▼                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │  ViolationService.recordViolation()                                     │   │
│  │                                                                         │   │
│  │  Step 1: Redis INCR (ATOMIC - THE POWER MOVE)                          │   │
│  │  ┌─────────────────────────────────────────────────────────────┐       │   │
│  │  │  INCR exam:session:strikes:123                              │       │   │
│  │  │  Current: 2  →  New: 4  (added 2 for MAJOR severity)       │       │   │
│  │  │  SET TTL 4 hours                                            │       │   │
│  │  └─────────────────────────────────────────────────────────────┘       │   │
│  │                                                                         │   │
│  │  Step 2: Save to PostgreSQL                                            │   │
│  │  ┌─────────────────────────────────────────────────────────────┐       │   │
│  │  │  INSERT INTO violations (                                   │       │   │
│  │  │    session_id, student_id, type, severity,                 │       │   │
│  │  │    evidence (JSONB), strike_count, detected_at             │       │   │
│  │  │  ) VALUES (                                                │       │   │
│  │  │    123, 789, 'PHONE_DETECTED', 'MAJOR',                   │       │   │
│  │  │    '{"screenshot": "...", "confidence": 0.95}', 2, NOW()  │       │   │
│  │  │  )                                                         │       │   │
│  │  └─────────────────────────────────────────────────────────────┘       │   │
│  │                                                                         │   │
│  │  Step 3: Update Session Manager                                        │   │
│  │  ┌─────────────────────────────────────────────────────────────┐       │   │
│  │  │  SessionManager.updateViolationCount(sessionId, 4)          │       │   │
│  │  │  → Updates Redis session hash                               │       │   │
│  │  └─────────────────────────────────────────────────────────────┘       │   │
│  │                                                                         │   │
│  │  Step 4: Publish Event (Async)                                         │   │
│  │  ┌─────────────────────────────────────────────────────────────┐       │   │
│  │  │  ApplicationEventPublisher.publish(ViolationEvent)           │       │   │
│  │  │  → ViolationEventListener processes in background            │       │   │
│  │  │    - Log analytics                                           │       │   │
│  │  │    - Send notifications (future)                             │       │   │
│  │  │    - Archive to S3 (future)                                  │       │   │
│  │  └─────────────────────────────────────────────────────────────┘       │   │
│  │                                                                         │   │
│  │  Step 5: Broadcast to Moderators                                       │   │
│  │  ┌─────────────────────────────────────────────────────────────┐       │   │
│  │  │  MonitoringBroadcastService.broadcastViolationAlert()        │       │   │
│  │  │  → Send to /topic/exam/456/monitoring                        │       │   │
│  │  │                                                              │       │   │
│  │  │  MonitoringBroadcastService.broadcastStudentStatus()         │       │   │
│  │  │  → Update student tile with new strike count                │       │   │
│  │  │  → Color: GREEN (0-1) | YELLOW (2-3) | RED (4-5)           │       │   │
│  │  └─────────────────────────────────────────────────────────────┘       │   │
│  │                                                                         │   │
│  │  Step 6: Auto-Termination Check                                        │   │
│  │  ┌─────────────────────────────────────────────────────────────┐       │   │
│  │  │  if (strikeCount >= 5) {                                    │       │   │
│  │  │    SessionManager.terminateSession(sessionId);              │       │   │
│  │  │                                                              │       │   │
│  │  │    // Notify student                                        │       │   │
│  │  │    MonitoringBroadcastService.sendToStudent(                │       │   │
│  │  │      studentId, "termination",                              │       │   │
│  │  │      {reason: "5 strikes", terminatedAt: ...}               │       │   │
│  │  │    );                                                        │       │   │
│  │  │                                                              │       │   │
│  │  │    // Notify moderators                                     │       │   │
│  │  │    MonitoringBroadcastService.broadcastTermination(          │       │   │
│  │  │      examId, studentId, "Auto-termination: 5 strikes"       │       │   │
│  │  │    );                                                        │       │   │
│  │  │  }                                                           │       │   │
│  │  └─────────────────────────────────────────────────────────────┘       │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────────┘
                                  │
                                  │
        ┌─────────────────────────┼─────────────────────────┐
        │                         │                         │
        ▼                         ▼                         ▼
┌───────────────────┐  ┌─────────────────────┐  ┌─────────────────────┐
│   Redis           │  │   PostgreSQL        │  │   RabbitMQ          │
│                   │  │                     │  │                     │
│  Strike Counters: │  │  violations table:  │  │  WebSocket Broker:  │
│                   │  │                     │  │                     │
│  strikes:123 → 4  │  │  id | session_id   │  │  Topic Exchange:    │
│  strikes:124 → 1  │  │  1  | 123          │  │  /topic/exam/456/   │
│  strikes:125 → 0  │  │  2  | 123          │  │  monitoring         │
│                   │  │  3  | 124          │  │                     │
│  TTL: 4 hours     │  │  evidence (JSONB)   │  │  User Queue:        │
│  Atomic INCR      │  │  GIN index          │  │  /user/789/queue/   │
│                   │  │                     │  │  messages           │
└───────────────────┘  └─────────────────────┘  └─────────────────────┘
```

## Data Flow Timeline

```
T=0ms:    Frontend detects phone
          └─> TensorFlow.js confidence: 0.95

T=10ms:   POST /api/violations/report
          └─> JWT validation
          └─> Extract student ID: 789

T=15ms:   Redis INCR
          └─> ATOMIC: strikes:123  2 → 4
          └─> No locks needed
          └─> Race-free counting

T=25ms:   PostgreSQL INSERT
          └─> Evidence stored as JSONB
          └─> Indexes: session_id, student_id, exam_id
          └─> GIN index on evidence column

T=30ms:   Session Manager Update
          └─> Redis session hash
          └─> violationCount: 4

T=35ms:   Event Published
          └─> Async handler processes in background
          └─> Non-blocking

T=40ms:   WebSocket Broadcast
          └─> RabbitMQ topic exchange
          └─> All moderators receive update
          └─> <1ms to reach clients

T=45ms:   Auto-Termination Check
          └─> if (4 < 5) continue
          └─> if (5 >= 5) terminate

T=50ms:   Response to Student
          └─> { strikeCount: 4, terminated: false }

Total:    50ms from detection to notification
```

## Redis Atomic Counter - The Power Move

### Why Redis INCR?

**Problem:** Race conditions in concurrent violation detection

```
Scenario: Phone + Tab Switch detected at exact same millisecond

❌ DATABASE APPROACH (LOST UPDATE):
  Thread 1: SELECT count FROM counters WHERE session_id=123  → 2
  Thread 2: SELECT count FROM counters WHERE session_id=123  → 2
  Thread 1: UPDATE counters SET count=4 WHERE session_id=123  (2+2)
  Thread 2: UPDATE counters SET count=4 WHERE session_id=123  (2+2)
  Result: 4 strikes (WRONG! Should be 6)

✅ REDIS INCR APPROACH (ATOMIC):
  Thread 1: INCR strikes:123 2  → Returns 4
  Thread 2: INCR strikes:123 2  → Returns 6
  Result: 6 strikes (CORRECT!)
```

### Code Implementation

```java
// ViolationService.java
String redisKey = "exam:session:strikes:" + sessionId;

// ATOMIC - No locks, no transactions, no race conditions
Long newStrikeCount = redisTemplate.opsForValue()
    .increment(redisKey, severity.getStrikeCount());

// If phone (2 strikes) + tab switch (2 strikes) happen simultaneously:
// Redis guarantees both are counted → 2 → 4 → 6
```

## Violation Severity System

```java
public enum Severity {
    MINOR(1),      // 1 strike - Brief face absence, look away
    MAJOR(2),      // 2 strikes - Phone, multiple faces, tab switch
    CRITICAL(5);   // 5 strikes - AI IDE, copy/paste from external source
}
```

### Examples

| Violation Type       | Severity | Strikes | Action                  |
|---------------------|----------|---------|-------------------------|
| NO_FACE_DETECTED    | MINOR    | 1       | Warning                 |
| TAB_SWITCH          | MAJOR    | 2       | Yellow alert            |
| PHONE_DETECTED      | MAJOR    | 2       | Yellow alert            |
| MULTIPLE_FACES      | MAJOR    | 2       | Yellow alert            |
| COPY_PASTE_DETECTED | CRITICAL | 5       | Immediate termination   |

### Auto-Termination Scenarios

```
Scenario 1: Gradual build-up
  TAB_SWITCH (2) + TAB_SWITCH (2) + NO_FACE (1) = 5 strikes → TERMINATED

Scenario 2: Critical violation
  COPY_PASTE_DETECTED (5) = 5 strikes → IMMEDIATE TERMINATION

Scenario 3: Multiple simultaneous
  PHONE (2) + MULTIPLE_FACES (2) + TAB_SWITCH (2) = 6 strikes → TERMINATED
```

## Evidence Storage (JSONB)

### Phone Detection
```json
{
  "screenshot": "data:image/png;base64,iVBORw0KGgo...",
  "confidence": 0.95,
  "detectedObject": "cell phone",
  "boundingBox": {
    "x": 100,
    "y": 200,
    "width": 50,
    "height": 80
  },
  "timestamp": "2025-12-31T10:30:00Z",
  "clientInfo": {
    "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
    "screenResolution": "1920x1080"
  }
}
```

### Tab Switch
```json
{
  "timestamp": "2025-12-31T10:30:00Z",
  "previousTab": "exam.html",
  "switchedTo": "google.com",
  "duration": "5 seconds"
}
```

### Multiple Faces
```json
{
  "faceCount": 2,
  "confidence": 0.92,
  "faces": [
    {"x": 100, "y": 50, "width": 80, "height": 100},
    {"x": 300, "y": 60, "width": 75, "height": 95}
  ],
  "screenshot": "base64..."
}
```

## WebSocket Broadcasting

### Moderator Receives Violation Alert
```javascript
// Subscribe to exam monitoring
stompClient.subscribe('/topic/exam/456/monitoring', (message) => {
  const update = JSON.parse(message.body);
  
  switch (update.type) {
    case 'violation_alert':
      // {
      //   "studentId": 789,
      //   "violationType": "PHONE_DETECTED",
      //   "message": "Cell phone detected (Total strikes: 4)",
      //   "timestamp": 1735639800000
      // }
      showViolationNotification(update.payload);
      break;
      
    case 'student_status':
      // Update student tile color
      updateStudentTile(update.payload.studentId, update.payload.statusColor);
      break;
      
    case 'termination':
      // Student auto-terminated
      markStudentTerminated(update.payload.studentId);
      break;
  }
});
```

### Student Receives Termination Notice
```javascript
stompClient.subscribe('/user/queue/messages', (message) => {
  const notification = JSON.parse(message.body);
  
  if (notification.type === 'termination') {
    // {
    //   "reason": "Automatic termination: 5 strikes",
    //   "terminatedAt": "2025-12-31T10:35:00",
    //   "strikes": 5
    // }
    
    // Disable exam interface
    disableCodeEditor();
    showTerminationModal(notification.payload);
    
    // Redirect after 5 seconds
    setTimeout(() => {
      window.location.href = '/exam-terminated';
    }, 5000);
  }
});
```

## False Positive Handling

### Moderator Review Flow
```
1. Moderator sees violation alert
   └─> Yellow tile on dashboard
   └─> "Phone detected - Confidence: 0.75"

2. Reviews evidence screenshot
   └─> Student was adjusting webcam
   └─> Not actually using phone

3. Rejects violation
   POST /api/violations/1/confirm
   { "confirmed": false, "reason": "Adjusting webcam" }

4. System adjusts:
   └─> Redis: DECR strikes:123 2  (4 → 2)
   └─> PostgreSQL: UPDATE violations SET confirmed=false
   └─> WebSocket: Broadcast status update (Yellow → Green)
```

## Performance Optimization

### Redis
- **INCR**: O(1) time complexity
- **Latency**: ~1ms
- **Atomic**: No locks needed
- **TTL**: Auto-expire after 4 hours

### PostgreSQL
- **Indexes**: session_id, student_id, exam_id
- **JSONB**: GIN index for evidence searching
- **Batch Insert**: Hibernate batch_size=20

### WebSocket
- **RabbitMQ**: Horizontal scaling
- **Topic Exchange**: Broadcast to all moderators
- **Latency**: <1ms to reach clients

## Security

### Authentication
```java
@PreAuthorize("hasRole('STUDENT')")
public ResponseEntity<ViolationResponse> reportViolation(
    @AuthenticationPrincipal CustomUserDetails student) {
    
    // Extract student ID from JWT (can't be spoofed)
    Long studentId = student.getId();
    
    // Record violation
    violationService.recordViolation(sessionId, studentId, ...);
}
```

### Department Isolation
```java
// Moderator can only see violations from their department
@PreAuthorize("hasRole('MODERATOR')")
public List<Violation> getExamViolations(Long examId) {
    return violationService.getExamViolations(examId)
        .stream()
        .filter(v -> securityService.hasDepartmentAccess(v.getDepartment()))
        .toList();
}
```

### Evidence Sanitization
```java
// Base64 validation
if (evidence.get("screenshot") != null) {
    String screenshot = (String) evidence.get("screenshot");
    if (!screenshot.matches("^data:image/(png|jpeg);base64,[A-Za-z0-9+/=]+$")) {
        throw new IllegalArgumentException("Invalid screenshot format");
    }
}
```

---

## Phase 6 Complete! ✅

**Implemented:**
- ✅ Redis atomic counters (race-free)
- ✅ PostgreSQL evidence storage (JSONB)
- ✅ Auto-termination at 5 strikes
- ✅ WebSocket broadcasting
- ✅ False positive handling
- ✅ Admin appeals
- ✅ Department isolation
- ✅ Comprehensive API

**Next Phase:** Frontend React interface with camera monitoring

# Violation System - API Reference

## Quick Start

### 1. Student Reports Violation
```javascript
// Student detected tab switch via frontend monitoring
const response = await fetch('http://localhost:8080/api/violations/report', {
  method: 'POST',
  headers: {
    'Authorization': 'Bearer ' + studentJWT,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    sessionId: 123,
    examId: 456,
    type: 'TAB_SWITCH',
    severity: 'MAJOR',
    description: 'Switched to Google Chrome',
    evidence: {
      timestamp: new Date().toISOString(),
      switchedTo: 'google.com'
    }
  })
});

const result = await response.json();
console.log(result);
// {
//   "strikeCount": 2,
//   "terminated": false,
//   "message": "Violation recorded. Total strikes: 2"
// }
```

### 2. Check Current Strike Count
```javascript
const response = await fetch('http://localhost:8080/api/violations/session/123/strikes', {
  headers: { 'Authorization': 'Bearer ' + jwt }
});

const strikes = await response.json();
console.log(strikes);
// {
//   "currentStrikes": 2,
//   "terminated": false,
//   "remainingStrikes": 3
// }
```

### 3. Moderator Views Session Violations
```javascript
const response = await fetch('http://localhost:8080/api/violations/session/123', {
  headers: { 'Authorization': 'Bearer ' + moderatorJWT }
});

const violations = await response.json();
console.log(violations);
// [
//   {
//     "id": 1,
//     "sessionId": 123,
//     "studentId": 789,
//     "type": "TAB_SWITCH",
//     "severity": "MAJOR",
//     "strikeCount": 2,
//     "confirmed": true,
//     "detectedAt": "2025-12-31T10:30:00",
//     "evidence": {...}
//   }
// ]
```

## Endpoints

### Student Endpoints

#### POST /api/violations/report
Report a detected violation.

**Auth:** Student role required  
**Request:**
```json
{
  "sessionId": 123,
  "examId": 456,
  "type": "PHONE_DETECTED",
  "severity": "MAJOR",
  "description": "Cell phone detected",
  "evidence": {
    "screenshot": "data:image/png;base64,...",
    "confidence": 0.95,
    "detectedObject": "cell phone",
    "boundingBox": {"x": 100, "y": 200, "width": 50, "height": 80}
  }
}
```

**Response:**
```json
{
  "strikeCount": 3,
  "terminated": false,
  "message": "Violation recorded. Total strikes: 3"
}
```

**Violation Types:**
- `MULTIPLE_FACES` - Multiple people detected
- `NO_FACE_DETECTED` - Face not visible
- `PHONE_DETECTED` - Mobile device detected
- `TAB_SWITCH` - Switched browser tab
- `WINDOW_BLUR` - Window lost focus
- `FULLSCREEN_EXIT` - Exited fullscreen mode
- `COPY_PASTE_DETECTED` - Code copy/paste detected
- `FORBIDDEN_CONSTRUCT` - Used forbidden code pattern
- `MANUAL_FLAG` - Manually flagged by moderator
- `SUSPICIOUS_ACTIVITY` - Other suspicious behavior

**Severity Levels:**
- `MINOR` - 1 strike (brief face absence)
- `MAJOR` - 2 strikes (phone, tab switch)
- `CRITICAL` - 5 strikes (immediate termination)

---

#### GET /api/violations/session/{sessionId}/strikes
Get current strike count.

**Auth:** Student/Moderator/Admin  
**Response:**
```json
{
  "currentStrikes": 2,
  "terminated": false,
  "remainingStrikes": 3
}
```

---

### Moderator Endpoints

#### GET /api/violations/session/{sessionId}
Get all violations for a session.

**Auth:** Moderator/Admin  
**Response:** Array of violation objects

---

#### GET /api/violations/student/{studentId}
Get all violations for a student (across all exams).

**Auth:** Moderator/Admin  
**Response:** Array of violation objects

---

#### GET /api/violations/exam/{examId}
Get all violations for an exam.

**Auth:** Moderator/Admin  
**Response:** Array of violation objects

---

#### GET /api/violations/session/{sessionId}/stats
Get violation statistics.

**Auth:** Moderator/Admin  
**Response:**
```json
{
  "totalStrikes": 5,
  "totalViolations": 3,
  "cameraViolations": 2,
  "tabSwitchCount": 1,
  "terminated": true
}
```

---

#### PUT /api/violations/{violationId}/confirm
Confirm or reject a violation (false positive handling).

**Auth:** Moderator/Admin  
**Request:**
```json
{
  "confirmed": false,
  "reason": "Student was adjusting webcam, not using phone"
}
```

**Response:**
```json
"Violation rejected"
```

---

### Admin Endpoints

#### POST /api/violations/session/{sessionId}/reset
Reset strike count (for appeals).

**Auth:** Admin only  
**Request:**
```json
{
  "reason": "Appeal granted - technical issue during exam"
}
```

**Response:**
```json
"Strike count reset"
```

---

## WebSocket Integration

### Moderator: Subscribe to Violations
```javascript
stompClient.subscribe('/topic/exam/456/monitoring', (message) => {
  const update = JSON.parse(message.body);
  
  if (update.type === 'violation_alert') {
    console.log('New violation:', update.payload);
    // {
    //   "studentId": 789,
    //   "violationType": "PHONE_DETECTED",
    //   "message": "Cell phone detected (Total strikes: 3)",
    //   "timestamp": 1735639800000
    // }
  }
  
  if (update.type === 'student_status') {
    console.log('Status update:', update.payload);
    // {
    //   "studentId": 789,
    //   "violationCount": 3,
    //   "statusColor": "YELLOW",
    //   ...
    // }
  }
});
```

### Student: Receive Termination Notice
```javascript
stompClient.subscribe('/user/queue/messages', (message) => {
  const notification = JSON.parse(message.body);
  
  if (notification.type === 'termination') {
    console.log('Exam terminated:', notification.payload);
    // {
    //   "reason": "Automatic termination: 5 strikes",
    //   "terminatedAt": "2025-12-31T10:35:00",
    //   "strikes": 5
    // }
    
    // Redirect to exam complete page
    window.location.href = '/exam-terminated';
  }
});
```

---

## Redis Keys

```bash
# Strike counter (atomic INCR)
exam:session:strikes:{sessionId}
# Example: GET exam:session:strikes:123 → "3"

# TTL: 4 hours
# Atomicity: Multiple simultaneous violations counted correctly
```

---

## Use Cases

### Use Case 1: Student Tab Switch Detection
```javascript
// Frontend monitors visibility change
document.addEventListener('visibilitychange', async () => {
  if (document.hidden) {
    // Tab switched or window minimized
    await reportViolation({
      type: 'TAB_SWITCH',
      severity: 'MAJOR',
      description: 'Tab switched or window minimized',
      evidence: {
        timestamp: new Date().toISOString(),
        duration: tabHiddenDuration
      }
    });
  }
});
```

### Use Case 2: Camera Monitoring with TensorFlow.js
```javascript
// Phase 8 implementation (future)
const predictions = await model.detect(videoElement);

if (predictions.length > 1) {
  // Multiple faces detected
  await reportViolation({
    type: 'MULTIPLE_FACES',
    severity: 'MAJOR',
    description: `${predictions.length} faces detected`,
    evidence: {
      faceCount: predictions.length,
      confidence: predictions[0].score,
      screenshot: captureFrame()
    }
  });
}
```

### Use Case 3: Moderator Reviews Violation
```javascript
// 1. Get violations
const violations = await fetch('/api/violations/session/123');

// 2. Review evidence
const violation = violations[0];
console.log('Evidence:', violation.evidence);

// 3. Reject false positive
if (violation.evidence.confidence < 0.80) {
  await fetch(`/api/violations/${violation.id}/confirm`, {
    method: 'PUT',
    body: JSON.stringify({
      confirmed: false,
      reason: 'Low confidence detection, likely false positive'
    })
  });
}
```

### Use Case 4: Admin Grants Appeal
```javascript
// Student appeals termination due to Wi-Fi issue
await fetch(`/api/violations/session/123/reset`, {
  method: 'POST',
  headers: {
    'Authorization': 'Bearer ' + adminJWT,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    reason: 'Appeal granted - Wi-Fi outage caused false violations'
  })
});

// Strike count reset to 0, student can retake exam
```

---

## Database Queries

### Find High-Risk Students
```sql
SELECT 
  session_id,
  student_id,
  COUNT(*) as violation_count,
  SUM(strike_count) as total_strikes
FROM violations
WHERE exam_id = 456 AND confirmed = true
GROUP BY session_id, student_id
HAVING SUM(strike_count) >= 3
ORDER BY total_strikes DESC;
```

### Violation Type Analysis
```sql
SELECT 
  type,
  COUNT(*) as count,
  AVG((evidence->>'confidence')::float) as avg_confidence
FROM violations
WHERE exam_id = 456
GROUP BY type
ORDER BY count DESC;
```

### Search Evidence for Specific Object
```sql
-- Find all phone detections with high confidence
SELECT 
  id,
  student_id,
  evidence->>'confidence' as confidence,
  evidence->>'detectedObject' as object,
  detected_at
FROM violations
WHERE evidence->>'detectedObject' = 'cell phone'
  AND (evidence->>'confidence')::float > 0.90
ORDER BY detected_at DESC;
```

---

## Testing with cURL

```bash
# 1. Report violation
curl -X POST http://localhost:8080/api/violations/report \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": 123,
    "examId": 456,
    "type": "TAB_SWITCH",
    "severity": "MAJOR",
    "description": "Tab switch detected",
    "evidence": {"timestamp": "2025-12-31T10:30:00Z"}
  }'

# 2. Get strike count
curl http://localhost:8080/api/violations/session/123/strikes \
  -H "Authorization: Bearer $JWT"

# 3. Get violations
curl http://localhost:8080/api/violations/session/123 \
  -H "Authorization: Bearer $MODERATOR_JWT"

# 4. Verify Redis
docker exec exam-portal-redis redis-cli GET exam:session:strikes:123

# 5. Check PostgreSQL
docker exec -it exam-portal-postgres psql -U exam_admin -d exam_portal_db
SELECT * FROM violations WHERE session_id = 123;
```

---

## Configuration

Edit `application.yml`:

```yaml
violation:
  max-strikes: 5  # Auto-terminate after 5 strikes
  debounce:
    consecutive-frames-required: 3  # For Phase 8 camera detection
    confidence-threshold: 0.85      # Minimum confidence for violation
```

---

## Error Handling

### Insufficient Strikes (< 5)
```json
{
  "strikeCount": 3,
  "terminated": false,
  "message": "Violation recorded. Total strikes: 3"
}
```

### Auto-Termination (≥ 5 strikes)
```json
{
  "strikeCount": 5,
  "terminated": true,
  "message": "Violation recorded. Total strikes: 5"
}
```
**Note:** Student receives WebSocket termination message and session status → TERMINATED

### Invalid Violation Type
```json
{
  "error": "Bad Request",
  "message": "Invalid violation type: INVALID_TYPE"
}
```

### Session Not Found
```json
{
  "error": "Not Found",
  "message": "Session not found: 999"
}
```

---

## Performance Metrics

- **Redis INCR**: ~1ms (atomic)
- **PostgreSQL INSERT**: ~10-20ms (with JSONB)
- **WebSocket broadcast**: <1ms
- **Total violation recording**: ~30-50ms

---

## Security

- All endpoints require JWT authentication
- Department-level isolation (ECE moderators can't see CSE violations)
- Evidence sanitized to prevent injection attacks
- Admin-only strike reset functionality

---

## Next Phase

**Phase 7**: React exam interface with camera monitoring  
- TensorFlow.js object detection
- IndexedDB offline storage
- Monaco Editor integration
- Real-time violation detection

-- Violations table schema
-- This documents the structure created by JPA/Hibernate

CREATE TABLE IF NOT EXISTS violations (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    exam_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    description VARCHAR(500),
    evidence JSONB,
    confirmed BOOLEAN NOT NULL DEFAULT true,
    detected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    strike_count INTEGER NOT NULL DEFAULT 1
);

-- Indexes for fast queries
CREATE INDEX IF NOT EXISTS idx_violation_session ON violations(session_id, detected_at);
CREATE INDEX IF NOT EXISTS idx_violation_student ON violations(student_id, detected_at);
CREATE INDEX IF NOT EXISTS idx_violation_exam ON violations(exam_id, detected_at);

-- GIN index for JSONB evidence column (for searching within evidence)
CREATE INDEX IF NOT EXISTS idx_violation_evidence ON violations USING GIN(evidence);

-- Example JSONB evidence structure
COMMENT ON COLUMN violations.evidence IS 'JSONB format: {
  "screenshot": "base64_encoded_image",
  "confidence": 0.95,
  "detectedObject": "cell phone",
  "boundingBox": {"x": 100, "y": 200, "width": 50, "height": 80},
  "timestamp": "2025-12-31T10:30:00Z",
  "clientInfo": {"userAgent": "...", "screenResolution": "1920x1080"}
}';

-- Constraint: Severity values
ALTER TABLE violations ADD CONSTRAINT check_severity 
CHECK (severity IN ('MINOR', 'MAJOR', 'CRITICAL'));

-- Constraint: Type values
ALTER TABLE violations ADD CONSTRAINT check_type 
CHECK (type IN (
    'MULTIPLE_FACES', 'NO_FACE_DETECTED', 'PHONE_DETECTED',
    'TAB_SWITCH', 'WINDOW_BLUR', 'FULLSCREEN_EXIT',
    'COPY_PASTE_DETECTED', 'FORBIDDEN_CONSTRUCT',
    'MANUAL_FLAG', 'SUSPICIOUS_ACTIVITY'
));

-- Example queries for moderator dashboard
-- 1. Get violations for a session with evidence
-- SELECT id, type, severity, description, evidence->>'screenshot' as screenshot, detected_at 
-- FROM violations WHERE session_id = 123 ORDER BY detected_at DESC;

-- 2. Count violations by type for an exam
-- SELECT type, COUNT(*) as count FROM violations 
-- WHERE exam_id = 456 GROUP BY type ORDER BY count DESC;

-- 3. Find sessions with high violation counts
-- SELECT session_id, COUNT(*) as violation_count 
-- FROM violations WHERE confirmed = true 
-- GROUP BY session_id HAVING COUNT(*) >= 5;

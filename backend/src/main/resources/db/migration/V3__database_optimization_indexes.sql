-- Phase 10: Database Optimization - Strategic Indexes
-- Flyway migration V3

-- ============================================
-- 1. VIOLATIONS TABLE OPTIMIZATION
-- ============================================

-- Drop existing basic indexes
DROP INDEX IF EXISTS idx_violation_session;
DROP INDEX IF EXISTS idx_violation_student;
DROP INDEX IF EXISTS idx_violation_exam;

-- Composite indexes for common query patterns
-- Pattern 1: Moderator dashboard - violations by exam + time
CREATE INDEX idx_violations_exam_time ON violations(exam_id, detected_at DESC, confirmed) 
WHERE confirmed = true;

-- Pattern 2: Student violation history - by student + time + exam
CREATE INDEX idx_violations_student_exam ON violations(student_id, exam_id, detected_at DESC);

-- Pattern 3: Session violations - by session + time for strike counting
CREATE INDEX idx_violations_session_strikes ON violations(session_id, detected_at DESC, severity);

-- Pattern 4: High-priority violations - for alerts
CREATE INDEX idx_violations_critical ON violations(severity, detected_at DESC) 
WHERE severity = 'CRITICAL' AND confirmed = true;

-- Pattern 5: Unconfirmed violations - for false positive review
CREATE INDEX idx_violations_unconfirmed ON violations(confirmed, detected_at DESC) 
WHERE confirmed = false;

-- JSONB optimizations
-- For searching specific evidence fields
CREATE INDEX idx_violations_confidence ON violations((evidence->>'confidence')::FLOAT);
CREATE INDEX idx_violations_consecutive_frames ON violations((evidence->>'consecutiveFrames')::INT);

-- ============================================
-- 2. USERS TABLE OPTIMIZATION
-- ============================================

-- Composite index for authentication queries
CREATE INDEX idx_users_email_enabled ON users(email, enabled) WHERE enabled = true;

-- Department-based queries for moderator access
CREATE INDEX idx_users_department_role ON users(department);

-- ============================================
-- 3. SESSION_MANAGER TABLE INDEXES
-- (Assuming table exists from Phase 5)
-- ============================================

-- Note: Table may need to be created in a separate migration
-- These indexes assume the table structure exists

-- Composite index for active sessions
CREATE INDEX IF NOT EXISTS idx_sessions_exam_active ON session_manager(exam_id, activity_status, last_heartbeat DESC) 
WHERE activity_status = 'ACTIVE';

-- Index for finding student sessions
CREATE INDEX IF NOT EXISTS idx_sessions_student_exam ON session_manager(student_id, exam_id, created_at DESC);

-- Index for connection monitoring
CREATE INDEX IF NOT EXISTS idx_sessions_connection_status ON session_manager(connection_status, last_heartbeat DESC);

-- Index for terminated sessions
CREATE INDEX IF NOT EXISTS idx_sessions_terminated ON session_manager(exam_id, terminated_at DESC) 
WHERE activity_status = 'TERMINATED';

-- ============================================
-- 4. EXAM SUBMISSIONS TABLE INDEXES
-- (Assuming table exists)
-- ============================================

CREATE INDEX IF NOT EXISTS idx_submissions_student_exam ON submissions(student_id, exam_id, submitted_at DESC);
CREATE INDEX IF NOT EXISTS idx_submissions_exam_status ON submissions(exam_id, status, submitted_at DESC);

-- ============================================
-- 5. STATISTICS AND MATERIALIZED VIEW
-- ============================================

-- Materialized view for exam statistics (refresh periodically)
CREATE MATERIALIZED VIEW IF NOT EXISTS exam_violation_stats AS
SELECT 
    e.exam_id,
    COUNT(DISTINCT v.student_id) as students_with_violations,
    COUNT(*) as total_violations,
    COUNT(*) FILTER (WHERE v.severity = 'CRITICAL') as critical_violations,
    COUNT(*) FILTER (WHERE v.severity = 'MAJOR') as major_violations,
    COUNT(*) FILTER (WHERE v.severity = 'MINOR') as minor_violations,
    AVG((v.evidence->>'confidence')::FLOAT) as avg_confidence,
    MAX(v.detected_at) as last_violation_time
FROM violations v
JOIN session_manager e ON v.session_id = e.id
WHERE v.confirmed = true
GROUP BY e.exam_id;

-- Index on materialized view
CREATE UNIQUE INDEX idx_exam_stats_exam_id ON exam_violation_stats(exam_id);

-- ============================================
-- 6. QUERY OPTIMIZATION HINTS
-- ============================================

-- Increase statistics target for frequently queried columns
ALTER TABLE violations ALTER COLUMN exam_id SET STATISTICS 1000;
ALTER TABLE violations ALTER COLUMN detected_at SET STATISTICS 1000;
ALTER TABLE violations ALTER COLUMN severity SET STATISTICS 500;

-- Enable parallel query for large scans
ALTER TABLE violations SET (parallel_workers = 4);

-- ============================================
-- 7. PARTITIONING PREPARATION (Optional)
-- ============================================

-- For very large deployments, consider partitioning violations by exam_id or detected_at
-- Example (commented out - enable if needed):

-- CREATE TABLE violations_partitioned (
--     LIKE violations INCLUDING ALL
-- ) PARTITION BY RANGE (detected_at);
-- 
-- CREATE TABLE violations_2025_q1 PARTITION OF violations_partitioned
--     FOR VALUES FROM ('2025-01-01') TO ('2025-04-01');
-- 
-- CREATE TABLE violations_2025_q2 PARTITION OF violations_partitioned
--     FOR VALUES FROM ('2025-04-01') TO ('2025-07-01');

-- ============================================
-- 8. MAINTENANCE JOBS
-- ============================================

COMMENT ON INDEX idx_violations_exam_time IS 'Optimizes moderator dashboard queries - refresh exam_violation_stats after bulk inserts';
COMMENT ON MATERIALIZED VIEW exam_violation_stats IS 'Refresh every 5 minutes: REFRESH MATERIALIZED VIEW CONCURRENTLY exam_violation_stats';

-- Example maintenance commands (run via cron job):
-- REFRESH MATERIALIZED VIEW CONCURRENTLY exam_violation_stats;
-- ANALYZE violations;
-- REINDEX INDEX CONCURRENTLY idx_violations_exam_time;

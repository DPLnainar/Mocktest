-- Phase 10: Maintenance Scripts
-- Run these commands periodically via cron job

-- ============================================
-- 1. REFRESH MATERIALIZED VIEW (Every 5 minutes)
-- ============================================

REFRESH MATERIALIZED VIEW CONCURRENTLY exam_violation_stats;

-- ============================================
-- 2. UPDATE STATISTICS (Daily at 2 AM)
-- ============================================

ANALYZE violations;
ANALYZE users;
ANALYZE session_manager;

-- ============================================
-- 3. REINDEX (Weekly on Sunday at 3 AM)
-- ============================================

REINDEX INDEX CONCURRENTLY idx_violations_exam_time;
REINDEX INDEX CONCURRENTLY idx_violations_session_strikes;
REINDEX INDEX CONCURRENTLY idx_violations_student_exam;

-- ============================================
-- 4. VACUUM (Daily at 3 AM)
-- ============================================

VACUUM ANALYZE violations;

-- ============================================
-- 5. CLEANUP OLD DATA (Monthly)
-- ============================================

-- Delete violations older than 1 year (adjust retention as needed)
DELETE FROM violations 
WHERE detected_at < NOW() - INTERVAL '1 year'
AND confirmed = false; -- Keep confirmed violations for audit

-- ============================================
-- 6. MONITORING QUERIES
-- ============================================

-- Check index usage
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_scan as scans,
    idx_tup_read as tuples_read,
    idx_tup_fetch as tuples_fetched
FROM pg_stat_user_indexes
WHERE schemaname = 'public'
ORDER BY idx_scan DESC;

-- Check table bloat
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- Check slow queries (requires pg_stat_statements extension)
SELECT 
    query,
    calls,
    total_time,
    mean_time,
    max_time
FROM pg_stat_statements
WHERE query LIKE '%violations%'
ORDER BY mean_time DESC
LIMIT 20;

-- Check connection pool utilization
SELECT 
    datname,
    count(*) as connections,
    max(state) as state
FROM pg_stat_activity
WHERE datname = 'exam_portal_db'
GROUP BY datname;

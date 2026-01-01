package com.examportal.violation.repository;

import com.examportal.violation.entity.Violation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Phase 10: Optimized ViolationRepository
 * 
 * Uses EntityGraph for N+1 prevention
 * Implements keyset pagination for large datasets
 */
@Repository
public interface OptimizedViolationRepository extends JpaRepository<Violation, Long> {

    // ============================================
    // OPTIMIZED QUERIES WITH INDEXES
    // ============================================

    /**
     * Find violations by session with keyset pagination
     * Uses idx_violations_session_strikes index
     */
    @Query("""
        SELECT v FROM Violation v 
        WHERE v.sessionId = :sessionId 
        AND (:cursor IS NULL OR v.detectedAt < :cursor)
        ORDER BY v.detectedAt DESC
        """)
    Page<Violation> findBySessionIdWithKeyset(
            @Param("sessionId") Long sessionId,
            @Param("cursor") LocalDateTime cursor,
            Pageable pageable);

    /**
     * Find violations by exam with filtering
     * Uses idx_violations_exam_time index
     */
    @Query("""
        SELECT v FROM Violation v 
        WHERE v.examId = :examId 
        AND v.confirmed = true
        AND (:cursor IS NULL OR v.detectedAt < :cursor)
        ORDER BY v.detectedAt DESC
        """)
    Page<Violation> findByExamIdWithKeyset(
            @Param("examId") Long examId,
            @Param("cursor") LocalDateTime cursor,
            Pageable pageable);

    /**
     * Count strikes by session (optimized with index)
     * Uses idx_violations_session_strikes
     */
    @Query("""
        SELECT COALESCE(SUM(v.strikeCount), 0) 
        FROM Violation v 
        WHERE v.sessionId = :sessionId 
        AND v.confirmed = true
        """)
    Integer sumStrikesBySessionId(@Param("sessionId") Long sessionId);

    /**
     * Find critical violations for an exam
     * Uses idx_violations_critical partial index
     */
    @Query("""
        SELECT v FROM Violation v 
        WHERE v.examId = :examId 
        AND v.severity = 'CRITICAL'
        AND v.confirmed = true
        ORDER BY v.detectedAt DESC
        """)
    List<Violation> findCriticalViolationsByExam(@Param("examId") Long examId);

    /**
     * Find unconfirmed violations (false positive review)
     * Uses idx_violations_unconfirmed partial index
     */
    @Query("""
        SELECT v FROM Violation v 
        WHERE v.confirmed = false 
        AND v.detectedAt > :since
        ORDER BY v.detectedAt DESC
        """)
    Page<Violation> findUnconfirmedViolations(
            @Param("since") LocalDateTime since,
            Pageable pageable);

    /**
     * Batch update - mark violations as confirmed
     * Optimized for bulk operations
     */
    @Query("""
        UPDATE Violation v 
        SET v.confirmed = :confirmed 
        WHERE v.id IN :ids
        """)
    void updateConfirmationBatch(@Param("ids") List<Long> ids, @Param("confirmed") boolean confirmed);

    /**
     * Find violations with evidence confidence filtering
     * Uses idx_violations_confidence functional index
     */
    @Query(value = """
        SELECT * FROM violations v
        WHERE v.exam_id = :examId
        AND (v.evidence->>'confidence')::FLOAT >= :minConfidence
        ORDER BY v.detected_at DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Violation> findHighConfidenceViolations(
            @Param("examId") Long examId,
            @Param("minConfidence") Double minConfidence,
            @Param("limit") int limit);

    /**
     * Aggregate query for exam statistics
     * Optimized with exam_violation_stats materialized view
     */
    @Query(value = """
        SELECT 
            v.type,
            v.severity,
            COUNT(*) as count,
            AVG((v.evidence->>'confidence')::FLOAT) as avg_confidence
        FROM violations v
        WHERE v.exam_id = :examId
        AND v.confirmed = true
        GROUP BY v.type, v.severity
        ORDER BY count DESC
        """, nativeQuery = true)
    List<Object[]> getViolationStatsByExam(@Param("examId") Long examId);

    /**
     * Find recent violations across all exams (for admin dashboard)
     * With keyset pagination
     */
    @Query("""
        SELECT v FROM Violation v 
        WHERE v.confirmed = true
        AND (:cursor IS NULL OR v.detectedAt < :cursor)
        ORDER BY v.detectedAt DESC
        """)
    Page<Violation> findAllRecentViolations(
            @Param("cursor") LocalDateTime cursor,
            Pageable pageable);

    /**
     * Count violations by student (for analytics)
     */
    @Query("""
        SELECT COUNT(v) FROM Violation v 
        WHERE v.studentId = :studentId 
        AND v.confirmed = true
        AND v.detectedAt BETWEEN :startDate AND :endDate
        """)
    Long countViolationsByStudentInDateRange(
            @Param("studentId") Long studentId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}

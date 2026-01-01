package com.examportal.violation.repository;

import com.examportal.violation.entity.Violation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ViolationRepository extends JpaRepository<Violation, Long> {

    /**
     * Count violations for a session
     */
    @Query("SELECT COUNT(v) FROM Violation v WHERE v.sessionId = :sessionId AND v.confirmed = true")
    int countBySessionId(@Param("sessionId") Long sessionId);

    /**
     * Sum strike counts for a session
     */
    @Query("SELECT COALESCE(SUM(v.strikeCount), 0) FROM Violation v WHERE v.sessionId = :sessionId AND v.confirmed = true")
    int sumStrikesBySessionId(@Param("sessionId") Long sessionId);

    /**
     * Get violations by session
     */
    List<Violation> findBySessionIdOrderByDetectedAtDesc(Long sessionId);

    /**
     * Get violations by student
     */
    List<Violation> findByStudentIdOrderByDetectedAtDesc(Long studentId);

    /**
     * Get violations by exam
     */
    List<Violation> findByExamIdOrderByDetectedAtDesc(Long examId);

    /**
     * Get violations by type for a session
     */
    List<Violation> findBySessionIdAndType(Long sessionId, Violation.ViolationType type);

    /**
     * Get recent violations (last 24 hours)
     */
    @Query("SELECT v FROM Violation v WHERE v.detectedAt > :since ORDER BY v.detectedAt DESC")
    List<Violation> findRecentViolations(@Param("since") LocalDateTime since);

    /**
     * Get unconfirmed violations (for review)
     */
    List<Violation> findByConfirmedFalse();
}

package com.examportal.violation.service;

import com.examportal.violation.entity.Violation;
import com.examportal.violation.repository.OptimizedViolationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Phase 10: Optimized Violation Query Service
 * 
 * Implements keyset pagination and efficient queries
 * Reduces N+1 queries with proper repository methods
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OptimizedViolationQueryService {

    private final OptimizedViolationRepository violationRepository;

    /**
     * Get violations with keyset pagination
     * More efficient than offset-based pagination for large datasets
     * 
     * @param sessionId Session ID
     * @param cursor Last seen timestamp (null for first page)
     * @param pageSize Number of results per page
     * @return Page of violations
     */
    @Transactional(readOnly = true)
    public Page<Violation> getSessionViolationsWithKeyset(Long sessionId, 
                                                          LocalDateTime cursor, 
                                                          int pageSize) {
        Pageable pageable = PageRequest.of(0, pageSize);
        return violationRepository.findBySessionIdWithKeyset(sessionId, cursor, pageable);
    }

    /**
     * Get exam violations with keyset pagination
     */
    @Transactional(readOnly = true)
    public Page<Violation> getExamViolationsWithKeyset(Long examId, 
                                                       LocalDateTime cursor, 
                                                       int pageSize) {
        Pageable pageable = PageRequest.of(0, pageSize);
        return violationRepository.findByExamIdWithKeyset(examId, cursor, pageable);
    }

    /**
     * Get critical violations (immediate alerts)
     * Uses partial index for fast retrieval
     */
    @Transactional(readOnly = true)
    public List<Violation> getCriticalViolations(Long examId) {
        return violationRepository.findCriticalViolationsByExam(examId);
    }

    /**
     * Get unconfirmed violations for false positive review
     */
    @Transactional(readOnly = true)
    public Page<Violation> getUnconfirmedViolations(LocalDateTime since, int pageSize) {
        Pageable pageable = PageRequest.of(0, pageSize);
        return violationRepository.findUnconfirmedViolations(since, pageable);
    }

    /**
     * Get high-confidence violations (>0.85)
     * Uses functional index on JSONB field
     */
    @Transactional(readOnly = true)
    public List<Violation> getHighConfidenceViolations(Long examId, int limit) {
        return violationRepository.findHighConfidenceViolations(examId, 0.85, limit);
    }

    /**
     * Get violation statistics for exam
     * Aggregated query optimized with materialized view
     */
    @Transactional(readOnly = true)
    public List<Object[]> getViolationStats(Long examId) {
        return violationRepository.getViolationStatsByExam(examId);
    }

    /**
     * Batch confirm violations
     * Reduces round trips with batch update
     */
    @Transactional
    public void batchConfirmViolations(List<Long> violationIds, boolean confirmed) {
        log.info("Batch updating {} violations to confirmed={}", violationIds.size(), confirmed);
        violationRepository.updateConfirmationBatch(violationIds, confirmed);
    }

    /**
     * Get recent violations across all exams (admin dashboard)
     */
    @Transactional(readOnly = true)
    public Page<Violation> getAllRecentViolations(LocalDateTime cursor, int pageSize) {
        Pageable pageable = PageRequest.of(0, pageSize);
        return violationRepository.findAllRecentViolations(cursor, pageable);
    }

    /**
     * Count violations for analytics
     */
    @Transactional(readOnly = true)
    public Long countStudentViolations(Long studentId, LocalDateTime startDate, LocalDateTime endDate) {
        return violationRepository.countViolationsByStudentInDateRange(studentId, startDate, endDate);
    }
}

package com.examportal.violation.controller;

import com.examportal.violation.dto.KeysetPageResponse;
import com.examportal.violation.entity.Violation;
import com.examportal.violation.service.OptimizedViolationQueryService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Phase 10: Optimized Violation Query Controller
 * 
 * Endpoints with keyset pagination for efficient large dataset queries
 */
@RestController
@RequestMapping("/api/violations/optimized")
public class OptimizedViolationController {

    private final OptimizedViolationQueryService queryService;

    public OptimizedViolationController(OptimizedViolationQueryService queryService) {
        this.queryService = queryService;
    }

    /**
     * Get session violations with keyset pagination
     * 
     * GET
     * /api/violations/optimized/session/{sessionId}?cursor=2025-12-31T10:30:00&size=20
     */
    @GetMapping("/session/{sessionId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<KeysetPageResponse<List<Violation>>> getSessionViolations(
            @PathVariable Long sessionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime cursor,
            @RequestParam(defaultValue = "20") int size) {

        Page<Violation> page = queryService.getSessionViolationsWithKeyset(sessionId, cursor, size);

        LocalDateTime nextCursor = page.hasContent()
                ? page.getContent().get(page.getContent().size() - 1).getTimestamp()
                : null;

        return ResponseEntity.ok(KeysetPageResponse.of(
                page.getContent(),
                nextCursor,
                page.hasNext(),
                size));
    }

    /**
     * Get exam violations with keyset pagination
     * 
     * GET
     * /api/violations/optimized/exam/{examId}?cursor=2025-12-31T10:30:00&size=50
     */
    @GetMapping("/exam/{examId}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public ResponseEntity<KeysetPageResponse<List<Violation>>> getExamViolations(
            @PathVariable Long examId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime cursor,
            @RequestParam(defaultValue = "50") int size) {

        Page<Violation> page = queryService.getExamViolationsWithKeyset(examId, cursor, size);

        LocalDateTime nextCursor = page.hasContent()
                ? page.getContent().get(page.getContent().size() - 1).getTimestamp()
                : null;

        return ResponseEntity.ok(KeysetPageResponse.of(
                page.getContent(),
                nextCursor,
                page.hasNext(),
                size));
    }

    /**
     * Get critical violations for immediate alerts
     * 
     * GET /api/violations/optimized/exam/{examId}/critical
     */
    @GetMapping("/exam/{examId}/critical")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public ResponseEntity<List<Violation>> getCriticalViolations(@PathVariable Long examId) {
        List<Violation> violations = queryService.getCriticalViolations(examId);
        return ResponseEntity.ok(violations);
    }

    /**
     * Get high-confidence violations (>0.85)
     * 
     * GET /api/violations/optimized/exam/{examId}/high-confidence?limit=100
     */
    @GetMapping("/exam/{examId}/high-confidence")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public ResponseEntity<List<Violation>> getHighConfidenceViolations(
            @PathVariable Long examId,
            @RequestParam(defaultValue = "100") int limit) {

        List<Violation> violations = queryService.getHighConfidenceViolations(examId, limit);
        return ResponseEntity.ok(violations);
    }

    /**
     * Get unconfirmed violations (false positive review)
     * 
     * GET /api/violations/optimized/unconfirmed?since=2025-12-01T00:00:00&size=50
     */
    @GetMapping("/unconfirmed")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public ResponseEntity<KeysetPageResponse<List<Violation>>> getUnconfirmedViolations(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since,
            @RequestParam(defaultValue = "50") int size) {

        Page<Violation> page = queryService.getUnconfirmedViolations(since, size);

        LocalDateTime nextCursor = page.hasContent()
                ? page.getContent().get(page.getContent().size() - 1).getTimestamp()
                : null;

        return ResponseEntity.ok(KeysetPageResponse.of(
                page.getContent(),
                nextCursor,
                page.hasNext(),
                size));
    }

    /**
     * Get violation statistics for exam
     * 
     * GET /api/violations/optimized/exam/{examId}/stats
     */
    @GetMapping("/exam/{examId}/stats")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public ResponseEntity<List<Object[]>> getViolationStats(@PathVariable Long examId) {
        List<Object[]> stats = queryService.getViolationStats(examId);
        return ResponseEntity.ok(stats);
    }

    /**
     * Batch confirm violations
     * 
     * POST /api/violations/optimized/batch-confirm
     */
    @PostMapping("/batch-confirm")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public ResponseEntity<Void> batchConfirmViolations(@RequestBody BatchConfirmRequest request) {
        queryService.batchConfirmViolations(request.getViolationIds(), request.isConfirmed());
        return ResponseEntity.ok().build();
    }

    /**
     * Get all recent violations (admin dashboard)
     * 
     * GET /api/violations/optimized/recent?cursor=2025-12-31T10:30:00&size=100
     */
    @GetMapping("/recent")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<KeysetPageResponse<List<Violation>>> getAllRecentViolations(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime cursor,
            @RequestParam(defaultValue = "100") int size) {

        Page<Violation> page = queryService.getAllRecentViolations(cursor, size);

        LocalDateTime nextCursor = page.hasContent()
                ? page.getContent().get(page.getContent().size() - 1).getTimestamp()
                : null;

        return ResponseEntity.ok(KeysetPageResponse.of(
                page.getContent(),
                nextCursor,
                page.hasNext(),
                size));
    }

    /**
     * Count student violations in date range
     * 
     * GET
     * /api/violations/optimized/student/{studentId}/count?start=2025-01-01T00:00:00&end=2025-12-31T23:59:59
     */
    @GetMapping("/student/{studentId}/count")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public ResponseEntity<Long> countStudentViolations(
            @PathVariable Long studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        Long count = queryService.countStudentViolations(studentId, start, end);
        return ResponseEntity.ok(count);
    }

    // DTO for batch operations
    public static class BatchConfirmRequest {
        private List<Long> violationIds;
        private boolean confirmed;

        public BatchConfirmRequest() {
        }

        public BatchConfirmRequest(List<Long> violationIds, boolean confirmed) {
            this.violationIds = violationIds;
            this.confirmed = confirmed;
        }

        public List<Long> getViolationIds() {
            return violationIds;
        }

        public void setViolationIds(List<Long> violationIds) {
            this.violationIds = violationIds;
        }

        public boolean isConfirmed() {
            return confirmed;
        }

        public void setConfirmed(boolean confirmed) {
            this.confirmed = confirmed;
        }
    }
}

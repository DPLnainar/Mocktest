package com.examportal.violation.controller;

import com.examportal.security.CustomUserDetails;
import com.examportal.security.DepartmentSecurityService;
import com.examportal.violation.dto.EnhancedViolationRequest;
import com.examportal.violation.entity.Violation;
import com.examportal.violation.service.FalsePositiveFilterService;
import com.examportal.violation.service.ViolationService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Violation Controller (Phase 8 Enhanced)
 * 
 * REST endpoints with false-positive filtering
 */
@RestController
@RequestMapping("/api/violations")
@RequiredArgsConstructor
@Slf4j
public class ViolationController {

    private final ViolationService violationService;
    private final FalsePositiveFilterService falsePositiveFilter;
    private final DepartmentSecurityService securityService;

    /**
     * Report a violation (Phase 8: with false-positive filtering)
     * 
     * POST /api/violations/report
     */
    @PostMapping("/report")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ViolationResponse> reportViolation(
            @Valid @RequestBody EnhancedViolationRequest request,
            @AuthenticationPrincipal CustomUserDetails student) {
        
        log.info("Student {} reporting violation: {} (confidence: {}, consecutive: {})", 
                student.getId(), request.getViolationType(), 
                request.getConfidence(), request.getConsecutiveFrames());

        // Phase 8: Validate violation meets quality thresholds
        if (!falsePositiveFilter.shouldProcessViolation(request)) {
            log.debug("Violation rejected - failed false-positive filter");
            
            int currentStrikes = violationService.getStrikeCount(request.getSessionId());
            return ResponseEntity.ok(new ViolationResponse(
                    currentStrikes,
                    false,
                    "Violation filtered (insufficient confidence or consecutive frames)"
            ));
        }

        // Parse enums
        Violation.ViolationType type = Violation.ViolationType.valueOf(request.getViolationType());
        Violation.Severity severity = Violation.Severity.valueOf(request.getSeverity());

        int strikeCount = violationService.recordViolation(
                request.getSessionId(),
                student.getId(),
                request.getExamId(),
                type,
                severity,
                request.getMessage(),
                request.getEvidence()
        );

        return ResponseEntity.ok(new ViolationResponse(
                strikeCount,
                strikeCount >= 5,
                "Violation recorded. Total strikes: " + strikeCount
        ));
    }

    /**
     * Get violations for a session
     * 
     * GET /api/violations/session/{sessionId}
     */
    @GetMapping("/session/{sessionId}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public ResponseEntity<List<Violation>> getSessionViolations(@PathVariable Long sessionId) {
        log.debug("Fetching violations for session {}", sessionId);

        List<Violation> violations = violationService.getSessionViolations(sessionId);
        return ResponseEntity.ok(violations);
    }

    /**
     * Get violations for a student
     * 
     * GET /api/violations/student/{studentId}
     */
    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public ResponseEntity<List<Violation>> getStudentViolations(@PathVariable Long studentId) {
        log.debug("Fetching violations for student {}", studentId);

        List<Violation> violations = violationService.getStudentViolations(studentId);
        return ResponseEntity.ok(violations);
    }

    /**
     * Get violations for an exam
     * 
     * GET /api/violations/exam/{examId}
     */
    @GetMapping("/exam/{examId}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public ResponseEntity<List<Violation>> getExamViolations(@PathVariable Long examId) {
        log.debug("Fetching violations for exam {}", examId);

        List<Violation> violations = violationService.getExamViolations(examId);
        return ResponseEntity.ok(violations);
    }

    /**
     * Get strike count for a session
     * 
     * GET /api/violations/session/{sessionId}/strikes
     */
    @GetMapping("/session/{sessionId}/strikes")
    @PreAuthorize("hasAnyRole('STUDENT', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<StrikeCountResponse> getStrikeCount(@PathVariable Long sessionId) {
        int strikeCount = violationService.getStrikeCount(sessionId);
        
        return ResponseEntity.ok(new StrikeCountResponse(
                strikeCount,
                strikeCount >= 5,
                5 - strikeCount
        ));
    }

    /**
     * Get violation statistics for a session
     * 
     * GET /api/violations/session/{sessionId}/stats
     */
    @GetMapping("/session/{sessionId}/stats")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public ResponseEntity<ViolationService.ViolationStats> getViolationStats(@PathVariable Long sessionId) {
        ViolationService.ViolationStats stats = violationService.getViolationStats(sessionId);
        return ResponseEntity.ok(stats);
    }

    /**
     * Confirm or reject a violation (for false positives)
     * 
     * PUT /api/violations/{violationId}/confirm
     */
    @PutMapping("/{violationId}/confirm")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public ResponseEntity<String> confirmViolation(
            @PathVariable Long violationId,
            @RequestBody ConfirmationRequest request) {
        
        log.info("Moderator {} violation {}: {}", 
                request.isConfirmed() ? "confirming" : "rejecting", 
                violationId, request.getReason());

        violationService.updateViolationConfirmation(
                violationId, 
                request.isConfirmed(), 
                request.getReason()
        );

        return ResponseEntity.ok(request.isConfirmed() ? "Violation confirmed" : "Violation rejected");
    }

    /**
     * Reset strike count (for appeals)
     * 
     * POST /api/violations/session/{sessionId}/reset
     */
    @PostMapping("/session/{sessionId}/reset")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> resetStrikeCount(
            @PathVariable Long sessionId,
            @RequestBody ResetRequest request) {
        
        log.warn("Admin resetting strike count for session {}: {}", sessionId, request.getReason());

        violationService.resetStrikeCount(sessionId, request.getReason());
        return ResponseEntity.ok("Strike count reset");
    }

    // Request/Response DTOs
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ViolationReportRequest {
        private Long sessionId;
        private Long examId;
        private Violation.ViolationType type;
        private Violation.Severity severity;
        private String description;
        private Map<String, Object> evidence;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ViolationResponse {
        private int strikeCount;
        private boolean terminated;
        private String message;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StrikeCountResponse {
        private int currentStrikes;
        private boolean terminated;
        private int remainingStrikes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfirmationRequest {
        private boolean confirmed;
        private String reason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResetRequest {
        private String reason;
    }
}

package com.examportal.controller;

import com.examportal.entity.ProctorLog;
import com.examportal.entity.ViolationType;
import com.examportal.service.ProctorLogService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/proctor")
@RequiredArgsConstructor
public class ProctorController {

    private final ProctorLogService proctorLogService;

    @PostMapping("/violation")
    @PreAuthorize("hasAuthority('STUDENT')")
    public ResponseEntity<ViolationResponse> logViolation(@RequestBody ViolationRequest request) {
        ProctorLog log = proctorLogService.logViolation(
            request.getAttemptId(),
            request.getEventType(),
            request.getMetadata()
        );

        long violationCount = proctorLogService.getViolationCount(request.getAttemptId());
        boolean shouldAutoSubmit = proctorLogService.checkAutoSubmitThreshold(request.getAttemptId());

        return ResponseEntity.ok(new ViolationResponse(violationCount, shouldAutoSubmit));
    }

    @GetMapping("/attempts/{attemptId}/violations")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public ResponseEntity<List<ProctorLog>> getViolations(@PathVariable Long attemptId) {
        List<ProctorLog> violations = proctorLogService.getViolationTimeline(attemptId);
        return ResponseEntity.ok(violations);
    }

    @GetMapping("/attempts/{attemptId}/violation-summary")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public ResponseEntity<Map<ViolationType, Long>> getViolationSummary(@PathVariable Long attemptId) {
        Map<ViolationType, Long> summary = proctorLogService.getViolationSummary(attemptId);
        return ResponseEntity.ok(summary);
    }

    @Data
    public static class ViolationRequest {
        private Long attemptId;
        private ViolationType eventType;
        private Map<String, Object> metadata;
    }

    @Data
    public static class ViolationResponse {
        private final long violationCount;
        private final boolean shouldAutoSubmit;
    }
}

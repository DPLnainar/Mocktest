package com.examportal.controller;

import com.examportal.dto.ProctorLogDTO;
import com.examportal.entity.ProctorLog;
import com.examportal.service.ProctorLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/student/proctor")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('STUDENT')")
public class ProctoringController {

    private final ProctorLogService proctorLogService;

    @PostMapping("/attempts/{attemptId}/log")
    public ResponseEntity<ProctorLog> logViolation(
            @PathVariable Long attemptId,
            @RequestBody ProctorLogDTO dto) {

        ProctorLog log = proctorLogService.logViolation(
                attemptId,
                dto.getEventType(),
                dto.getMetadata());

        // Check if test needs to be auto-submitted
        proctorLogService.checkAutoSubmitThreshold(attemptId);

        return ResponseEntity.ok(log);
    }
}

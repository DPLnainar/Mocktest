package com.examportal.violation.service;

import com.examportal.monitoring.model.StudentStatus;
import com.examportal.violation.entity.Violation;
import com.examportal.violation.repository.ViolationRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ViolationService {

    @Autowired
    private ViolationRepository violationRepository;

    public Violation logViolation(Violation violation) {
        violation.setTimestamp(LocalDateTime.now());
        violation.setDetectedAt(LocalDateTime.now());
        return violationRepository.save(violation);
    }

    public List<Violation> getStudentViolations(Long studentId) {
        return violationRepository.findByStudentIdOrderByDetectedAtDesc(studentId);
    }

    public List<Violation> getSessionViolations(Long sessionId) {
        return violationRepository.findBySessionIdOrderByDetectedAtDesc(sessionId);
    }

    public List<Violation> getExamViolations(Long examId) {
        return violationRepository.findByExamIdOrderByDetectedAtDesc(examId);
    }

    public int getStrikeCount(Long sessionId) {
        return violationRepository.sumStrikesBySessionId(sessionId);
    }

    public int recordViolation(Long sessionId, Long studentId, Long examId, 
                                Violation.ViolationType type, Violation.Severity severity, 
                                String message, Object evidence) {
        Violation violation = Violation.builder()
                .sessionId(sessionId)
                .studentId(studentId)
                .examId(examId)
                .type(type)
                .violationType(type)
                .severity(severity)
                .message(message)
                .timestamp(LocalDateTime.now())
                .detectedAt(LocalDateTime.now())
                .confirmed(false)
                .strikeCount(1)
                .build();
        
        violationRepository.save(violation);
        return getStrikeCount(sessionId);
    }

    public ViolationStats getViolationStats(Long sessionId) {
        List<Violation> violations = getSessionViolations(sessionId);
        
        Map<Violation.ViolationType, Long> typeCount = violations.stream()
                .collect(Collectors.groupingBy(Violation::getType, Collectors.counting()));
        
        int totalStrikes = getStrikeCount(sessionId);
        int confirmedCount = (int) violations.stream().filter(Violation::isConfirmed).count();
        
        // Calculate camera violations (PHONE_DETECTED, NO_FACE_DETECTED, MULTIPLE_FACES, NO_FACE, UNKNOWN_FACE)
        int cameraViolations = (int) violations.stream()
                .filter(v -> v.getType() == Violation.ViolationType.PHONE_DETECTED ||
                            v.getType() == Violation.ViolationType.NO_FACE_DETECTED ||
                            v.getType() == Violation.ViolationType.MULTIPLE_FACES ||
                            v.getType() == Violation.ViolationType.NO_FACE ||
                            v.getType() == Violation.ViolationType.UNKNOWN_FACE)
                .count();
        
        // Calculate tab switch count
        int tabSwitchCount = (int) violations.stream()
                .filter(v -> v.getType() == Violation.ViolationType.TAB_SWITCH)
                .count();
        
        // Determine if session is terminated (e.g., if total strikes >= 5)
        boolean terminated = totalStrikes >= 5;
        
        return ViolationStats.builder()
                .totalViolations(violations.size())
                .totalStrikes(totalStrikes)
                .confirmedViolations(confirmedCount)
                .pendingReview(violations.size() - confirmedCount)
                .cameraViolations(cameraViolations)
                .tabSwitchCount(tabSwitchCount)
                .terminated(terminated)
                .violationsByType(typeCount)
                .build();
    }

    public void updateViolationConfirmation(Long violationId, boolean confirmed, String reason) {
        Violation violation = violationRepository.findById(violationId)
                .orElseThrow(() -> new RuntimeException("Violation not found: " + violationId));
        
        violation.setConfirmed(confirmed);
        violation.setMessage(violation.getMessage() + " [Review: " + reason + "]");
        violationRepository.save(violation);
    }

    public void resetStrikeCount(Long sessionId, String reason) {
        List<Violation> violations = getSessionViolations(sessionId);
        violations.forEach(v -> {
            v.setStrikeCount(0);
            v.setMessage(v.getMessage() + " [Reset: " + reason + "]");
        });
        violationRepository.saveAll(violations);
    }

    public void processViolation(Violation v, StudentStatus status) {
        // Update counts based on violation type
        int currentViolations = status.getViolationCount() != null ? status.getViolationCount() : 0;
        status.setViolationCount(currentViolations + 1);

        // Update status color based on simple logic
        if (status.getViolationCount() > 5) {
            status.setStatusColor(StudentStatus.StatusColor.RED);
        } else if (status.getViolationCount() > 2) {
            status.setStatusColor(StudentStatus.StatusColor.YELLOW);
        } else {
            status.setStatusColor(StudentStatus.StatusColor.GREEN);
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ViolationStats {
        private int totalViolations;
        private int totalStrikes;
        private int confirmedViolations;
        private int pendingReview;
        private int cameraViolations;
        private int tabSwitchCount;
        private boolean terminated;
        private Map<Violation.ViolationType, Long> violationsByType;
    }
}

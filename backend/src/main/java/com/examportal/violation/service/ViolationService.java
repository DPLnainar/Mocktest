package com.examportal.violation.service;

import com.examportal.entity.StudentAttempt;
import com.examportal.entity.AttemptStatus;
import com.examportal.monitoring.model.StudentStatus;
import com.examportal.repository.StudentAttemptRepository;
import com.examportal.violation.entity.Violation;
import com.examportal.violation.repository.ViolationRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ViolationService {

    private static final Logger log = LoggerFactory.getLogger(ViolationService.class);

    @Autowired
    private ViolationRepository violationRepository;

    @Autowired
    private StudentAttemptRepository attemptRepository;

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

    public long countTotalViolations() {
        return violationRepository.count();
    }

    @SuppressWarnings("unchecked")
    public ViolationRecordResult recordViolation(Long sessionId, Long studentId, Long examId,
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
                .evidence(evidence instanceof java.util.Map ? (java.util.Map<String, Object>) evidence : null)
                .timestamp(LocalDateTime.now())
                .detectedAt(LocalDateTime.now())
                .confirmed(false)
                .strikeCount(1)
                .build();

        violationRepository.save(java.util.Objects.requireNonNull(violation));

        // Check if test should be frozen
        checkAndFreezeAttempt(sessionId, type);

        // Check actual status
        boolean isFrozen = false;
        try {
            StudentAttempt attempt = attemptRepository.findById(sessionId).orElse(null);
            if (attempt != null && attempt.getStatus() == AttemptStatus.FROZEN) {
                isFrozen = true;
            }
        } catch (Exception e) {
            log.error("Failed to check status", e);
        }

        return new ViolationRecordResult(getStrikeCount(sessionId), isFrozen);
    }

    public record ViolationRecordResult(int strikeCount, boolean isFrozen) {
    }

    /**
     * Check if attempt should be frozen based on violation type or threshold
     */
    private void checkAndFreezeAttempt(Long sessionId, Violation.ViolationType type) {
        // Check for immediate freeze violations
        if (shouldFreezeImmediately(type)) {
            freezeAttempt(sessionId, "Critical violation detected: " + type);
            return;
        }

        // Check threshold-based freeze
        int totalViolations = getStrikeCount(sessionId);
        if (totalViolations >= 5) {
            freezeAttempt(sessionId, "Too many violations (" + totalViolations + " strikes)");
        }
    }

    /**
     * Determine if violation type should trigger immediate freeze
     */
    private boolean shouldFreezeImmediately(Violation.ViolationType type) {
        return type == Violation.ViolationType.FULLSCREEN_EXIT ||
                type == Violation.ViolationType.CAMERA_DETECTED ||
                type == Violation.ViolationType.SCREENSHOT_ATTEMPT;
    }

    /**
     * Freeze the test attempt
     */
    private void freezeAttempt(Long sessionId, String reason) {
        try {
            StudentAttempt attempt = attemptRepository.findById(sessionId)
                    .orElseThrow(() -> new RuntimeException("Attempt not found: " + sessionId));

            // Only freeze if not already frozen or submitted
            if (attempt.getStatus() != AttemptStatus.FROZEN &&
                    attempt.getStatus() != AttemptStatus.SUBMITTED) {

                attempt.setStatus(AttemptStatus.FROZEN);
                attempt.setFreezeReason(reason);
                attempt.setFrozenAt(LocalDateTime.now());
                attemptRepository.save(attempt);

                log.warn("Test frozen - Session: {}, Reason: {}", sessionId, reason);
            }
        } catch (Exception e) {
            log.error("Failed to freeze attempt {}: {}", sessionId, e.getMessage());
        }
    }

    public ViolationStats getViolationStats(Long sessionId) {
        List<Violation> violations = getSessionViolations(sessionId);

        Map<Violation.ViolationType, Long> typeCount = violations.stream()
                .collect(Collectors.groupingBy(Violation::getType, Collectors.counting()));

        int totalStrikes = getStrikeCount(sessionId);
        int confirmedCount = (int) violations.stream().filter(v -> Boolean.TRUE.equals(v.getConfirmed())).count();

        // Calculate camera violations (PHONE_DETECTED, NO_FACE_DETECTED,
        // MULTIPLE_FACES, NO_FACE, UNKNOWN_FACE)
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

        return new ViolationStats(
                violations.size(),
                totalStrikes,
                confirmedCount,
                violations.size() - confirmedCount,
                cameraViolations,
                tabSwitchCount,
                terminated,
                typeCount);
    }

    public void updateViolationConfirmation(Long violationId, boolean confirmed, String reason) {
        Violation violation = violationRepository.findById(java.util.Objects.requireNonNull(violationId))
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

    public record ViolationStats(
            int totalViolations,
            int totalStrikes,
            int confirmedViolations,
            int pendingReview,
            int cameraViolations,
            int tabSwitchCount,
            boolean terminated,
            Map<Violation.ViolationType, Long> violationsByType) {
    }
}

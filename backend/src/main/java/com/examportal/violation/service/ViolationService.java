package com.examportal.violation.service;

import com.examportal.monitoring.model.ExamSession;
import com.examportal.monitoring.model.StudentStatus;
import com.examportal.monitoring.service.MonitoringBroadcastService;
import com.examportal.monitoring.service.SessionManagerService;
import com.examportal.violation.entity.Violation;
import com.examportal.violation.event.ViolationEvent;
import com.examportal.violation.repository.ViolationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Violation Service
 * 
 * Core violation tracking with Redis atomic counters
 * 
 * Architecture:
 * 1. Violation detected → Redis INCR (atomic, race-free)
 * 2. Save to PostgreSQL (evidence storage)
 * 3. Broadcast to moderators
 * 4. Check if strikes >= 5 → Auto-terminate
 * 
 * The "Power Move": Redis INCR prevents race conditions
 * If phone + tab switch detected at exact same millisecond, both count as strikes
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ViolationService {

    private final ViolationRepository violationRepository;
    private final StringRedisTemplate redisTemplate;
    private final SessionManagerService sessionManager;
    private final MonitoringBroadcastService broadcastService;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${violation.max-strikes:5}")
    private int maxStrikes;

    private static final String STRIKE_COUNTER_PREFIX = "exam:session:strikes:";

    /**
     * Record a violation
     * Uses Redis atomic INCR for race-free counting
     * 
     * @return Total strike count after this violation
     */
    @Transactional
    public int recordViolation(Long sessionId, Long studentId, Long examId,
                              Violation.ViolationType type, Violation.Severity severity,
                              String description, Map<String, Object> evidence) {
        
        log.warn("Recording {} violation for student {} in session {}: {}", 
                severity, studentId, sessionId, type);

        // 1. Atomic increment in Redis (THE POWER MOVE)
        String redisKey = STRIKE_COUNTER_PREFIX + sessionId;
        Long newStrikeCount = redisTemplate.opsForValue().increment(redisKey, severity.getStrikeCount());
        
        // Set TTL if this is the first strike
        if (newStrikeCount == severity.getStrikeCount()) {
            redisTemplate.expire(redisKey, 4, TimeUnit.HOURS);
        }

        log.info("Strike count for session {}: {} (added {})", 
                sessionId, newStrikeCount, severity.getStrikeCount());

        // 2. Save to PostgreSQL for evidence storage
        Violation violation = Violation.builder()
                .sessionId(sessionId)
                .studentId(studentId)
                .examId(examId)
                .type(type)
                .severity(severity)
                .description(description)
                .evidence(evidence)
                .confirmed(true)
                .detectedAt(LocalDateTime.now())
                .build();

        violationRepository.save(violation);

        // 3. Update session violation count
        sessionManager.updateViolationCount(sessionId, newStrikeCount.intValue());

        // 4. Publish event for async processing
        ViolationEvent event = new ViolationEvent(
                this, sessionId, studentId, examId, type, severity, description, evidence
        );
        eventPublisher.publishEvent(event);

        // 5. Broadcast to moderators
        broadcastViolationToModerators(examId, studentId, type, description, newStrikeCount.intValue());

        // 6. Check for auto-termination
        if (newStrikeCount >= maxStrikes) {
            log.warn("Strike limit reached for session {}. Auto-terminating...", sessionId);
            terminateSession(sessionId, studentId, examId, "Automatic termination: " + maxStrikes + " strikes");
        }

        return newStrikeCount.intValue();
    }

    /**
     * Get current strike count from Redis
     */
    public int getStrikeCount(Long sessionId) {
        String redisKey = STRIKE_COUNTER_PREFIX + sessionId;
        String count = redisTemplate.opsForValue().get(redisKey);
        return count != null ? Integer.parseInt(count) : 0;
    }

    /**
     * Get violations for a session
     */
    public List<Violation> getSessionViolations(Long sessionId) {
        return violationRepository.findBySessionIdOrderByDetectedAtDesc(sessionId);
    }

    /**
     * Get violations for a student
     */
    public List<Violation> getStudentViolations(Long studentId) {
        return violationRepository.findByStudentIdOrderByDetectedAtDesc(studentId);
    }

    /**
     * Get violations for an exam
     */
    public List<Violation> getExamViolations(Long examId) {
        return violationRepository.findByExamIdOrderByDetectedAtDesc(examId);
    }

    /**
     * Reset strike count (for appeals)
     */
    @Transactional
    public void resetStrikeCount(Long sessionId, String reason) {
        log.info("Resetting strike count for session {}: {}", sessionId, reason);
        
        String redisKey = STRIKE_COUNTER_PREFIX + sessionId;
        redisTemplate.delete(redisKey);
        
        sessionManager.updateViolationCount(sessionId, 0);
    }

    /**
     * Confirm or reject violation (for false positives)
     */
    @Transactional
    public void updateViolationConfirmation(Long violationId, boolean confirmed, String reason) {
        Violation violation = violationRepository.findById(violationId)
                .orElseThrow(() -> new IllegalArgumentException("Violation not found"));

        if (violation.getConfirmed() == confirmed) {
            return; // No change
        }

        violation.setConfirmed(confirmed);
        violationRepository.save(violation);

        // Adjust Redis counter
        String redisKey = STRIKE_COUNTER_PREFIX + violation.getSessionId();
        if (confirmed) {
            // Add strikes back
            redisTemplate.opsForValue().increment(redisKey, violation.getStrikeCount());
        } else {
            // Remove strikes
            redisTemplate.opsForValue().decrement(redisKey, violation.getStrikeCount());
        }

        log.info("Violation {} {} - Reason: {}", 
                violationId, confirmed ? "confirmed" : "rejected", reason);
    }

    /**
     * Auto-terminate session
     */
    private void terminateSession(Long sessionId, Long studentId, Long examId, String reason) {
        // Update session status
        sessionManager.terminateSession(sessionId);

        // Notify student
        broadcastService.sendToStudent(
                studentId, 
                "termination",
                Map.of(
                    "reason", reason,
                    "terminatedAt", LocalDateTime.now().toString(),
                    "strikes", maxStrikes
                )
        );

        // Broadcast to moderators
        broadcastService.broadcastTermination(examId, studentId, reason);

        // Update student status
        StudentStatus status = StudentStatus.builder()
                .studentId(studentId)
                .sessionId(sessionId)
                .activityStatus(StudentStatus.ActivityStatus.TERMINATED)
                .statusColor(StudentStatus.StatusColor.RED)
                .violationCount(maxStrikes)
                .lastActivity(LocalDateTime.now())
                .build();

        broadcastService.broadcastStudentStatus(examId, status);

        log.warn("Session {} auto-terminated for student {}: {}", sessionId, studentId, reason);
    }

    /**
     * Broadcast violation alert to moderators
     */
    private void broadcastViolationToModerators(Long examId, Long studentId, 
                                                Violation.ViolationType type, 
                                                String description, int totalStrikes) {
        MonitoringBroadcastService.ViolationAlert alert = 
                new MonitoringBroadcastService.ViolationAlert(
                        studentId,
                        type.name(),
                        description + " (Total strikes: " + totalStrikes + ")",
                        System.currentTimeMillis()
                );

        broadcastService.broadcastViolationAlert(examId, alert);

        // Also update student status with new violation count
        StudentStatus status = StudentStatus.builder()
                .studentId(studentId)
                .violationCount(totalStrikes)
                .statusColor(StudentStatus.calculateStatusColor(totalStrikes))
                .lastActivity(LocalDateTime.now())
                .build();

        broadcastService.broadcastStudentStatus(examId, status);
    }

    /**
     * Get violation statistics
     */
    public ViolationStats getViolationStats(Long sessionId) {
        int strikeCount = getStrikeCount(sessionId);
        List<Violation> violations = getSessionViolations(sessionId);
        
        long cameraViolations = violations.stream()
                .filter(v -> v.getType() == Violation.ViolationType.MULTIPLE_FACES ||
                           v.getType() == Violation.ViolationType.NO_FACE_DETECTED ||
                           v.getType() == Violation.ViolationType.PHONE_DETECTED)
                .count();

        long tabSwitchCount = violations.stream()
                .filter(v -> v.getType() == Violation.ViolationType.TAB_SWITCH)
                .count();

        return new ViolationStats(
                strikeCount,
                violations.size(),
                cameraViolations,
                tabSwitchCount,
                strikeCount >= maxStrikes
        );
    }

    public record ViolationStats(
            int totalStrikes,
            int totalViolations,
            long cameraViolations,
            long tabSwitchCount,
            boolean terminated
    ) {}
}

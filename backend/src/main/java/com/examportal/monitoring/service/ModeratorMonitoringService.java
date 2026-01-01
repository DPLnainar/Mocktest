package com.examportal.monitoring.service;

import com.examportal.monitoring.dto.MonitoringUpdate;
import com.examportal.monitoring.dto.StudentStatusDTO;
import com.examportal.session.entity.SessionManager;
import com.examportal.session.repository.SessionManagerRepository;
import com.examportal.violation.entity.Violation;
import com.examportal.violation.repository.ViolationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ModeratorMonitoringService
 * 
 * Manages War Room dashboard functionality
 * Broadcasts real-time updates to moderators
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ModeratorMonitoringService {

    private final SessionManagerRepository sessionRepository;
    private final ViolationRepository violationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Handle moderator connection
     * Send initial batch of student status
     */
    @Transactional(readOnly = true)
    public void handleModeratorConnect(String moderatorId, Long examId) {
        log.info("Sending initial data to moderator {} for exam {}", moderatorId, examId);

        // Get all sessions for exam
        List<SessionManager> sessions = sessionRepository.findByExamId(examId);

        // Build student status list
        List<StudentStatusDTO> studentStatuses = sessions.stream()
                .map(this::buildStudentStatus)
                .collect(Collectors.toList());

        // Send batch update
        MonitoringUpdate update = MonitoringUpdate.batchStatus(studentStatuses);
        
        messagingTemplate.convertAndSend(
                "/topic/exam/" + examId + "/monitoring",
                update
        );

        log.info("Sent {} student statuses to moderator", studentStatuses.size());
    }

    /**
     * Build student status DTO
     */
    private StudentStatusDTO buildStudentStatus(SessionManager session) {
        // Get strike count from Redis
        String redisKey = "exam:session:strikes:" + session.getId();
        String strikeCountStr = redisTemplate.opsForValue().get(redisKey);
        int violationCount = strikeCountStr != null ? Integer.parseInt(strikeCountStr) : 0;

        // Determine color
        String statusColor = violationCount >= 4 ? "RED" :
                           violationCount >= 2 ? "YELLOW" : "GREEN";

        return StudentStatusDTO.builder()
                .studentId(session.getStudentId())
                .studentName(session.getStudentName())
                .department(session.getDepartment())
                .connectionStatus(session.getConnectionStatus().name())
                .activityStatus(session.getActivityStatus().name())
                .violationCount(violationCount)
                .statusColor(statusColor)
                .lastActivity(session.getLastActivity())
                .lastHeartbeat(session.getLastHeartbeat())
                .currentLanguage(session.getCurrentLanguage())
                .linesOfCode(session.getLinesOfCode())
                .ipAddress(session.getIpAddress())
                .userAgent(session.getUserAgent())
                .build();
    }

    /**
     * Broadcast student status update
     */
    public void broadcastStudentStatus(SessionManager session) {
        StudentStatusDTO status = buildStudentStatus(session);
        
        MonitoringUpdate update = MonitoringUpdate.studentStatus(status);
        
        messagingTemplate.convertAndSend(
                "/topic/exam/" + session.getExamId() + "/monitoring",
                update
        );
    }

    /**
     * Broadcast violation alert
     */
    public void broadcastViolationAlert(Violation violation, String studentName, int totalStrikes) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("violationId", violation.getId());
        payload.put("studentId", violation.getStudentId());
        payload.put("studentName", studentName);
        payload.put("violationType", violation.getViolationType().name());
        payload.put("message", violation.getMessage());
        payload.put("totalStrikes", totalStrikes);
        payload.put("timestamp", violation.getDetectedAt());

        MonitoringUpdate update = MonitoringUpdate.violationAlert(payload);
        
        messagingTemplate.convertAndSend(
                "/topic/exam/" + violation.getExamId() + "/monitoring",
                update
        );
    }

    /**
     * Terminate student (moderator action)
     */
    @Transactional
    public void terminateStudent(Long studentId, String moderatorId, String reason) {
        log.warn("Moderator {} terminating student {}: {}", moderatorId, studentId, reason);

        // Find active session
        SessionManager session = sessionRepository.findByStudentIdAndActivityStatus(
                studentId,
                SessionManager.ActivityStatus.ACTIVE
        ).orElseThrow(() -> new RuntimeException("Active session not found"));

        // Update session
        session.setActivityStatus(SessionManager.ActivityStatus.TERMINATED);
        session.setTerminatedAt(LocalDateTime.now());
        session.setTerminationReason("Moderator termination: " + reason);
        sessionRepository.save(session);

        // Broadcast termination
        Map<String, Object> payload = new HashMap<>();
        payload.put("studentId", studentId);
        payload.put("studentName", session.getStudentName());
        payload.put("reason", reason);
        payload.put("moderatorId", moderatorId);
        payload.put("timestamp", System.currentTimeMillis());

        MonitoringUpdate update = MonitoringUpdate.termination(payload);
        
        messagingTemplate.convertAndSend(
                "/topic/exam/" + session.getExamId() + "/monitoring",
                update
        );

        // Send termination message to student
        Map<String, Object> studentPayload = new HashMap<>();
        studentPayload.put("type", "termination");
        studentPayload.put("reason", reason);
        
        messagingTemplate.convertAndSendToUser(
                String.valueOf(studentId),
                "/queue/messages",
                studentPayload
        );

        log.info("Student {} terminated by moderator {}", studentId, moderatorId);
    }

    /**
     * Send warning to student
     */
    public void sendWarning(Long studentId, String moderatorId, String message) {
        log.info("Moderator {} sending warning to student {}: {}", moderatorId, studentId, message);

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "warning");
        payload.put("message", message);
        payload.put("moderatorId", moderatorId);
        payload.put("timestamp", System.currentTimeMillis());

        messagingTemplate.convertAndSendToUser(
                String.valueOf(studentId),
                "/queue/messages",
                payload
        );

        log.info("Warning sent to student {}", studentId);
    }
}

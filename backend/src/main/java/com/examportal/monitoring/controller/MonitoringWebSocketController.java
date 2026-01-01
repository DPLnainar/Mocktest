package com.examportal.monitoring.controller;

import com.examportal.monitoring.model.ExamSession;
import com.examportal.monitoring.model.StudentStatus;
import com.examportal.monitoring.service.MonitoringBroadcastService;
import com.examportal.monitoring.service.SessionManagerService;
import com.examportal.security.CustomUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

/**
 * WebSocket Controller
 * 
 * Handles real-time WebSocket communication for exam monitoring
 * 
 * Client messages go to /app/... (application prefix)
 * Server broadcasts go to /topic/... (topic prefix)
 * Private messages go to /user/.../queue/... (user prefix)
 */
@Controller
public class MonitoringWebSocketController {

    private static final Logger log = LoggerFactory.getLogger(MonitoringWebSocketController.class);

    private final SessionManagerService sessionManager;
    private final MonitoringBroadcastService broadcastService;

    public MonitoringWebSocketController(SessionManagerService sessionManager, MonitoringBroadcastService broadcastService) {
        this.sessionManager = sessionManager;
        this.broadcastService = broadcastService;
    }

    /**
     * Student connects to exam session
     * 
     * Client sends: /app/exam/{examId}/connect
     * Server broadcasts: /topic/exam/{examId}/monitoring
     */
    @MessageMapping("/exam/{examId}/connect")
    @SendTo("/topic/exam/{examId}/monitoring")
    public StudentStatus handleConnect(
            @DestinationVariable Long examId,
            @Payload ConnectMessage message,
            SimpMessageHeaderAccessor headerAccessor) {
        
        String webSocketSessionId = headerAccessor.getSessionId();
        log.info("Student {} connecting to exam {} via WebSocket {}", 
                message.getStudentId(), examId, webSocketSessionId);

        // Get or create exam session
        ExamSession session = sessionManager.getSession(message.getSessionId());
        if (session != null) {
            // Update WebSocket session ID (for reconnection)
            sessionManager.updateWebSocketSession(message.getSessionId(), webSocketSessionId);
        }

        // Broadcast connection status
        StudentStatus status = StudentStatus.builder()
                .studentId(message.getStudentId())
                .studentName(message.getStudentName())
                .sessionId(message.getSessionId())
                .connectionStatus(StudentStatus.ConnectionStatus.ONLINE)
                .activityStatus(StudentStatus.ActivityStatus.IDLE)
                .violationCount(0)
                .statusColor(StudentStatus.StatusColor.GREEN)
                .lastActivity(LocalDateTime.now())
                .cameraStatus(StudentStatus.CameraStatus.ACTIVE)
                .tabSwitchCount(0)
                .build();

        return status;
    }

    /**
     * Student disconnects from exam
     */
    @MessageMapping("/exam/{examId}/disconnect")
    public void handleDisconnect(
            @DestinationVariable Long examId,
            @Payload DisconnectMessage message) {
        
        log.info("Student {} disconnecting from exam {}", message.getStudentId(), examId);

        // Broadcast offline status
        StudentStatus status = StudentStatus.builder()
                .studentId(message.getStudentId())
                .connectionStatus(StudentStatus.ConnectionStatus.OFFLINE)
                .lastActivity(LocalDateTime.now())
                .build();

        broadcastService.broadcastStudentStatus(examId, status);
    }

    /**
     * Heartbeat from student (every 10 seconds)
     * Keeps connection alive and updates last activity
     */
    @MessageMapping("/exam/{examId}/heartbeat")
    public void handleHeartbeat(
            @DestinationVariable Long examId,
            @Payload HeartbeatMessage message) {
        
        log.trace("Heartbeat from student {} in exam {}", message.getStudentId(), examId);

        // Update session heartbeat in Redis
        sessionManager.updateHeartbeat(message.getSessionId());

        // Broadcast activity update if status changed
        if (message.getActivityStatus() != null) {
            StudentStatus status = StudentStatus.builder()
                    .studentId(message.getStudentId())
                    .sessionId(message.getSessionId())
                    .activityStatus(message.getActivityStatus())
                    .currentQuestion(message.getCurrentQuestion())
                    .lastActivity(LocalDateTime.now())
                    .build();

            broadcastService.broadcastStudentStatus(examId, status);
        }
    }

    /**
     * Student activity update (typing, submitting, etc.)
     */
    @MessageMapping("/exam/{examId}/activity")
    public void handleActivity(
            @DestinationVariable Long examId,
            @Payload ActivityMessage message) {
        
        log.debug("Activity update from student {} in exam {}: {}", 
                message.getStudentId(), examId, message.getActivityType());

        StudentStatus status = StudentStatus.builder()
                .studentId(message.getStudentId())
                .sessionId(message.getSessionId())
                .activityStatus(message.getActivityType())
                .currentQuestion(message.getCurrentQuestion())
                .lastActivity(LocalDateTime.now())
                .build();

        broadcastService.broadcastStudentStatus(examId, status);
    }

    /**
     * Moderator requests full student list
     */
    @MessageMapping("/exam/{examId}/moderator/request-status")
    public void handleStatusRequest(
            @DestinationVariable Long examId,
            @AuthenticationPrincipal CustomUserDetails moderator) {
        
        log.info("Moderator {} requesting status for exam {}", moderator.getId(), examId);

        // Get all active sessions for this exam
        var sessions = sessionManager.getActiveSessionsForExam(examId);
        
        // Convert to StudentStatus and broadcast
        var statusList = sessions.stream()
                .map(this::convertToStudentStatus)
                .toList();

        broadcastService.broadcastBatchStatus(examId, statusList);
    }

    /**
     * Moderator terminates student exam
     */
    @MessageMapping("/exam/{examId}/moderator/terminate")
    public void handleModeratorTermination(
            @DestinationVariable Long examId,
            @Payload TerminationRequest request,
            @AuthenticationPrincipal CustomUserDetails moderator) {
        
        log.warn("Moderator {} terminating student {} in exam {}: {}", 
                moderator.getId(), request.getStudentId(), examId, request.getReason());

        // Update session status
        sessionManager.terminateSession(request.getSessionId());

        // Notify student
        broadcastService.sendToStudent(
                request.getStudentId(), 
                "termination", 
                new TerminationNotice(request.getReason(), LocalDateTime.now())
        );

        // Broadcast to moderators
        broadcastService.broadcastTermination(examId, request.getStudentId(), request.getReason());

        // Broadcast updated status
        StudentStatus status = new StudentStatus();
        status.setStudentId(request.getStudentId());
        status.setSessionId(request.getSessionId());
        status.setActivityStatus(StudentStatus.ActivityStatus.TERMINATED);
        status.setStatusColor(StudentStatus.StatusColor.RED);
        status.setLastActivity(LocalDateTime.now());

        broadcastService.broadcastStudentStatus(examId, status);
    }

    /**
     * Convert ExamSession to StudentStatus
     */
    private StudentStatus convertToStudentStatus(ExamSession session) {
        StudentStatus status = new StudentStatus();
        status.setStudentId(session.getStudentId());
        status.setStudentName(session.getStudentName());
        status.setSessionId(session.getId());
        status.setConnectionStatus(session.isHeartbeatStale() ? 
                StudentStatus.ConnectionStatus.OFFLINE : 
                StudentStatus.ConnectionStatus.ONLINE);
        status.setActivityStatus(StudentStatus.ActivityStatus.IDLE);
        status.setViolationCount(session.getViolationCount());
        // Calculate status color based on violation count
        int violations = session.getViolationCount() != null ? session.getViolationCount() : 0;
        if (violations > 5) {
            status.setStatusColor(StudentStatus.StatusColor.RED);
        } else if (violations > 2) {
            status.setStatusColor(StudentStatus.StatusColor.YELLOW);
        } else {
            status.setStatusColor(StudentStatus.StatusColor.GREEN);
        }
        status.setLastActivity(session.getLastHeartbeat());
        return status;
    }

    // Message DTOs
    public static class ConnectMessage {
        private Long studentId;
        private String studentName;
        private Long sessionId;

        public ConnectMessage() {}
        public ConnectMessage(Long studentId, String studentName, Long sessionId) {
            this.studentId = studentId;
            this.studentName = studentName;
            this.sessionId = sessionId;
        }

        public Long getStudentId() { return studentId; }
        public void setStudentId(Long studentId) { this.studentId = studentId; }
        public String getStudentName() { return studentName; }
        public void setStudentName(String studentName) { this.studentName = studentName; }
        public Long getSessionId() { return sessionId; }
        public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    }

    public static class DisconnectMessage {
        private Long studentId;
        private Long sessionId;

        public DisconnectMessage() {}
        public DisconnectMessage(Long studentId, Long sessionId) {
            this.studentId = studentId;
            this.sessionId = sessionId;
        }

        public Long getStudentId() { return studentId; }
        public void setStudentId(Long studentId) { this.studentId = studentId; }
        public Long getSessionId() { return sessionId; }
        public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    }

    public static class HeartbeatMessage {
        private Long studentId;
        private Long sessionId;
        private StudentStatus.ActivityStatus activityStatus;
        private Integer currentQuestion;

        public HeartbeatMessage() {}
        public HeartbeatMessage(Long studentId, Long sessionId, StudentStatus.ActivityStatus activityStatus, Integer currentQuestion) {
            this.studentId = studentId;
            this.sessionId = sessionId;
            this.activityStatus = activityStatus;
            this.currentQuestion = currentQuestion;
        }

        public Long getStudentId() { return studentId; }
        public void setStudentId(Long studentId) { this.studentId = studentId; }
        public Long getSessionId() { return sessionId; }
        public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
        public StudentStatus.ActivityStatus getActivityStatus() { return activityStatus; }
        public void setActivityStatus(StudentStatus.ActivityStatus activityStatus) { this.activityStatus = activityStatus; }
        public Integer getCurrentQuestion() { return currentQuestion; }
        public void setCurrentQuestion(Integer currentQuestion) { this.currentQuestion = currentQuestion; }
    }

    public static class ActivityMessage {
        private Long studentId;
        private Long sessionId;
        private StudentStatus.ActivityStatus activityType;
        private Integer currentQuestion;

        public ActivityMessage() {}
        public ActivityMessage(Long studentId, Long sessionId, StudentStatus.ActivityStatus activityType, Integer currentQuestion) {
            this.studentId = studentId;
            this.sessionId = sessionId;
            this.activityType = activityType;
            this.currentQuestion = currentQuestion;
        }

        public Long getStudentId() { return studentId; }
        public void setStudentId(Long studentId) { this.studentId = studentId; }
        public Long getSessionId() { return sessionId; }
        public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
        public StudentStatus.ActivityStatus getActivityType() { return activityType; }
        public void setActivityType(StudentStatus.ActivityStatus activityType) { this.activityType = activityType; }
        public Integer getCurrentQuestion() { return currentQuestion; }
        public void setCurrentQuestion(Integer currentQuestion) { this.currentQuestion = currentQuestion; }
    }

    public static class TerminationRequest {
        private Long studentId;
        private Long sessionId;
        private String reason;

        public TerminationRequest() {}
        public TerminationRequest(Long studentId, Long sessionId, String reason) {
            this.studentId = studentId;
            this.sessionId = sessionId;
            this.reason = reason;
        }

        public Long getStudentId() { return studentId; }
        public void setStudentId(Long studentId) { this.studentId = studentId; }
        public Long getSessionId() { return sessionId; }
        public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class TerminationNotice {
        private String reason;
        private LocalDateTime terminatedAt;

        public TerminationNotice() {}
        public TerminationNotice(String reason, LocalDateTime terminatedAt) {
            this.reason = reason;
            this.terminatedAt = terminatedAt;
        }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public LocalDateTime getTerminatedAt() { return terminatedAt; }
        public void setTerminatedAt(LocalDateTime terminatedAt) { this.terminatedAt = terminatedAt; }
    }
}

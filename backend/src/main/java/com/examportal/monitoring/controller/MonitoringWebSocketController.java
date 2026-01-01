package com.examportal.monitoring.controller;

import com.examportal.monitoring.model.ExamSession;
import com.examportal.monitoring.model.StudentStatus;
import com.examportal.monitoring.service.MonitoringBroadcastService;
import com.examportal.monitoring.service.SessionManagerService;
import com.examportal.security.CustomUserDetails;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequiredArgsConstructor
@Slf4j
public class MonitoringWebSocketController {

    private final SessionManagerService sessionManager;
    private final MonitoringBroadcastService broadcastService;

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
        StudentStatus status = StudentStatus.builder()
                .studentId(request.getStudentId())
                .sessionId(request.getSessionId())
                .activityStatus(StudentStatus.ActivityStatus.TERMINATED)
                .statusColor(StudentStatus.StatusColor.RED)
                .lastActivity(LocalDateTime.now())
                .build();

        broadcastService.broadcastStudentStatus(examId, status);
    }

    /**
     * Convert ExamSession to StudentStatus
     */
    private StudentStatus convertToStudentStatus(ExamSession session) {
        return StudentStatus.builder()
                .studentId(session.getStudentId())
                .studentName(session.getStudentName())
                .sessionId(session.getId())
                .connectionStatus(session.isHeartbeatStale() ? 
                        StudentStatus.ConnectionStatus.OFFLINE : 
                        StudentStatus.ConnectionStatus.ONLINE)
                .activityStatus(StudentStatus.ActivityStatus.IDLE)
                .violationCount(session.getViolationCount())
                .statusColor(StudentStatus.calculateStatusColor(session.getViolationCount()))
                .lastActivity(session.getLastHeartbeat())
                .build();
    }

    // Message DTOs
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConnectMessage {
        private Long studentId;
        private String studentName;
        private Long sessionId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DisconnectMessage {
        private Long studentId;
        private Long sessionId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HeartbeatMessage {
        private Long studentId;
        private Long sessionId;
        private StudentStatus.ActivityStatus activityStatus;
        private Integer currentQuestion;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityMessage {
        private Long studentId;
        private Long sessionId;
        private StudentStatus.ActivityStatus activityType;
        private Integer currentQuestion;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TerminationRequest {
        private Long studentId;
        private Long sessionId;
        private String reason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TerminationNotice {
        private String reason;
        private LocalDateTime terminatedAt;
    }
}

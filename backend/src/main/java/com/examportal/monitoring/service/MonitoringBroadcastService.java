package com.examportal.monitoring.service;

import com.examportal.monitoring.model.StudentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Monitoring Broadcast Service
 * 
 * Broadcasts real-time updates to moderator dashboards via WebSocket
 * Uses RabbitMQ for horizontal scaling - broadcasts reach all server instances
 * 
 * Target: <1s latency from event to moderator screen
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MonitoringBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Broadcast student status to exam monitoring channel
     * Moderators subscribed to /topic/exam/{examId}/monitoring receive this
     * 
     * @param examId Exam ID
     * @param status Student status update
     */
    public void broadcastStudentStatus(Long examId, StudentStatus status) {
        String destination = "/topic/exam/" + examId + "/monitoring";
        
        try {
            messagingTemplate.convertAndSend(destination, status);
            log.debug("Broadcast status for student {} to exam {}", status.getStudentId(), examId);
        } catch (Exception e) {
            log.error("Error broadcasting student status", e);
        }
    }

    /**
     * Broadcast batch status updates (efficient for full dashboard refresh)
     */
    public void broadcastBatchStatus(Long examId, List<StudentStatus> statusList) {
        String destination = "/topic/exam/" + examId + "/monitoring";
        
        try {
            messagingTemplate.convertAndSend(destination, statusList);
            log.debug("Broadcast batch status ({} students) to exam {}", statusList.size(), examId);
        } catch (Exception e) {
            log.error("Error broadcasting batch status", e);
        }
    }

    /**
     * Send private message to specific student
     * Used for violation alerts, exam termination notices
     */
    public void sendToStudent(Long studentId, String messageType, Object payload) {
        String destination = "/user/" + studentId + "/queue/" + messageType;
        
        try {
            messagingTemplate.convertAndSend(destination, payload);
            log.debug("Sent {} message to student {}", messageType, studentId);
        } catch (Exception e) {
            log.error("Error sending message to student", e);
        }
    }

    /**
     * Broadcast violation alert to moderators
     */
    public void broadcastViolationAlert(Long examId, ViolationAlert alert) {
        String destination = "/topic/exam/" + examId + "/violations";
        
        try {
            messagingTemplate.convertAndSend(destination, alert);
            log.info("Broadcast violation alert for student {} to exam {}", alert.studentId(), examId);
        } catch (Exception e) {
            log.error("Error broadcasting violation alert", e);
        }
    }

    /**
     * Broadcast exam termination event
     */
    public void broadcastTermination(Long examId, Long studentId, String reason) {
        TerminationEvent event = new TerminationEvent(studentId, reason, System.currentTimeMillis());
        String destination = "/topic/exam/" + examId + "/terminations";
        
        try {
            messagingTemplate.convertAndSend(destination, event);
            log.info("Broadcast termination for student {} in exam {}", studentId, examId);
        } catch (Exception e) {
            log.error("Error broadcasting termination", e);
        }
    }

    /**
     * Send connection status update
     */
    public void broadcastConnectionStatus(Long examId, Long studentId, StudentStatus.ConnectionStatus status) {
        ConnectionStatusUpdate update = new ConnectionStatusUpdate(studentId, status, System.currentTimeMillis());
        String destination = "/topic/exam/" + examId + "/connections";
        
        try {
            messagingTemplate.convertAndSend(destination, update);
            log.debug("Broadcast connection status {} for student {}", status, studentId);
        } catch (Exception e) {
            log.error("Error broadcasting connection status", e);
        }
    }

    // DTOs for specialized messages
    public record ViolationAlert(Long studentId, String violationType, String description, long timestamp) {}
    public record TerminationEvent(Long studentId, String reason, long timestamp) {}
    public record ConnectionStatusUpdate(Long studentId, StudentStatus.ConnectionStatus status, long timestamp) {}
}

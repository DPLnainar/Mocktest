package com.examportal.monitoring.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Exam Session Info
 * 
 * Tracks active exam session for a student
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamSession {

    private Long id;
    private Long examId;
    private String examTitle;
    private Long studentId;
    private String studentName;
    private String department;
    
    private LocalDateTime startedAt;
    private LocalDateTime expiresAt;
    
    private SessionStatus status;
    private Integer violationCount;
    
    private String webSocketSessionId;
    private LocalDateTime lastHeartbeat;

    public enum SessionStatus {
        ACTIVE,
        TERMINATED,
        COMPLETED,
        EXPIRED
    }

    /**
     * Check if session has expired
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Check if heartbeat is stale (>30 seconds)
     */
    public boolean isHeartbeatStale() {
        if (lastHeartbeat == null) return true;
        return LocalDateTime.now().minusSeconds(30).isAfter(lastHeartbeat);
    }
}

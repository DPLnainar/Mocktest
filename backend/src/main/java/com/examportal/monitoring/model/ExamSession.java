package com.examportal.monitoring.model;

import java.time.LocalDateTime;

/**
 * Exam Session Model
 * 
 * Represents an active exam session in memory
 */
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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getExamId() { return examId; }
    public void setExamId(Long examId) { this.examId = examId; }
    public String getExamTitle() { return examTitle; }
    public void setExamTitle(String examTitle) { this.examTitle = examTitle; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public SessionStatus getStatus() { return status; }
    public void setStatus(SessionStatus status) { this.status = status; }
    public Integer getViolationCount() { return violationCount; }
    public void setViolationCount(Integer violationCount) { this.violationCount = violationCount; }
    public String getWebSocketSessionId() { return webSocketSessionId; }
    public void setWebSocketSessionId(String webSocketSessionId) { this.webSocketSessionId = webSocketSessionId; }
    public LocalDateTime getLastHeartbeat() { return lastHeartbeat; }
    public void setLastHeartbeat(LocalDateTime lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; }
}

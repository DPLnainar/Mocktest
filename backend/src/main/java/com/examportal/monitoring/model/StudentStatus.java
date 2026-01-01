package com.examportal.monitoring.model;

import java.time.LocalDateTime;

/**
 * Student Status Message
 * 
 * Real-time status updates broadcast to moderator dashboard
 * Includes activity status, violations, and connection health
 */
public class StudentStatus {

    /**
     * Student user ID
     */
    private Long studentId;

    /**
     * Student name
     */
    private String studentName;

    /**
     * Student email
     */
    private String email;

    /**
     * Current exam session ID
     */
    private Long sessionId;

    /**
     * Connection status
     */
    private ConnectionStatus connectionStatus;

    /**
     * Activity status (typing, idle, submitting, etc.)
     */
    private ActivityStatus activityStatus;

    /**
     * Current violation count (0-5)
     */
    private Integer violationCount;

    /**
     * Status color for dashboard (GREEN, YELLOW, RED)
     */
    private StatusColor statusColor;

    /**
     * Last activity timestamp
     */
    private LocalDateTime lastActivity;

    /**
     * Current question being attempted
     */
    private Integer currentQuestion;

    /**
     * Number of questions completed
     */
    private Integer completedQuestions;

    /**
     * Camera status
     */
    private CameraStatus cameraStatus;

    /**
     * Tab switch count
     */
    private Integer tabSwitchCount;

    public StudentStatus() {}

    public StudentStatus(Long studentId, String studentName, String email, Long sessionId, ConnectionStatus connectionStatus, ActivityStatus activityStatus, Integer violationCount, StatusColor statusColor, LocalDateTime lastActivity, Integer currentQuestion, Integer completedQuestions, CameraStatus cameraStatus, Integer tabSwitchCount) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.email = email;
        this.sessionId = sessionId;
        this.connectionStatus = connectionStatus;
        this.activityStatus = activityStatus;
        this.violationCount = violationCount;
        this.statusColor = statusColor;
        this.lastActivity = lastActivity;
        this.currentQuestion = currentQuestion;
        this.completedQuestions = completedQuestions;
        this.cameraStatus = cameraStatus;
        this.tabSwitchCount = tabSwitchCount;
    }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public ConnectionStatus getConnectionStatus() { return connectionStatus; }
    public void setConnectionStatus(ConnectionStatus connectionStatus) { this.connectionStatus = connectionStatus; }
    public ActivityStatus getActivityStatus() { return activityStatus; }
    public void setActivityStatus(ActivityStatus activityStatus) { this.activityStatus = activityStatus; }
    public Integer getViolationCount() { return violationCount; }
    public void setViolationCount(Integer violationCount) { this.violationCount = violationCount; }
    public StatusColor getStatusColor() { return statusColor; }
    public void setStatusColor(StatusColor statusColor) { this.statusColor = statusColor; }
    public LocalDateTime getLastActivity() { return lastActivity; }
    public void setLastActivity(LocalDateTime lastActivity) { this.lastActivity = lastActivity; }
    public Integer getCurrentQuestion() { return currentQuestion; }
    public void setCurrentQuestion(Integer currentQuestion) { this.currentQuestion = currentQuestion; }
    public Integer getCompletedQuestions() { return completedQuestions; }
    public void setCompletedQuestions(Integer completedQuestions) { this.completedQuestions = completedQuestions; }
    public CameraStatus getCameraStatus() { return cameraStatus; }
    public void setCameraStatus(CameraStatus cameraStatus) { this.cameraStatus = cameraStatus; }
    public Integer getTabSwitchCount() { return tabSwitchCount; }
    public void setTabSwitchCount(Integer tabSwitchCount) { this.tabSwitchCount = tabSwitchCount; }

    public enum ConnectionStatus {
        ONLINE,
        OFFLINE,
        RECONNECTING
    }

    public enum ActivityStatus {
        IDLE,
        TYPING,
        READING,
        SUBMITTING,
        TERMINATED
    }

    public enum StatusColor {
        GREEN,   // No violations, all good
        YELLOW,  // 1-4 violations, warning
        RED      // 5+ violations or terminated
    }

    public enum CameraStatus {
        ACTIVE,
        DISABLED,
        VIOLATION_DETECTED
    }

    /**
     * Calculate status color based on violations
     */
    public static StatusColor calculateStatusColor(int violations) {
        if (violations >= 5) return StatusColor.RED;
        if (violations > 0) return StatusColor.YELLOW;
        return StatusColor.GREEN;
    }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public ConnectionStatus getConnectionStatus() { return connectionStatus; }
    public void setConnectionStatus(ConnectionStatus connectionStatus) { this.connectionStatus = connectionStatus; }
    public ActivityStatus getActivityStatus() { return activityStatus; }
    public void setActivityStatus(ActivityStatus activityStatus) { this.activityStatus = activityStatus; }
    public Integer getViolationCount() { return violationCount; }
    public void setViolationCount(Integer violationCount) { this.violationCount = violationCount; }
    public StatusColor getStatusColor() { return statusColor; }
    public void setStatusColor(StatusColor statusColor) { this.statusColor = statusColor; }
    public LocalDateTime getLastActivity() { return lastActivity; }
    public void setLastActivity(LocalDateTime lastActivity) { this.lastActivity = lastActivity; }
    public Integer getCurrentQuestion() { return currentQuestion; }
    public void setCurrentQuestion(Integer currentQuestion) { this.currentQuestion = currentQuestion; }
    public Integer getCompletedQuestions() { return completedQuestions; }
    public void setCompletedQuestions(Integer completedQuestions) { this.completedQuestions = completedQuestions; }
    public CameraStatus getCameraStatus() { return cameraStatus; }
    public void setCameraStatus(CameraStatus cameraStatus) { this.cameraStatus = cameraStatus; }
    public Integer getTabSwitchCount() { return tabSwitchCount; }
    public void setTabSwitchCount(Integer tabSwitchCount) { this.tabSwitchCount = tabSwitchCount; }
}

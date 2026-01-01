package com.examportal.monitoring.dto;

import java.time.LocalDateTime;

/**
 * StudentStatusDTO
 * 
 * Real-time student status for moderator dashboard
 */
public class StudentStatusDTO {
    
    private Long studentId;
    private String studentName;
    private String department;
    
    private String connectionStatus; // CONNECTED, DISCONNECTED, INACTIVE
    private String activityStatus; // ACTIVE, IDLE, TERMINATED
    
    private Integer violationCount;
    private String statusColor; // GREEN (0-1), YELLOW (2-3), RED (4-5)
    
    private LocalDateTime lastActivity;
    private LocalDateTime lastHeartbeat;
    
    private String currentLanguage;
    private Integer linesOfCode;
    
    // Connection info
    private String ipAddress;
    private String userAgent;

    public StudentStatusDTO() {}

    public StudentStatusDTO(Long studentId, String studentName, String department, String connectionStatus, String activityStatus, Integer violationCount, String statusColor, LocalDateTime lastActivity, LocalDateTime lastHeartbeat, String currentLanguage, Integer linesOfCode, String ipAddress, String userAgent) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.department = department;
        this.connectionStatus = connectionStatus;
        this.activityStatus = activityStatus;
        this.violationCount = violationCount;
        this.statusColor = statusColor;
        this.lastActivity = lastActivity;
        this.lastHeartbeat = lastHeartbeat;
        this.currentLanguage = currentLanguage;
        this.linesOfCode = linesOfCode;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getConnectionStatus() { return connectionStatus; }
    public void setConnectionStatus(String connectionStatus) { this.connectionStatus = connectionStatus; }
    public String getActivityStatus() { return activityStatus; }
    public void setActivityStatus(String activityStatus) { this.activityStatus = activityStatus; }
    public Integer getViolationCount() { return violationCount; }
    public void setViolationCount(Integer violationCount) { this.violationCount = violationCount; }
    public String getStatusColor() { return statusColor; }
    public void setStatusColor(String statusColor) { this.statusColor = statusColor; }
    public LocalDateTime getLastActivity() { return lastActivity; }
    public void setLastActivity(LocalDateTime lastActivity) { this.lastActivity = lastActivity; }
    public LocalDateTime getLastHeartbeat() { return lastHeartbeat; }
    public void setLastHeartbeat(LocalDateTime lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; }
    public String getCurrentLanguage() { return currentLanguage; }
    public void setCurrentLanguage(String currentLanguage) { this.currentLanguage = currentLanguage; }
    public Integer getLinesOfCode() { return linesOfCode; }
    public void setLinesOfCode(Integer linesOfCode) { this.linesOfCode = linesOfCode; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
}

package com.examportal.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * StudentStatusDTO
 * 
 * Real-time student status for moderator dashboard
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
}

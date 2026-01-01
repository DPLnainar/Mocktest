package com.examportal.monitoring.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder // Added this
@NoArgsConstructor // Added this
@AllArgsConstructor // Added this
public class StudentStatus {
    
    private Long studentId;
    private String studentName;
    private String email;
    private Long sessionId;
    private ConnectionStatus connectionStatus;
    private ActivityStatus activityStatus;
    private Integer violationCount;
    private StatusColor statusColor;
    private LocalDateTime lastActivity;
    private Integer currentQuestion;
    private Integer completedQuestions;
    private CameraStatus cameraStatus;
    private Integer tabSwitchCount;

    public enum ConnectionStatus {
        ONLINE, OFFLINE, UNSTABLE
    }

    public enum ActivityStatus {
        ACTIVE, IDLE, SUSPICIOUS
    }

    public enum StatusColor {
        GREEN, YELLOW, RED
    }

    public enum CameraStatus {
        ACTIVE, INACTIVE, BLOCKED
    }
    
    // NO manual getters or setters here
}

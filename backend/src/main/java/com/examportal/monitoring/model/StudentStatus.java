package com.examportal.monitoring.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    // Enums
    public enum ConnectionStatus { ONLINE, OFFLINE, UNSTABLE }
    public enum ActivityStatus { ACTIVE, IDLE, SUSPICIOUS, TERMINATED }
    public enum StatusColor { GREEN, YELLOW, RED }
    public enum CameraStatus { ACTIVE, INACTIVE, BLOCKED }
    
    // logic helper for ViolationService
    public void calculateStatusColor(int strikes) {
        if (strikes > 5) this.statusColor = StatusColor.RED;
        else if (strikes > 2) this.statusColor = StatusColor.YELLOW;
        else this.statusColor = StatusColor.GREEN;
    }
}

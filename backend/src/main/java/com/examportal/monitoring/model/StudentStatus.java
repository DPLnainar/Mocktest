package com.examportal.monitoring.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Student Status Message
 * 
 * Real-time status updates broadcast to moderator dashboard
 * Includes activity status, violations, and connection health
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
}

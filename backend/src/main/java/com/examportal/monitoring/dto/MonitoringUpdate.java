package com.examportal.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MonitoringUpdate
 * 
 * WebSocket message wrapper for moderator updates
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitoringUpdate {
    
    private String type; // student_status, batch_status, violation_alert, termination, connection_status
    private Object payload;
    private Long timestamp;
    
    public static MonitoringUpdate studentStatus(StudentStatusDTO status) {
        return MonitoringUpdate.builder()
                .type("student_status")
                .payload(status)
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    public static MonitoringUpdate batchStatus(Object payload) {
        return MonitoringUpdate.builder()
                .type("batch_status")
                .payload(payload)
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    public static MonitoringUpdate violationAlert(Object payload) {
        return MonitoringUpdate.builder()
                .type("violation_alert")
                .payload(payload)
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    public static MonitoringUpdate termination(Object payload) {
        return MonitoringUpdate.builder()
                .type("termination")
                .payload(payload)
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    public static MonitoringUpdate connectionStatus(Object payload) {
        return MonitoringUpdate.builder()
                .type("connection_status")
                .payload(payload)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}

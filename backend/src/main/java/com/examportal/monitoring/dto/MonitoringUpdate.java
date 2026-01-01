package com.examportal.monitoring.dto;

/**
 * MonitoringUpdate
 * 
 * WebSocket message wrapper for moderator updates
 */
public class MonitoringUpdate {
    
    private String type; // student_status, batch_status, violation_alert, termination, connection_status
    private Object payload;
    private Long timestamp;

    public MonitoringUpdate() {}

    public MonitoringUpdate(String type, Object payload, Long timestamp) {
        this.type = type;
        this.payload = payload;
        this.timestamp = timestamp;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Object getPayload() { return payload; }
    public void setPayload(Object payload) { this.payload = payload; }
    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    
    public static MonitoringUpdate studentStatus(StudentStatusDTO status) {
        return new MonitoringUpdate("student_status", status, System.currentTimeMillis());
    }
    
    public static MonitoringUpdate batchStatus(Object payload) {
        return new MonitoringUpdate("batch_status", payload, System.currentTimeMillis());
    }
    
    public static MonitoringUpdate violationAlert(Object payload) {
        return new MonitoringUpdate("violation_alert", payload, System.currentTimeMillis());
    }
    
    public static MonitoringUpdate termination(Object payload) {
        return new MonitoringUpdate("termination", payload, System.currentTimeMillis());
    }
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

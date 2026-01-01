package com.examportal.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MonitoringUpdate {

    private String type;      // e.g., "STATUS", "HEARTBEAT", "VIOLATION"
    private Object payload;   // The data (StudentStatusDTO, etc.)
    private long timestamp;

    // Helper constructor for convenience
    public MonitoringUpdate(String type, Object payload) {
        this.type = type;
        this.payload = payload;
        this.timestamp = System.currentTimeMillis();
    }
}

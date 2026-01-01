package com.examportal.monitoring.dto;

import lombok.Data;

@Data
public class ModeratorTerminateRequest {
    private Long studentId;
    private String reason;
    private Long timestamp;
}

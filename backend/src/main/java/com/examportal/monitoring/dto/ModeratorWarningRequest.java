package com.examportal.monitoring.dto;

import lombok.Data;

@Data
public class ModeratorWarningRequest {
    private Long studentId;
    private String message;
    private Long timestamp;
}

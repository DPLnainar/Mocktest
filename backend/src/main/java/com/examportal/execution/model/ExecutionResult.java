package com.examportal.execution.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionResult {
    private String output;
    private String error;
    private ExecutionStatus status; // Changed from String to Enum
    private double time;
    private double memory;
    private String message;

    // This was missing!
    public enum ExecutionStatus {
        QUEUED,
        PROCESSING,
        ACCEPTED,
        WRONG_ANSWER,
        COMPILE_ERROR,
        RUNTIME_ERROR,
        TIME_LIMIT_EXCEEDED,
        MEMORY_LIMIT_EXCEEDED,
        INTERNAL_ERROR
    }
}

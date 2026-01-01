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
    private String executionId; // Judge0 token/execution ID
    private String output;
    private String error;
    private ExecutionStatus status; // Changed from String to Enum
    private double time;
    private double memory;
    private String message;
    private boolean passed;

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
    
    public static ExecutionStatus fromJudge0Status(Integer statusId) {
        if (statusId == null) return INTERNAL_ERROR;
        return switch (statusId) {
            case 1, 2 -> QUEUED;
            case 3 -> ACCEPTED;
            case 4 -> WRONG_ANSWER;
            case 6 -> COMPILE_ERROR;
            case 5, 7, 8, 9, 10, 11, 12 -> RUNTIME_ERROR;
            case 13 -> INTERNAL_ERROR;
            default -> INTERNAL_ERROR;
        };
    }
}

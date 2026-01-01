package com.examportal.execution.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Execution Result
 * 
 * Internal representation of code execution results
 * Used for storing in database and returning to frontend
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionResult {

    /**
     * Unique execution ID
     */
    private String executionId;

    /**
     * Judge0 submission token
     */
    private String submissionToken;

    /**
     * Execution status
     */
    private ExecutionStatus status;

    /**
     * Standard output
     */
    private String output;

    /**
     * Error output
     */
    private String error;

    /**
     * Compilation errors
     */
    private String compileOutput;

    /**
     * Exit code
     */
    private Integer exitCode;

    /**
     * CPU time used (milliseconds)
     */
    private Long cpuTimeMs;

    /**
     * Memory used (KB)
     */
    private Integer memoryKb;

    /**
     * Whether execution passed all test cases
     */
    private Boolean passed;

    /**
     * Test cases passed / total
     */
    private String testCasesSummary;

    /**
     * Execution timestamp
     */
    private LocalDateTime executedAt;

    public enum ExecutionStatus {
        QUEUED,
        PROCESSING,
        ACCEPTED,
        WRONG_ANSWER,
        TIME_LIMIT_EXCEEDED,
        MEMORY_LIMIT_EXCEEDED,
        RUNTIME_ERROR,
        COMPILATION_ERROR,
        INTERNAL_ERROR
    }

    /**
     * Convert Judge0 status to internal status
     */
    public static ExecutionStatus fromJudge0Status(int statusId) {
        return switch (statusId) {
            case 1 -> ExecutionStatus.QUEUED;
            case 2 -> ExecutionStatus.PROCESSING;
            case 3 -> ExecutionStatus.ACCEPTED;
            case 4 -> ExecutionStatus.WRONG_ANSWER;
            case 5 -> ExecutionStatus.TIME_LIMIT_EXCEEDED;
            case 6 -> ExecutionStatus.COMPILATION_ERROR;
            case 7, 8, 9, 10, 11 -> ExecutionStatus.RUNTIME_ERROR;
            default -> ExecutionStatus.INTERNAL_ERROR;
        };
    }
}

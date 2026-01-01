package com.examportal.execution.model;

import java.time.LocalDateTime;

/**
 * Execution Result
 * 
 * Internal representation of code execution results
 * Used for storing in database and returning to frontend
 */
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
     * Execution time (seconds)
     */
    private Double time;

    /**
     * Memory usage (KB)
     */
    private Integer memory;

    /**
     * When execution was completed
     */
    private LocalDateTime completedAt;

    public ExecutionResult() {}

    public ExecutionResult(String executionId, String submissionToken, ExecutionStatus status, String output, String error, String compileOutput, Double time, Integer memory, LocalDateTime completedAt) {
        this.executionId = executionId;
        this.submissionToken = submissionToken;
        this.status = status;
        this.output = output;
        this.error = error;
        this.compileOutput = compileOutput;
        this.time = time;
        this.memory = memory;
        this.completedAt = completedAt;
    }

    public String getExecutionId() { return executionId; }
    public void setExecutionId(String executionId) { this.executionId = executionId; }
    public String getSubmissionToken() { return submissionToken; }
    public void setSubmissionToken(String submissionToken) { this.submissionToken = submissionToken; }
    public ExecutionStatus getStatus() { return status; }
    public void setStatus(ExecutionStatus status) { this.status = status; }
    public String getOutput() { return output; }
    public void setOutput(String output) { this.output = output; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public String getCompileOutput() { return compileOutput; }
    public void setCompileOutput(String compileOutput) { this.compileOutput = compileOutput; }
    public Double getTime() { return time; }
    public void setTime(Double time) { this.time = time; }
    public Integer getMemory() { return memory; }
    public void setMemory(Integer memory) { this.memory = memory; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

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

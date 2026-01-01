package com.examportal.execution.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Judge0 Submission Response
 * 
 * Response from Judge0 API after code execution
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Judge0SubmissionResponse {

    /**
     * Submission token (unique ID)
     */
    private String token;

    /**
     * Status of execution
     */
    private Status status;

    /**
     * Standard output from program
     */
    private String stdout;

    /**
     * Standard error output
     */
    private String stderr;

    /**
     * Compilation output (errors)
     */
    private String compile_output;

    /**
     * Exit code
     */
    private Integer exit_code;

    /**
     * CPU time used (seconds)
     */
    private Double time;

    /**
     * Memory used (KB)
     */
    private Integer memory;

    /**
     * Execution status message
     */
    private String message;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Status {
        private Integer id;
        private String description;
    }

    /**
     * Check if execution was successful
     */
    public boolean isAccepted() {
        return status != null && status.getId() == 3; // Status 3 = Accepted
    }

    /**
     * Check if compilation failed
     */
    public boolean isCompilationError() {
        return status != null && status.getId() == 6; // Status 6 = Compilation Error
    }

    /**
     * Check if runtime error occurred
     */
    public boolean isRuntimeError() {
        return status != null && (status.getId() == 4 || status.getId() == 5); 
        // 4 = Wrong Answer, 5 = Time Limit Exceeded
    }

    /**
     * Check if execution is still pending
     */
    public boolean isPending() {
        return status != null && (status.getId() == 1 || status.getId() == 2);
        // 1 = In Queue, 2 = Processing
    }
}

package com.examportal.execution.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Judge0 Submission Request
 * 
 * Maps to Judge0 API submission format
 * See: https://ce.judge0.com/#submissions-submission-post
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Judge0SubmissionRequest {

    /**
     * Source code to execute
     */
    private String source_code;

    /**
     * Language ID (Judge0 specific)
     * Java: 62, Python: 71, C++: 54
     */
    private Integer language_id;

    /**
     * Standard input for the program
     */
    private String stdin;

    /**
     * Expected output (for comparison)
     */
    private String expected_output;

    /**
     * CPU time limit in seconds (default: 5)
     */
    private Double cpu_time_limit;

    /**
     * Wall time limit in seconds (default: 10)
     */
    private Double wall_time_limit;

    /**
     * Memory limit in KB (default: 256000 = 256MB)
     */
    private Integer memory_limit;

    /**
     * Callback URL for async results
     */
    private String callback_url;

    /**
     * Whether to wait for result (sync mode)
     */
    private Boolean wait;

    /**
     * Base64 encode source code
     */
    private Boolean base64_encoded;
}

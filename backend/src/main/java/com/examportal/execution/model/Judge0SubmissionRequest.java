package com.examportal.execution.model;

/**
 * Judge0 Submission Request
 * 
 * Maps to Judge0 API submission format
 * See: https://ce.judge0.com/#submissions-submission-post
 */
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

    public Judge0SubmissionRequest() {}

    public Judge0SubmissionRequest(String source_code, Integer language_id, String stdin, String expected_output, Double cpu_time_limit, Double wall_time_limit) {
        this.source_code = source_code;
        this.language_id = language_id;
        this.stdin = stdin;
        this.expected_output = expected_output;
        this.cpu_time_limit = cpu_time_limit;
        this.wall_time_limit = wall_time_limit;
    }

    public String getSource_code() { return source_code; }
    public void setSource_code(String source_code) { this.source_code = source_code; }
    public Integer getLanguage_id() { return language_id; }
    public void setLanguage_id(Integer language_id) { this.language_id = language_id; }
    public String getStdin() { return stdin; }
    public void setStdin(String stdin) { this.stdin = stdin; }
    public String getExpected_output() { return expected_output; }
    public void setExpected_output(String expected_output) { this.expected_output = expected_output; }
    public Double getCpu_time_limit() { return cpu_time_limit; }
    public void setCpu_time_limit(Double cpu_time_limit) { this.cpu_time_limit = cpu_time_limit; }
    public Double getWall_time_limit() { return wall_time_limit; }
    public void setWall_time_limit(Double wall_time_limit) { this.wall_time_limit = wall_time_limit; }

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

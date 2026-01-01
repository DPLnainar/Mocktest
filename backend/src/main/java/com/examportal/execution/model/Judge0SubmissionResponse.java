package com.examportal.execution.model;

/**
 * Judge0 Submission Response
 * 
 * Response from Judge0 API after code execution
 */
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

    public Judge0SubmissionResponse() {}

    public Judge0SubmissionResponse(String token, Status status, String stdout, String stderr, String compile_output, Integer exit_code, Double time, Integer memory) {
        this.token = token;
        this.status = status;
        this.stdout = stdout;
        this.stderr = stderr;
        this.compile_output = compile_output;
        this.exit_code = exit_code;
        this.time = time;
        this.memory = memory;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getStdout() { return stdout; }
    public void setStdout(String stdout) { this.stdout = stdout; }
    public String getStderr() { return stderr; }
    public void setStderr(String stderr) { this.stderr = stderr; }
    public String getCompile_output() { return compile_output; }
    public void setCompile_output(String compile_output) { this.compile_output = compile_output; }
    public Integer getExit_code() { return exit_code; }
    public void setExit_code(Integer exit_code) { this.exit_code = exit_code; }
    public Double getTime() { return time; }
    public void setTime(Double time) { this.time = time; }
    public Integer getMemory() { return memory; }
    public void setMemory(Integer memory) { this.memory = memory; }

    /**
     * Execution status message
     */
    private String message;

    public static class Status {
        private Integer id;
        private String description;

        public Status() {}
        public Status(Integer id, String description) {
            this.id = id;
            this.description = description;
        }

        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
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

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getStdout() { return stdout; }
    public void setStdout(String stdout) { this.stdout = stdout; }
    public String getStderr() { return stderr; }
    public void setStderr(String stderr) { this.stderr = stderr; }
    public String getCompile_output() { return compile_output; }
    public void setCompile_output(String compile_output) { this.compile_output = compile_output; }
    public Integer getExit_code() { return exit_code; }
    public void setExit_code(Integer exit_code) { this.exit_code = exit_code; }
    public Double getTime() { return time; }
    public void setTime(Double time) { this.time = time; }
    public Integer getMemory() { return memory; }
    public void setMemory(Integer memory) { this.memory = memory; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public static class Status {
        private Integer id;
        private String description;

        public Status() {}
        public Status(Integer id, String description) {
            this.id = id;
            this.description = description;
        }

        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}

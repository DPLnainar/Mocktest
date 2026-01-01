package com.examportal.execution.controller;

import com.examportal.execution.model.ExecutionResult;
import com.examportal.execution.service.Judge0Service;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.examportal.security.CustomUserDetails;

/**
 * Code Execution Controller
 * 
 * Provides endpoints for submitting and retrieving code execution results
 */
@RestController
@RequestMapping("/api/execution")
public class ExecutionController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ExecutionController.class);
    private final Judge0Service judge0Service;

    public ExecutionController(Judge0Service judge0Service) {
        this.judge0Service = judge0Service;
    }

    /**
     * Execute code
     * 
     * POST /api/execution/run
     */
    @PostMapping("/run")
    @PreAuthorize("hasAnyRole('STUDENT', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<ExecutionResult> executeCode(
            @Valid @RequestBody ExecutionRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        
        log.info("Executing code for user {} in language {}", user.getId(), request.getLanguageId());

        ExecutionResult result = judge0Service.executeCode(
            request.getCode(),
            request.getLanguageId(),
            request.getStdin(),
            user.getId()
        );

        return ResponseEntity.ok(result);
    }

    /**
     * Get execution result
     * 
     * GET /api/execution/{executionId}
     */
    @GetMapping("/{executionId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<ExecutionResult> getExecutionResult(@PathVariable String executionId) {
        log.debug("Fetching execution result for {}", executionId);

        ExecutionResult result = judge0Service.getExecutionResult(executionId);
        return ResponseEntity.ok(result);
    }

    /**
     * Batch execution for multiple test cases
     * 
     * POST /api/execution/batch
     */
    @PostMapping("/batch")
    @PreAuthorize("hasAnyRole('STUDENT', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<ExecutionResult[]> executeBatch(
            @Valid @RequestBody BatchExecutionRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        
        log.info("Batch execution for user {} with {} test cases", user.getId(), request.getTestInputs().length);

        ExecutionResult[] results = judge0Service.executeBatch(
            request.getCode(),
            request.getLanguageId(),
            request.getTestInputs(),
            user.getId()
        );

        return ResponseEntity.ok(results);
    }

    public static class ExecutionRequest {
        private String code;
        private Integer languageId; // Judge0 language ID
        private String stdin;

        public ExecutionRequest() {}
        public ExecutionRequest(String code, Integer languageId, String stdin) {
            this.code = code;
            this.languageId = languageId;
            this.stdin = stdin;
        }

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public Integer getLanguageId() { return languageId; }
        public void setLanguageId(Integer languageId) { this.languageId = languageId; }
        public String getStdin() { return stdin; }
        public void setStdin(String stdin) { this.stdin = stdin; }
    }

    public static class BatchExecutionRequest {
        private String code;
        private Integer languageId;
        private String[] testInputs;

        public BatchExecutionRequest() {}
        public BatchExecutionRequest(String code, Integer languageId, String[] testInputs) {
            this.code = code;
            this.languageId = languageId;
            this.testInputs = testInputs;
        }

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public Integer getLanguageId() { return languageId; }
        public void setLanguageId(Integer languageId) { this.languageId = languageId; }
        public String[] getTestInputs() { return testInputs; }
        public void setTestInputs(String[] testInputs) { this.testInputs = testInputs; }
    }
}

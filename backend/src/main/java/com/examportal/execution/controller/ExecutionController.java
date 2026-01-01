package com.examportal.execution.controller;

import com.examportal.execution.model.ExecutionResult;
import com.examportal.execution.service.Judge0Service;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequiredArgsConstructor
@Slf4j
public class ExecutionController {

    private final Judge0Service judge0Service;

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

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecutionRequest {
        private String code;
        private Integer languageId; // Judge0 language ID
        private String stdin;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchExecutionRequest {
        private String code;
        private Integer languageId;
        private String[] testInputs;
    }
}

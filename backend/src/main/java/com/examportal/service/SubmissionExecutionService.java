package com.examportal.service;

import com.examportal.dto.CodeVerificationResult;
import com.examportal.execution.model.ExecutionResult;
import com.examportal.execution.service.Judge0Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Submission Execution Service
 * 
 * Orchestrates the secure execution of student code.
 * Implements a strict two-phase pipeline:
 * Phase 1: Lightweight ANTLR Verification (Static Analysis)
 * Phase 2: Containered Judge0 Execution (Dynamic Sandbox) with strict limits.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionExecutionService {

    private final CodeVerificationService codeVerificationService;
    private final Judge0Service judge0Service;

    // Strict limits for Phase 2 (The "Crash-Proof" settings)
    private static final double CPU_TIME_LIMIT = 2.0; // 2 seconds max
    private static final double WALL_TIME_LIMIT = 5.0; // 5 seconds hard stop
    private static final double MEMORY_LIMIT = 128000.0; // 128 MB max

    /**
     * Execute a student's submission through the secure pipeline.
     *
     * @param code        The student's source code
     * @param languageId  The ID of the language (e.g., 62 for Java)
     * @param constraints Map of logic constraints (e.g., "forbidLoops": true)
     * @param studentId   ID of the student (for rate limiting)
     * @return ExecutionResult containing status and output/error
     */
    /**
     * Execute a student's submission through the secure pipeline.
     */
    public ExecutionResult executeSubmission(String executionId, String code, Integer languageId,
            Map<String, Boolean> constraints,
            Long studentId, Long attemptId, Long questionId) {
        // String executionId = UUID.randomUUID().toString(); // Use provided ID
        log.info("Starting secure execution pipeline for Submission {}", executionId);

        try {
            // --- PHASE 1: The "Lightweight" Guard (ANTLR) ---
            log.debug("Phase 1: Running ANTLR Static Analysis...");

            // Pass null for allowed languages here as we assume that check is done before
            // calling this service,
            // or we could inspect the languageId if needed.
            // Converting ID to name helper
            String languageName = getLanguageName(languageId);

            CodeVerificationResult verificationResult = codeVerificationService.verifyCode(
                    code,
                    languageName,
                    constraints,
                    null // Allowed list not checked here, assumed valid or checked upstream
            );

            if (!verificationResult.isValid()) {
                String violations = String.join("\n", verificationResult.getErrors());
                log.warn("Phase 1 Blocked: Security/Logic violations found: {}", violations);

                // Return REJECTED result immediately
                return ExecutionResult.builder()
                        .executionId(executionId)
                        .status(ExecutionResult.ExecutionStatus.INTERNAL_ERROR) // Frontend treats as error/block
                        .error("LOGIC BLOCK: " + violations) // Specific prefix for frontend alert
                        .executedAt(LocalDateTime.now())
                        .build();
            }

            log.info("Phase 1 Passed: Code is structurally safe.");

            // --- PHASE 2: The "Containered" Executor (Judge0) ---
            log.debug("Phase 2: Submitting to Judge0 Sandbox with strict limits...");

            try {
                // Call Judge0 with CRASH-PROOF limits
                return judge0Service.executeCode(
                        executionId,
                        code,
                        languageId,
                        "", // stdin (empty for now, could be passed if needed)
                        studentId,
                        CPU_TIME_LIMIT,
                        WALL_TIME_LIMIT,
                        MEMORY_LIMIT,
                        attemptId,
                        questionId);
            } catch (Exception e) {
                log.error("Phase 2 Error: Judge0 Execution failed", e);
                // Graceful error handling - avoid crashing controller
                return ExecutionResult.builder()
                        .executionId(executionId)
                        .status(ExecutionResult.ExecutionStatus.INTERNAL_ERROR)
                        .error("Execution Error: The sandbox service is temporarily unavailable.")
                        .executedAt(LocalDateTime.now())
                        .build();
            }

        } catch (Exception e) {
            log.error("Critical Pipeline Error", e);
            return ExecutionResult.builder()
                    .executionId(executionId)
                    .status(ExecutionResult.ExecutionStatus.INTERNAL_ERROR)
                    .error("System Error: An unexpected error occurred during processing.")
                    .executedAt(LocalDateTime.now())
                    .build();
        }
    }

    private String getLanguageName(Integer id) {
        if (id == null)
            return "unknown";
        return switch (id) {
            case 62 -> "java";
            case 71 -> "python";
            case 50, 54 -> "cpp";
            case 63 -> "javascript";
            default -> "unknown";
        };
    }
}

package com.examportal.execution.service;

import com.examportal.execution.client.Judge0Client;
import com.examportal.execution.model.ExecutionResult;
import com.examportal.execution.model.Judge0SubmissionRequest;
import com.examportal.execution.model.Judge0SubmissionResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Judge0 Service
 * 
 * Manages code execution via Judge0 API
 * Features:
 * - Circuit breaker for fault tolerance
 * - Redis-based execution queue
 * - Rate limiting per student
 * - Async execution with webhooks
 */
@Service
public class Judge0Service {

    private static final Logger log = LoggerFactory.getLogger(Judge0Service.class);

    private final Judge0Client judge0Client;
    private final StringRedisTemplate redisTemplate;
    private final ExecutionQueueService executionQueueService;

    public Judge0Service(Judge0Client judge0Client, StringRedisTemplate redisTemplate,
            ExecutionQueueService executionQueueService) {
        this.judge0Client = judge0Client;
        this.redisTemplate = redisTemplate;
        this.executionQueueService = executionQueueService;
    }

    @Value("${judge0.api-key}")
    private String apiKey;

    @Value("${judge0.api-host:}")
    private String apiHost;

    @Value("${judge0.callback-url}")
    private String callbackUrl;

    @Value("${judge0.max-concurrent-per-student:10}")
    private int maxConcurrentPerStudent;

    /**
     * Submit code for execution
     * Circuit breaker protects against Judge0 failures
     * 
     * @param code       Source code
     * @param languageId Judge0 language ID
     * @param stdin      Standard input
     * @param studentId  Student ID (for rate limiting)
     * @return Execution result
     */
    @CircuitBreaker(name = "judge0Service", fallbackMethod = "executionFallback")
    public ExecutionResult executeCode(String code, Integer languageId, String stdin, Long studentId) {
        return executeCode(UUID.randomUUID().toString(), code, languageId, stdin, studentId, 5.0, 10.0, 256000.0, null,
                null);
    }

    /**
     * Submit code for execution with custom limits and context
     */
    @CircuitBreaker(name = "judge0Service", fallbackMethod = "executionFallbackWithLimits")
    public ExecutionResult executeCode(String executionId, String code, Integer languageId, String stdin,
            Long studentId,
            Double cpuTimeLimit, Double wallTimeLimit, Double memoryLimit, Long attemptId, Long questionId) {
        // Check rate limit
        if (!checkRateLimit(studentId)) {
            return ExecutionResult.builder()
                    .executionId(executionId)
                    .status(ExecutionResult.ExecutionStatus.INTERNAL_ERROR)
                    .error("Rate limit exceeded. Maximum " + maxConcurrentPerStudent +
                            " concurrent executions allowed.")
                    .executedAt(LocalDateTime.now())
                    .build();
        }

        // Use provided ID or generate new
        if (executionId == null)
            executionId = UUID.randomUUID().toString();

        try {
            // Increment student's concurrent execution count
            incrementExecutionCount(studentId);

            // Build submission request
            Judge0SubmissionRequest request = Judge0SubmissionRequest.builder()
                    .source_code(code)
                    .language_id(languageId)
                    .stdin(stdin)
                    .cpu_time_limit(cpuTimeLimit)
                    .wall_time_limit(wallTimeLimit)
                    .memory_limit(memoryLimit)
                    .callback_url(callbackUrl + "/" + executionId)
                    .wait(false) // Async mode
                    .base64_encoded(false)
                    .build();

            log.info("Submitting code execution for student {} with execution ID {}", studentId, executionId);

            // Submit to Judge0
            Judge0SubmissionResponse response = judge0Client.createSubmission(apiKey, apiHost, request);

            // Queue execution for polling
            // Queue execution for polling
            executionQueueService.queueExecution(executionId, response.getToken(), studentId, attemptId, questionId);

            // Return initial result
            return ExecutionResult.builder()
                    .executionId(executionId)
                    .submissionToken(response.getToken())
                    .status(ExecutionResult.ExecutionStatus.QUEUED)
                    .executedAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Error submitting code execution", e);
            decrementExecutionCount(studentId);
            throw e;
        }
    }

    /**
     * Get execution result by ID
     * Checks Redis cache first, then polls Judge0 if needed
     */
    public ExecutionResult getExecutionResult(String executionId) {
        // Check if result is cached in Redis
        String cachedResult = redisTemplate.opsForValue().get("execution:result:" + executionId);
        if (cachedResult != null) {
            // Parse and return cached result
            return parseExecutionResult(cachedResult);
        }

        // Get submission token from queue
        String submissionToken = executionQueueService.getSubmissionToken(executionId);
        if (submissionToken == null) {
            return ExecutionResult.builder()
                    .executionId(executionId)
                    .status(ExecutionResult.ExecutionStatus.INTERNAL_ERROR)
                    .error("Execution not found")
                    .build();
        }

        // Poll Judge0 for result
        return pollJudge0Result(executionId, submissionToken);
    }

    /**
     * Poll Judge0 for execution result
     */
    @CircuitBreaker(name = "judge0Service", fallbackMethod = "pollFallback")
    private ExecutionResult pollJudge0Result(String executionId, String submissionToken) {
        try {
            Judge0SubmissionResponse response = judge0Client.getSubmission(
                    apiKey,
                    apiHost,
                    submissionToken,
                    "stdout,stderr,status,compile_output,time,memory,exit_code");

            ExecutionResult result = convertToExecutionResult(executionId, response);

            // Cache result if execution is complete
            if (!result.getStatus().equals(ExecutionResult.ExecutionStatus.QUEUED) &&
                    !result.getStatus().equals(ExecutionResult.ExecutionStatus.PROCESSING)) {

                cacheExecutionResult(executionId, result);

                // Decrement student's execution count
                Long studentId = executionQueueService.getStudentId(executionId);
                if (studentId != null) {
                    decrementExecutionCount(studentId);
                }
            }

            return result;

        } catch (Exception e) {
            log.error("Error polling Judge0 result", e);
            throw e;
        }
    }

    /**
     * Batch execution for multiple test cases
     */
    @CircuitBreaker(name = "judge0Service", fallbackMethod = "batchExecutionFallback")
    public ExecutionResult[] executeBatch(String code, Integer languageId, String[] testInputs, Long studentId) {
        // Build batch requests
        Judge0SubmissionRequest[] requests = new Judge0SubmissionRequest[testInputs.length];
        for (int i = 0; i < testInputs.length; i++) {
            requests[i] = Judge0SubmissionRequest.builder()
                    .source_code(code)
                    .language_id(languageId)
                    .stdin(testInputs[i])
                    .cpu_time_limit(5.0)
                    .wall_time_limit(10.0)
                    .memory_limit(256000.0)
                    .wait(true) // Sync mode for batch
                    .build();
        }

        // Submit batch
        Judge0SubmissionResponse[] responses = judge0Client.createBatchSubmissions(apiKey, apiHost, requests);

        // Convert responses
        ExecutionResult[] results = new ExecutionResult[responses.length];
        for (int i = 0; i < responses.length; i++) {
            results[i] = convertToExecutionResult(UUID.randomUUID().toString(), responses[i]);
        }

        return results;
    }

    /**
     * Check rate limit for student
     */
    private boolean checkRateLimit(Long studentId) {
        String key = "execution:count:student:" + studentId;
        String count = redisTemplate.opsForValue().get(key);

        if (count == null) {
            return true;
        }

        return Integer.parseInt(count) < maxConcurrentPerStudent;
    }

    /**
     * Increment student's concurrent execution count
     */
    private void incrementExecutionCount(Long studentId) {
        String key = "execution:count:student:" + studentId;
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, 1, TimeUnit.HOURS);
    }

    /**
     * Decrement student's concurrent execution count
     */
    private void decrementExecutionCount(Long studentId) {
        String key = "execution:count:student:" + studentId;
        redisTemplate.opsForValue().decrement(key);
    }

    /**
     * Convert Judge0 response to ExecutionResult
     */
    private ExecutionResult convertToExecutionResult(String executionId, Judge0SubmissionResponse response) {
        return ExecutionResult.builder()
                .executionId(executionId)
                .submissionToken(response.getToken())
                .status(ExecutionResult.fromJudge0Status(response.getStatus().getId()))
                .output(response.getStdout())
                .error(response.getStderr())
                .compileOutput(response.getCompile_output())
                .exitCode(response.getExit_code())
                .cpuTimeMs(response.getTime() != null ? (long) (response.getTime() * 1000) : null)
                .memoryKb(response.getMemory())
                .passed(response.isAccepted())
                .executedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Cache execution result in Redis
     */
    private void cacheExecutionResult(String executionId, ExecutionResult result) {
        String key = "execution:result:" + executionId;
        // Store as JSON or serialized object
        redisTemplate.opsForValue().set(java.util.Objects.requireNonNull(key),
                java.util.Objects.requireNonNull(result.toString()), 1, TimeUnit.HOURS);
    }

    private ExecutionResult parseExecutionResult(String cached) {
        // Implement JSON parsing
        return null; // Placeholder
    }

    /**
     * Fallback method when circuit breaker opens
     */
    private ExecutionResult executionFallback(String code, Integer languageId, String stdin,
            Long studentId, Exception e) {
        log.error("Circuit breaker activated - Judge0 service unavailable", e);

        return ExecutionResult.builder()
                .executionId(UUID.randomUUID().toString())
                .status(ExecutionResult.ExecutionStatus.INTERNAL_ERROR)
                .error("Code execution service is temporarily unavailable. Please try again later.")
                .executedAt(LocalDateTime.now())
                .build();
    }

    @SuppressWarnings("unused")
    private ExecutionResult pollFallback(String executionId, String submissionToken, Exception e) {
        log.error("Circuit breaker activated - Cannot poll Judge0 result", e);

        return ExecutionResult.builder()
                .executionId(executionId)
                .status(ExecutionResult.ExecutionStatus.INTERNAL_ERROR)
                .error("Unable to retrieve execution result. Service temporarily unavailable.")
                .build();
    }

    @SuppressWarnings("unused")
    private ExecutionResult[] batchExecutionFallback(String code, Integer languageId,
            String[] testInputs, Long studentId, Exception e) {
        log.error("Circuit breaker activated - Batch execution failed", e);

        ExecutionResult[] results = new ExecutionResult[testInputs.length];
        for (int i = 0; i < results.length; i++) {
            results[i] = executionFallback(code, languageId, testInputs[i], studentId, e);
        }
        return results;
    }
}

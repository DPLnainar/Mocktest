package com.examportal.execution.controller;

import com.examportal.execution.model.Judge0SubmissionResponse;
import com.examportal.execution.service.ExecutionQueueService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

/**
 * Judge0 Webhook Controller
 * 
 * Receives callbacks from Judge0 when code execution completes
 * Endpoint is public (no authentication) as Judge0 doesn't support auth headers
 */
@RestController
@RequestMapping("/api/judge0/callback")
public class Judge0WebhookController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Judge0WebhookController.class);
    private final RedisTemplate<String, Object> redisTemplate;
    private final ExecutionQueueService executionQueueService;
    private final com.examportal.repository.StudentAttemptRepository attemptRepository;

    public Judge0WebhookController(RedisTemplate<String, Object> redisTemplate,
            ExecutionQueueService executionQueueService,
            com.examportal.repository.StudentAttemptRepository attemptRepository) {
        this.redisTemplate = redisTemplate;
        this.executionQueueService = executionQueueService;
        this.attemptRepository = attemptRepository;
    }

    /**
     * Receive Judge0 callback
     * 
     * POST /api/judge0/callback/{executionId}
     */
    @PostMapping("/{executionId}")
    public ResponseEntity<String> handleCallback(
            @PathVariable String executionId,
            @RequestBody Judge0SubmissionResponse response) {

        log.info("Received Judge0 callback for execution {}: Status {}",
                executionId, response.getStatus().getDescription());

        try {
            // Store result in Redis
            String cacheKey = "execution:result:" + executionId;
            redisTemplate.opsForValue().set(cacheKey, response, 1, TimeUnit.HOURS);

            // Remove from pending queue
            executionQueueService.removeFromQueue(executionId);

            // Decrement student's execution count
            Long studentId = executionQueueService.getStudentId(executionId);
            if (studentId != null) {
                String countKey = "execution:count:student:" + studentId;
                redisTemplate.opsForValue().decrement(countKey);
            }

            // PERSISTENCE FIX: Update DB Result
            String context = executionQueueService.getExecutionContext(executionId);
            if (context != null && context.contains(":")) {
                try {
                    String[] parts = context.split(":");
                    Long attemptId = Long.parseLong(parts[0]);
                    String questionIdStr = parts[1]; // Store as string in map

                    com.examportal.entity.StudentAttempt attempt = attemptRepository.findById(attemptId).orElse(null);
                    if (attempt != null) {
                        // Convert Judge0 response to ExecutionResult (simplified for storage)
                        // Note: Using a helper or manual builder here to avoid circular dependencies
                        // Ideally checking Judge0Service for conversion logic would be better, but we
                        // want to avoid dep cycles.
                        // Let's rely on the fact that StudentAttempt stores Map<String, Object> and we
                        // can store the response directly or valid fields.
                        // But wait, frontend expects ExecutionResult format.

                        // Let's duplicate the minimal conversion logic here to be safe and fast.
                        com.examportal.execution.model.ExecutionResult result = com.examportal.execution.model.ExecutionResult
                                .builder()
                                .executionId(executionId)
                                .submissionToken(response.getToken())
                                .status(com.examportal.execution.model.ExecutionResult
                                        .fromJudge0Status(response.getStatus().getId()))
                                .output(response.getStdout())
                                .error(response.getStderr())
                                .compileOutput(response.getCompile_output())
                                .passed(response.isAccepted())
                                // .executedAt(java.time.LocalDateTime.now()) // Optional
                                .build();

                        attempt.getExecutionResults().put(questionIdStr, result);
                        attemptRepository.save(attempt);
                        log.info("Persisted execution result to DB for Attempt {} Question {}", attemptId,
                                questionIdStr);
                    }
                } catch (Exception dbEx) {
                    log.error("Failed to persist execution result to DB", dbEx);
                }
            }

            log.info("Finished processing callback for {}. Future: Notify student via WebSocket.", executionId);

            return ResponseEntity.ok("Callback received");

        } catch (Exception e) {
            log.error("Error processing Judge0 callback", e);
            return ResponseEntity.internalServerError().body("Error processing callback");
        }
    }

    /**
     * Health check endpoint for Judge0
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Webhook endpoint is healthy");
    }
}

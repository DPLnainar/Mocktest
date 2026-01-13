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

    public Judge0WebhookController(RedisTemplate<String, Object> redisTemplate,
            ExecutionQueueService executionQueueService) {
        this.redisTemplate = redisTemplate;
        this.executionQueueService = executionQueueService;
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

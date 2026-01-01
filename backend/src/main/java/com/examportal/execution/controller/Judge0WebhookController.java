package com.examportal.execution.controller;

import com.examportal.execution.model.Judge0SubmissionResponse;
import com.examportal.execution.service.ExecutionQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequiredArgsConstructor
@Slf4j
public class Judge0WebhookController {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ExecutionQueueService executionQueueService;

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

            // TODO: Notify via WebSocket if student is monitoring results live

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

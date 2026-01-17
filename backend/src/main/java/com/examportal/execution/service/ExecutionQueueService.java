package com.examportal.execution.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Execution Queue Service
 * 
 * Manages pending code executions using Redis
 * Tracks submission tokens and student IDs for result polling
 */
@Service
public class ExecutionQueueService {

    private static final Logger log = LoggerFactory.getLogger(ExecutionQueueService.class);

    private final StringRedisTemplate redisTemplate;

    public ExecutionQueueService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private static final String QUEUE_PREFIX = "execution:queue:";
    private static final String TOKEN_PREFIX = "execution:token:";
    private static final String STUDENT_PREFIX = "execution:student:";
    private static final long TTL_HOURS = 2;

    /**
     * Queue an execution for later polling
     * 
     * @param executionId     Unique execution ID
     * @param submissionToken Judge0 submission token
     * @param studentId       Student ID
     * @param attemptId       Student Attempt ID (optional)
     * @param questionId      Question ID (optional)
     */
    public void queueExecution(String executionId, String submissionToken, Long studentId, Long attemptId,
            Long questionId) {
        try {
            // Store submission token
            String tokenKey = TOKEN_PREFIX + executionId;
            redisTemplate.opsForValue().set(java.util.Objects.requireNonNull(tokenKey),
                    java.util.Objects.requireNonNull(submissionToken), TTL_HOURS, TimeUnit.HOURS);

            // Store student ID
            String studentKey = STUDENT_PREFIX + executionId;
            redisTemplate.opsForValue().set(java.util.Objects.requireNonNull(studentKey),
                    java.util.Objects.requireNonNull(studentId.toString()), TTL_HOURS, TimeUnit.HOURS);

            // Store Context (Attempt + Question)
            if (attemptId != null && questionId != null) {
                String contextKey = "execution:context:" + executionId;
                String contextValue = attemptId + ":" + questionId;
                redisTemplate.opsForValue().set(contextKey, contextValue, TTL_HOURS, TimeUnit.HOURS);
            }

            // Add to queue (sorted set with timestamp)
            String queueKey = QUEUE_PREFIX + "pending";
            redisTemplate.opsForZSet().add(java.util.Objects.requireNonNull(queueKey),
                    java.util.Objects.requireNonNull(executionId), System.currentTimeMillis());

            log.debug("Queued execution {} for student {} (Context: {}/{})", executionId, studentId, attemptId,
                    questionId);

        } catch (Exception e) {
            log.error("Error queuing execution", e);
        }
    }

    /**
     * Get submission token for an execution
     */
    public String getSubmissionToken(String executionId) {
        String tokenKey = TOKEN_PREFIX + executionId;
        return redisTemplate.opsForValue().get(tokenKey);
    }

    public Long getStudentId(String executionId) {
        String studentKey = STUDENT_PREFIX + executionId;
        String studentId = redisTemplate.opsForValue().get(studentKey);
        return studentId != null ? Long.parseLong(studentId) : null;
    }

    public String getExecutionContext(String executionId) {
        return redisTemplate.opsForValue().get("execution:context:" + executionId);
    }

    /**
     * Remove execution from queue (after completion)
     */
    public void removeFromQueue(String executionId) {
        try {
            String queueKey = QUEUE_PREFIX + "pending";
            redisTemplate.opsForZSet().remove(queueKey, executionId);

            // Clean up keys
            redisTemplate.delete(TOKEN_PREFIX + executionId);
            redisTemplate.delete(STUDENT_PREFIX + executionId);

            log.debug("Removed execution {} from queue", executionId);

        } catch (Exception e) {
            log.error("Error removing execution from queue", e);
        }
    }

    /**
     * Get pending execution count for monitoring
     */
    public long getPendingCount() {
        String queueKey = QUEUE_PREFIX + "pending";
        Long count = redisTemplate.opsForZSet().size(queueKey);
        return count != null ? count : 0;
    }
}

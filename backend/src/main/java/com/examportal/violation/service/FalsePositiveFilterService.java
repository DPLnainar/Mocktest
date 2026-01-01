package com.examportal.violation.service;

import com.examportal.violation.dto.EnhancedViolationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Phase 8: False Positive Filter Service
 * 
 * Validates violations before processing
 * Filters out low-confidence and non-consecutive detections
 */
@Service
public class FalsePositiveFilterService {

    private static final Logger log = LoggerFactory.getLogger(FalsePositiveFilterService.class);

    private final StringRedisTemplate redisTemplate;

    public FalsePositiveFilterService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private static final String CONSECUTIVE_FRAME_PREFIX = "exam:consecutive:";
    private static final int MIN_CONSECUTIVE_FRAMES = 3;
    private static final double MIN_CONFIDENCE = 0.85;

    /**
     * Validate violation meets Phase 8 criteria
     * 
     * @return true if violation should be processed
     */
    public boolean shouldProcessViolation(EnhancedViolationRequest request) {
        // Phase 8: Check confidence threshold
        if (request.getConfidence() != null && request.getConfidence() < MIN_CONFIDENCE) {
            log.debug("Rejecting violation - low confidence: {}", request.getConfidence());
            return false;
        }

        // Phase 8: Check consecutive frames
        if (request.getConsecutiveFrames() != null && 
            request.getConsecutiveFrames() < MIN_CONSECUTIVE_FRAMES) {
            log.debug("Rejecting violation - insufficient consecutive frames: {}", 
                    request.getConsecutiveFrames());
            return false;
        }

        // Phase 8: Must be marked as confirmed
        if (request.getConfirmed() != null && !request.getConfirmed()) {
            log.debug("Rejecting violation - not confirmed");
            return false;
        }

        return true;
    }

    /**
     * Track consecutive detections in Redis
     * Used for server-side validation if needed
     */
    public int incrementConsecutiveDetection(Long sessionId, String violationType) {
        String key = CONSECUTIVE_FRAME_PREFIX + sessionId + ":" + violationType;
        Long count = redisTemplate.opsForValue().increment(key);
        
        // Expire after 5 seconds (if no new detections, reset)
        redisTemplate.expire(key, 5, TimeUnit.SECONDS);
        
        return count.intValue();
    }

    /**
     * Reset consecutive counter
     */
    public void resetConsecutiveDetection(Long sessionId, String violationType) {
        String key = CONSECUTIVE_FRAME_PREFIX + sessionId + ":" + violationType;
        redisTemplate.delete(key);
    }

    /**
     * Get current consecutive count
     */
    public int getConsecutiveCount(Long sessionId, String violationType) {
        String key = CONSECUTIVE_FRAME_PREFIX + sessionId + ":" + violationType;
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Integer.parseInt(value) : 0;
    }
}

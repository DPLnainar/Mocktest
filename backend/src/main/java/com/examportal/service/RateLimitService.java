package com.examportal.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token bucket rate limiter for student code submissions
 * Limits: 10 requests per student per minute
 */
@Slf4j
@Service
public class RateLimitService {

    private static final int MAX_REQUESTS = 10;
    private static final long WINDOW_SECONDS = 60;

    private final Map<Long, TokenBucket> buckets = new ConcurrentHashMap<>();

    /**
     * Check if a request is allowed for the given student
     */
    public boolean allowRequest(Long studentId) {
        TokenBucket bucket = buckets.computeIfAbsent(studentId, k -> new TokenBucket());
        boolean allowed = bucket.tryConsume();

        if (!allowed) {
            log.warn("Rate limit exceeded for student: {}", studentId);
        }

        return allowed;
    }

    /**
     * Get the number of seconds until the student can retry
     */
    public long getRetryAfterSeconds(Long studentId) {
        TokenBucket bucket = buckets.get(studentId);
        return bucket != null ? bucket.getRetryAfter() : 0;
    }

    /**
     * Token bucket implementation for rate limiting
     */
    private static class TokenBucket {
        private long tokens = MAX_REQUESTS;
        private long lastRefill = Instant.now().getEpochSecond();

        synchronized boolean tryConsume() {
            refill();
            if (tokens > 0) {
                tokens--;
                return true;
            }
            return false;
        }

        synchronized long getRetryAfter() {
            refill();
            if (tokens > 0)
                return 0;
            return WINDOW_SECONDS - (Instant.now().getEpochSecond() - lastRefill);
        }

        private void refill() {
            long now = Instant.now().getEpochSecond();
            long elapsed = now - lastRefill;
            if (elapsed >= WINDOW_SECONDS) {
                tokens = MAX_REQUESTS;
                lastRefill = now;
            }
        }
    }
}

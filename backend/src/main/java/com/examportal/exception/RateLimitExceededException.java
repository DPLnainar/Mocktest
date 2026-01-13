package com.examportal.exception;

import lombok.Getter;

/**
 * Exception thrown when a student exceeds the rate limit for code submissions
 */
@Getter
public class RateLimitExceededException extends RuntimeException {
    private final long retryAfter;

    public RateLimitExceededException(String message, long retryAfter) {
        super(message);
        this.retryAfter = retryAfter;
    }
}

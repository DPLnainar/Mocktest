package com.examportal.exception;

/**
 * Exception thrown when code execution times out
 */
public class ExecutionTimeoutException extends RuntimeException {
    public ExecutionTimeoutException(String message) {
        super(message);
    }
}

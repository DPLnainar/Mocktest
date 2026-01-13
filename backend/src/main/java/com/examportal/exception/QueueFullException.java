package com.examportal.exception;

/**
 * Exception thrown when the submission queue is full
 */
public class QueueFullException extends RuntimeException {
    public QueueFullException(String message) {
        super(message);
    }
}

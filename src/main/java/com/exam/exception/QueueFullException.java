package com.exam.exception;

/**
 * Thrown when the allocation executor queue is full.
 * Mapped to HTTP 503 (Service Unavailable) by GlobalExceptionHandler.
 */
public class QueueFullException extends RuntimeException {

    public QueueFullException(String message) {
        super(message);
    }
}

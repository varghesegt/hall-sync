package com.exam.exception;

import java.util.UUID;

public class ResourceNotFoundException extends RuntimeException {
    private final UUID allocationRequestId;

    public ResourceNotFoundException(String message, UUID allocationRequestId) {
        super(message);
        this.allocationRequestId = allocationRequestId;
    }

    public ResourceNotFoundException(String message) {
        this(message, null);
    }

    public UUID getAllocationRequestId() {
        return allocationRequestId;
    }
}

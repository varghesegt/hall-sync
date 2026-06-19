package com.exam.exception;

import java.util.UUID;

public class AllocationInProgressException extends RuntimeException {
    private final UUID allocationRequestId;

    public AllocationInProgressException(String message, UUID allocationRequestId) {
        super(message);
        this.allocationRequestId = allocationRequestId;
    }

    public AllocationInProgressException(String message) {
        this(message, null);
    }

    public UUID getAllocationRequestId() {
        return allocationRequestId;
    }
}

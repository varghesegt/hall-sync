package com.exam.exception;

import java.util.UUID;

public class CooldownActiveException extends RuntimeException {
    private final UUID allocationRequestId;

    public CooldownActiveException(String message, UUID allocationRequestId) {
        super(message);
        this.allocationRequestId = allocationRequestId;
    }

    public CooldownActiveException(String message) {
        this(message, null);
    }

    public UUID getAllocationRequestId() {
        return allocationRequestId;
    }
}

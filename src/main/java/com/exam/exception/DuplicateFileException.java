package com.exam.exception;

import java.util.UUID;

public class DuplicateFileException extends RuntimeException {
    private final UUID allocationRequestId;

    public DuplicateFileException(String message, UUID allocationRequestId) {
        super(message);
        this.allocationRequestId = allocationRequestId;
    }

    public DuplicateFileException(String message) {
        this(message, null);
    }

    public UUID getAllocationRequestId() {
        return allocationRequestId;
    }
}

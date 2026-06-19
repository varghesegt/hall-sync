package com.exam.exception;

import java.util.List;
import java.util.UUID;

public class PreflightValidationException extends RuntimeException {
    private final List<String> failures;
    private final UUID allocationRequestId;

    public PreflightValidationException(String message, List<String> failures, UUID allocationRequestId) {
        super(message);
        this.failures = failures;
        this.allocationRequestId = allocationRequestId;
    }

    public PreflightValidationException(String message, List<String> failures) {
        this(message, failures, null);
    }

    public List<String> getFailures() {
        return failures;
    }

    public UUID getAllocationRequestId() {
        return allocationRequestId;
    }
}

package com.exam.engine.model;

import java.util.Objects;

public record AllocationViolation(ViolationType type, String message) {
    public AllocationViolation {
        Objects.requireNonNull(type, "Violation type cannot be null");
        Objects.requireNonNull(message, "Message cannot be null");
    }
}

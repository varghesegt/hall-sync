package com.exam.engine.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record AllocationResult(
        List<SeatAssignment> assignments,
        List<AllocationViolation> violations,
        EngineStats stats,
        Map<String, String> hallReasoning
) {
    public AllocationResult {
        Objects.requireNonNull(assignments, "Assignments cannot be null");
        Objects.requireNonNull(violations, "Violations cannot be null");
        Objects.requireNonNull(stats, "Stats cannot be null");
    }
}

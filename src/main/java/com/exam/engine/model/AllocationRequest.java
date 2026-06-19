package com.exam.engine.model;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.Collections;

public record AllocationRequest(
        List<Student> students,
        List<Hall> halls,
        Map<String, Set<String>> seasonHistory,
        Map<String, Set<String>> positionHistory,
        int seasonSessionIndex,
        String seed
) {
    public AllocationRequest {
        Objects.requireNonNull(students, "Students cannot be null");
        Objects.requireNonNull(halls, "Halls cannot be null");
        Objects.requireNonNull(seasonHistory, "Season history cannot be null");
        // positionHistory can be null for backward compat — default to empty
        if (positionHistory == null) {
            positionHistory = Collections.emptyMap();
        }
    }
}

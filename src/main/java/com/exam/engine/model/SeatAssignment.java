package com.exam.engine.model;

import java.util.Objects;

public record SeatAssignment(
    String registerNumber,
    String hallId,
    int row,
    String col,
    String subjectCode,
    String department,
    String semester,
    String regulation,
    int allocationRiskScore
) {
    public SeatAssignment {
        Objects.requireNonNull(registerNumber, "Register number cannot be null");
        Objects.requireNonNull(hallId, "Hall ID cannot be null");
        Objects.requireNonNull(col, "Column cannot be null");
    }
}

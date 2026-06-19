package com.exam.engine.model;

import java.util.Objects;

public record Student(
    String registerNumber, 
    String department, 
    String subjectCode,
    String semester,
    String regulation
) {
    public Student {
        Objects.requireNonNull(registerNumber, "Register number cannot be null");
        Objects.requireNonNull(department, "Department cannot be null");
    }
}

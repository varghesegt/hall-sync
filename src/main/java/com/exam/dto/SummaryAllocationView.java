package com.exam.dto;

import java.util.UUID;

public record SummaryAllocationView(
        String hallId,
        String hallName,
        String department,
        String subjectName,
        String subjectCode,
        Long studentCount
) {}

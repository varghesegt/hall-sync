package com.exam.dto;

import java.util.UUID;

public record PdfAllocationView(
        String hallId,
        String hallName,
        Integer seatRow,
        String seatCol,
        String registerNumber,
        String studentName,
        String department,
        String subjectCode,
        String semester,
        String regulation,
        Integer riskScore
) {}

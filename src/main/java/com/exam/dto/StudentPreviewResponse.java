package com.exam.dto;

import java.util.List;
import java.util.UUID;

public record StudentPreviewResponse(
    UUID fileId,
    int totalStudents,
    List<StudentPreviewDto> students
) {}

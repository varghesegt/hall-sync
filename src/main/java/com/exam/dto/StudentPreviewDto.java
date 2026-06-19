package com.exam.dto;

import java.util.UUID;

public record StudentPreviewDto(
    String registerNumber,
    String name,
    String department,
    String className,
    String subjectName,
    String subjectCode,
    String semester,
    String regulation
) {
    public static StudentPreviewDto fromParser(String registerNumber, String name, String department, 
                                               String className, String subjectName, String subjectCode,
                                               String semester, String regulation) {
        return new StudentPreviewDto(registerNumber, name, department, className, 
                cleanSubjectName(subjectName), subjectCode, semester, regulation);
    }

    private static String cleanSubjectName(String name) {
        if (name == null) return null;
        // Case-insensitive regex to handle variations in spacing and parens
        return name.replaceAll("(?i)\\s*\\(\\s*CUM\\s+PRACTICAL\\s*\\)\\s*", " ").trim();
    }
}

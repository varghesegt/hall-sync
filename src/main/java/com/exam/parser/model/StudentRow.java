package com.exam.parser.model;

/**
 * Parsed student record. Immutable.
 *
 * If requiresManualResolution is true, the department could not be resolved
 * from the alias map. The ExamSessionFacade MUST block allocation until
 * all unresolved rows are fixed.
 */
public record StudentRow(
        String  registerNumber,
        String  studentName,
        String  department,
        String  className,
        String  subjectName,
        String  subjectCode,
        String  semester,
        String  regulation,
        int     sourcePageNumber,
        int     sourceLineNumber,
        boolean requiresManualResolution
) {
    /** Convenience factory for fully resolved rows */
    public static StudentRow resolved(String registerNumber, String studentName,
                                       String department, String className,
                                       String subjectName, String subjectCode,
                                       String semester, String regulation,
                                       int page, int line) {
        return new StudentRow(registerNumber, studentName, department, className, 
                cleanSubjectName(subjectName), subjectCode, semester, regulation, page, line, false);
    }

    /** Factory for rows with unresolved department */
    public static StudentRow unresolved(String registerNumber, String studentName,
                                         String subjectName, String subjectCode,
                                         String semester, String regulation,
                                         int page, int line) {
        return new StudentRow(registerNumber, studentName, null, "UNKNOWN", 
                cleanSubjectName(subjectName), subjectCode, semester, regulation, page, line, true);
    }

    private static String cleanSubjectName(String name) {
        if (name == null) return null;
        // Case-insensitive regex to handle variations in spacing and parens
        return name.replaceAll("(?i)\\s*\\(\\s*CUM\\s+PRACTICAL\\s*\\)\\s*", " ").trim();
    }
}

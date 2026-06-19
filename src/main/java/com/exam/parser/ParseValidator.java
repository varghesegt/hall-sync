package com.exam.parser;

import com.exam.parser.model.ParseError;
import com.exam.parser.model.ParseErrorCode;
import com.exam.parser.model.StudentRow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Stage 6: Cross-record validation.
 *
 * Duplicate detection runs AFTER full normalization (Gap #8 fix).
 * Any FATAL error → entire upload rejected.
 */
public class ParseValidator {

    public List<ParseError> validate(List<StudentRow> students, 
                                      int totalLinesScanned, int noiseCount, int invalidRowCount) {
        List<ParseError> errors = new ArrayList<>();

        // Zero students — include debugging context
        if (students.isEmpty()) {
            errors.add(new ParseError(0, 0,
                    ParseErrorCode.ZERO_STUDENTS, "WARNING",
                    "No students extracted. Lines processed: " + totalLinesScanned 
                            + ", noise lines: " + noiseCount 
                            + ", invalid rows: " + invalidRowCount
                            + ". Verify PDF contains selectable text and register patterns match.",
                    ""));
            return errors;
        }

        // Duplicate register detection (post-normalization)
        Map<String, List<StudentRow>> grouped = students.stream()
                .collect(Collectors.groupingBy(StudentRow::registerNumber));

        for (var entry : grouped.entrySet()) {
            if (entry.getValue().size() > 1) {
                String lineNums = entry.getValue().stream()
                        .map(s -> String.valueOf(s.sourceLineNumber()))
                        .collect(Collectors.joining(", "));

                errors.add(new ParseError(0, 0,
                        ParseErrorCode.DUPLICATE_REGISTER, "FATAL",
                        "Duplicate register number '" + entry.getKey()
                                + "' found on lines: " + lineNums,
                        entry.getKey()));
            }
        }

        // Blank register (should not happen after extraction, but defense-in-depth)
        for (StudentRow s : students) {
            if (s.registerNumber() == null || s.registerNumber().isBlank()) {
                errors.add(new ParseError(s.sourcePageNumber(), s.sourceLineNumber(),
                        ParseErrorCode.BLANK_REGISTER, "FATAL",
                        "Student row has blank register number",
                        ""));
            }
            if (s.department() == null || s.department().isBlank()) {
                errors.add(new ParseError(s.sourcePageNumber(), s.sourceLineNumber(),
                        ParseErrorCode.BLANK_DEPARTMENT, "WARNING",
                        "Student row has blank or unresolved department",
                        ""));
            }
        }

        // Single department warning
        long distinctDepts = students.stream()
                .map(StudentRow::department)
                .distinct()
                .count();
        if (distinctDepts == 1) {
            errors.add(new ParseError(0, 0,
                    ParseErrorCode.SINGLE_DEPARTMENT, "WARNING",
                    "Only one department detected — verify PDF contains multiple departments",
                    students.get(0).department()));
        }

        return errors;
    }
}

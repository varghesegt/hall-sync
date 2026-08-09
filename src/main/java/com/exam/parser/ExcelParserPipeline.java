package com.exam.parser;

import com.exam.parser.model.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.util.*;

/**
 * Excel Parser Pipeline for Internal Exam mode.
 * Parses .xlsx files with columns:
 *   Register Number, Name, Department, Class, Subject Name, Subject Code, Semester, Regulation
 *
 * Produces the same ParseResult / StudentRow output as PdfParserPipeline.
 */
@Component
public class ExcelParserPipeline {

    private static final Logger logger = LoggerFactory.getLogger(ExcelParserPipeline.class);

    // Case-insensitive column header patterns
    private static final Map<String, String> HEADER_ALIASES = new LinkedHashMap<>();
    static {
        HEADER_ALIASES.put("register number", "registerNumber");
        HEADER_ALIASES.put("reg no", "registerNumber");
        HEADER_ALIASES.put("reg.no", "registerNumber");
        HEADER_ALIASES.put("register no", "registerNumber");
        HEADER_ALIASES.put("registration number", "registerNumber");
        HEADER_ALIASES.put("reg_no", "registerNumber");
        HEADER_ALIASES.put("regno", "registerNumber");
        HEADER_ALIASES.put("roll number", "registerNumber");
        HEADER_ALIASES.put("roll no", "registerNumber");
        HEADER_ALIASES.put("rollno", "registerNumber");
        HEADER_ALIASES.put("roll_no", "registerNumber");
        
        HEADER_ALIASES.put("name", "name");
        HEADER_ALIASES.put("student name", "name");
        HEADER_ALIASES.put("student_name", "name");
        
        HEADER_ALIASES.put("department", "department");
        HEADER_ALIASES.put("dept", "department");
        HEADER_ALIASES.put("dept.", "department");
        HEADER_ALIASES.put("branch", "department");
        
        HEADER_ALIASES.put("class", "className");
        HEADER_ALIASES.put("class name", "className");
        HEADER_ALIASES.put("section", "className");
        
        HEADER_ALIASES.put("subject name", "subjectName");
        HEADER_ALIASES.put("subject", "subjectName");
        HEADER_ALIASES.put("sub name", "subjectName");
        
        HEADER_ALIASES.put("subject code", "subjectCode");
        HEADER_ALIASES.put("sub code", "subjectCode");
        HEADER_ALIASES.put("sub.code", "subjectCode");
        HEADER_ALIASES.put("code", "subjectCode");
        
        HEADER_ALIASES.put("semester", "semester");
        HEADER_ALIASES.put("sem", "semester");
        
        HEADER_ALIASES.put("regulation", "regulation");
        HEADER_ALIASES.put("reg", "regulation");
    }

    public ParseResult parse(byte[] excelBytes) {
        List<StudentRow> students = new ArrayList<>();
        List<ParseError> errors = new ArrayList<>();
        String hash = computeHash(excelBytes);

        try (ByteArrayInputStream bis = new ByteArrayInputStream(excelBytes);
             Workbook workbook = new XSSFWorkbook(bis)) {

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                return new ParseResult(ParseStatus.FAILED, List.of(),
                        List.of(new ParseError(0, 0, ParseErrorCode.EMPTY_PAGE, "FATAL", "Excel file has no sheets", null)),
                        hash, 0, 0);
            }

            // 1. Find header row (search first 10 rows)
            int headerRowIdx = -1;
            Map<String, Integer> columnMapping = null;
            for (int r = 0; r <= Math.min(10, sheet.getLastRowNum()); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                Map<String, Integer> mapping = tryParseHeader(row);
                if (mapping.containsKey("registerNumber") && mapping.containsKey("department")) {
                    headerRowIdx = r;
                    columnMapping = mapping;
                    break;
                }
            }

            if (columnMapping == null) {
                return new ParseResult(ParseStatus.FAILED, List.of(),
                        List.of(new ParseError(0, 0, ParseErrorCode.ZERO_STUDENTS, "FATAL",
                                "Could not find header row with 'Roll/Register Number' and 'Department' columns. " +
                                "Expected columns: Roll Number, Name, Department, Class, Subject Code, Semester, Regulation",
                                null)),
                        hash, 1, 0);
            }

            logger.info("Header found at row {}: {}", headerRowIdx, columnMapping);

            // 2. Parse data rows
            int totalLines = 0;
            for (int r = headerRowIdx + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                totalLines++;

                String registerNumber = getCellString(row, columnMapping.get("registerNumber"));
                if (registerNumber == null || registerNumber.isBlank()) continue; // Skip empty rows

                String name = getCellString(row, columnMapping.get("name"));
                String department = getCellString(row, columnMapping.get("department"));
                String className = getCellString(row, columnMapping.get("className"));
                String subjectName = getCellString(row, columnMapping.get("subjectName"));
                String subjectCode = getCellString(row, columnMapping.get("subjectCode"));
                if ((subjectCode == null || subjectCode.isBlank()) && subjectName != null && !subjectName.isBlank()) {
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\b([A-Za-z]{2,6}\\s*[0-9]{3,5}[A-Za-z0-9-]*)\\b").matcher(subjectName);
                    if (m.find()) {
                        subjectCode = m.group(1).replaceAll("\\s+", "").toUpperCase();
                    }
                }
                String semester = getCellString(row, columnMapping.get("semester"));
                String regulation = getCellString(row, columnMapping.get("regulation"));

                // Validate
                if (department == null || department.isBlank()) {
                    errors.add(new ParseError(1, r + 1, ParseErrorCode.UNKNOWN_DEPARTMENT, "WARNING",
                            "Missing department for student: " + registerNumber, registerNumber));
                    students.add(StudentRow.unresolved(registerNumber, name, subjectName, subjectCode,
                            semester, regulation, 1, r + 1));
                } else {
                    students.add(StudentRow.resolved(registerNumber.trim(), name,
                            department.trim().toUpperCase(), className,
                            subjectName, subjectCode != null ? subjectCode.trim() : null,
                            semester, regulation, 1, r + 1));
                }
            }

            if (students.isEmpty()) {
                return new ParseResult(ParseStatus.FAILED, List.of(),
                        List.of(new ParseError(1, 0, ParseErrorCode.ZERO_STUDENTS, "FATAL",
                                "No student data found in Excel file after header row", null)),
                        hash, 1, totalLines);
            }

            logger.info("Excel parsed: {} students, {} warnings", students.size(), errors.size());

            ParseStatus status = errors.isEmpty() ? ParseStatus.PASSED : ParseStatus.PASSED_WITH_WARNINGS;
            return new ParseResult(status, students, errors, hash, 1, totalLines);

        } catch (Exception e) {
            logger.error("Excel parsing failed", e);
            return new ParseResult(ParseStatus.FAILED, List.of(),
                    List.of(new ParseError(0, 0, ParseErrorCode.EMPTY_PAGE, "FATAL",
                            "Failed to parse Excel file: " + e.getMessage(), null)),
                    hash, 0, 0);
        }
    }

    private Map<String, Integer> tryParseHeader(Row row) {
        Map<String, Integer> mapping = new HashMap<>();
        for (int c = 0; c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell == null) continue;
            String value = getCellValueAsString(cell);
            if (value == null || value.isBlank()) continue;
            
            String normalized = value.trim().toLowerCase().replaceAll("[\\s_.-]+", " ").trim();
            
            for (Map.Entry<String, String> alias : HEADER_ALIASES.entrySet()) {
                if (normalized.equals(alias.getKey()) || normalized.contains(alias.getKey())) {
                    mapping.putIfAbsent(alias.getValue(), c);
                    break;
                }
            }
        }
        return mapping;
    }

    private String getCellString(Row row, Integer colIdx) {
        if (colIdx == null || row == null) return null;
        Cell cell = row.getCell(colIdx);
        if (cell == null) return null;
        return getCellValueAsString(cell);
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double val = cell.getNumericCellValue();
                if (val == Math.floor(val) && !Double.isInfinite(val)) {
                    yield String.valueOf((long) val);
                }
                yield String.valueOf(val);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield cell.getStringCellValue().trim();
                } catch (Exception e) {
                    try {
                        double val = cell.getNumericCellValue();
                        if (val == Math.floor(val)) yield String.valueOf((long) val);
                        yield String.valueOf(val);
                    } catch (Exception e2) {
                        yield null;
                    }
                }
            }
            default -> null;
        };
    }

    private String computeHash(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (Exception e) {
            return "unknown";
        }
    }
}

package com.exam.parser;

import com.exam.parser.model.*;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Robust Excel Parser Pipeline for Internal Exam mode.
 * Parses .xlsx and .xls files with columns such as:
 *   Register Number, Name, Department, Class, Subject Name, Subject Code, Semester, Regulation.
 *
 * Supports:
 * - Both modern (.xlsx) and legacy (.xls) Excel workbooks via WorkbookFactory
 * - Multi-sheet rosters (skips summary/cover sheets automatically)
 * - Flexible title banners (searches up to row 40 for table headers)
 * - Intelligent column role deduction with priority (avoids greedy substring collisions)
 * - Inferred department deduction (from banner, sheet name, or student register number)
 */
@Component
public class ExcelParserPipeline {

    private static final Logger logger = LoggerFactory.getLogger(ExcelParserPipeline.class);

    private static final Pattern SUBJECT_CODE_PATTERN =
            Pattern.compile("\\b([A-Za-z]{2,6}\\s*[0-9]{3,5}[A-Za-z0-9-]*)\\b");

    // Autonomous department code pattern (e.g. 8115U24AD001, 811524CS015, 24EC005)
    private static final Pattern AUTONOMOUS_REG_DEPT_PATTERN =
            Pattern.compile("(?i)^[0-9]*[A-Z]*[0-9]{2}([A-Z]{2})[0-9]+$");

    // Anna University 12-digit format (e.g. 8115 21 104 001)
    private static final Pattern ANNA_UNIV_REG_PATTERN =
            Pattern.compile("(?i)^8115[0-9]{2}(104|106|105|114|205|243|244|103|115)[0-9]{3}$");

    private static final Set<String> NON_STUDENT_KEYWORDS = Set.of(
            "TOTAL", "SIGNATURE", "STAFF", "PRINCIPAL", "HOD", "COE", "DATE",
            "CONTROLLER", "SL NO", "S NO", "SERIAL", "ABSENT", "REMARKS", "INVIGILATOR"
    );

    public ParseResult parse(byte[] excelBytes) {
        if (excelBytes == null || excelBytes.length == 0) {
            return new ParseResult(ParseStatus.FAILED, List.of(),
                    List.of(new ParseError(0, 0, ParseErrorCode.EMPTY_PAGE, "FATAL",
                            "Excel file is empty", null)),
                    "empty", 0, 0);
        }

        String hash = computeHash(excelBytes);
        List<StudentRow> allStudents = new ArrayList<>();
        List<ParseError> errors = new ArrayList<>();
        int totalRowsScanned = 0;

        try (ByteArrayInputStream bis = new ByteArrayInputStream(excelBytes);
             Workbook workbook = WorkbookFactory.create(bis)) {

            DataFormatter formatter = new DataFormatter();
            int numSheets = workbook.getNumberOfSheets();
            if (numSheets == 0) {
                return new ParseResult(ParseStatus.FAILED, List.of(),
                        List.of(new ParseError(0, 0, ParseErrorCode.EMPTY_PAGE, "FATAL",
                                "Excel file has no sheets", null)),
                        hash, 0, 0);
            }

            // Iterate through all sheets to find and aggregate student rosters
            for (int s = 0; s < numSheets; s++) {
                Sheet sheet = workbook.getSheetAt(s);
                if (sheet == null || sheet.getLastRowNum() < 0) continue;

                String sheetName = sheet.getSheetName();
                String sheetDefaultDept = resolveDepartmentFromText(sheetName);

                // 1. Scan for header row (check first 40 rows)
                int headerRowIdx = -1;
                Map<String, Integer> columnMapping = null;

                for (int r = 0; r <= Math.min(40, sheet.getLastRowNum()); r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) continue;

                    // If department not found yet, check banner rows for clues
                    if (sheetDefaultDept == null) {
                        String bannerText = getRowFullText(row, formatter);
                        sheetDefaultDept = resolveDepartmentFromText(bannerText);
                    }

                    Map<String, Integer> mapping = tryParseHeader(row, formatter);
                    if (mapping.containsKey("registerNumber")) {
                        headerRowIdx = r;
                        columnMapping = mapping;
                        break;
                    }

                    // Check for 2-row combined header (merged headers)
                    if (r + 1 <= sheet.getLastRowNum()) {
                        Row nextRow = sheet.getRow(r + 1);
                        if (nextRow != null) {
                            Map<String, Integer> combined = tryParseCombinedHeader(row, nextRow, formatter);
                            if (combined.containsKey("registerNumber")) {
                                headerRowIdx = r + 1;
                                columnMapping = combined;
                                break;
                            }
                        }
                    }
                }

                if (columnMapping == null) {
                    // No student table found in this sheet (e.g. summary or cover page)
                    logger.debug("Sheet '{}' has no student table header, skipping.", sheetName);
                    continue;
                }

                logger.info("Found student roster header in sheet '{}' at row {}: {}", sheetName, headerRowIdx, columnMapping);

                // 2. Parse data rows in this sheet
                for (int r = headerRowIdx + 1; r <= sheet.getLastRowNum(); r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) continue;
                    totalRowsScanned++;

                    String registerNumber = getCellFormatted(row, columnMapping.get("registerNumber"), formatter);
                    if (registerNumber == null || registerNumber.isBlank()) continue;

                    // Sanitize against footer / summary rows
                    if (isNonStudentRow(registerNumber)) continue;

                    String name = getCellFormatted(row, columnMapping.get("name"), formatter);
                    String department = getCellFormatted(row, columnMapping.get("department"), formatter);
                    String className = getCellFormatted(row, columnMapping.get("className"), formatter);
                    String subjectName = getCellFormatted(row, columnMapping.get("subjectName"), formatter);
                    String subjectCode = getCellFormatted(row, columnMapping.get("subjectCode"), formatter);
                    String semester = getCellFormatted(row, columnMapping.get("semester"), formatter);
                    String regulation = getCellFormatted(row, columnMapping.get("regulation"), formatter);

                    // Cross-deduce Subject Code and Subject Name if one is missing
                    if ((subjectCode == null || subjectCode.isBlank()) && subjectName != null && !subjectName.isBlank()) {
                        Matcher m = SUBJECT_CODE_PATTERN.matcher(subjectName);
                        if (m.find()) {
                            subjectCode = m.group(1).replaceAll("\\s+", "").toUpperCase();
                        }
                    }
                    if ((subjectName == null || subjectName.isBlank()) && subjectCode != null && !subjectCode.isBlank()) {
                        subjectName = subjectCode;
                    }

                    // Department Resolution Fallbacks
                    if (department == null || department.isBlank()) {
                        if (sheetDefaultDept != null) {
                            department = sheetDefaultDept;
                        } else {
                            department = inferDepartmentFromRegNo(registerNumber);
                        }
                    }

                    if (department == null || department.isBlank()) {
                        if (className != null && !className.isBlank()) {
                            department = resolveDepartmentFromText(className);
                        }
                    }

                    // Create StudentRow
                    if (department == null || department.isBlank()) {
                        errors.add(new ParseError(s + 1, r + 1, ParseErrorCode.UNKNOWN_DEPARTMENT, "WARNING",
                                "Missing department for student: " + registerNumber, registerNumber));
                        allStudents.add(StudentRow.unresolved(registerNumber.trim(), name, subjectName, subjectCode,
                                semester, regulation, s + 1, r + 1));
                    } else {
                        allStudents.add(StudentRow.resolved(registerNumber.trim(), name,
                                department.trim().toUpperCase(), className,
                                subjectName, subjectCode != null ? subjectCode.trim() : null,
                                semester, regulation, s + 1, r + 1));
                    }
                }
            }

            // 3. Deduplicate students by register number
            Map<String, StudentRow> uniqueStudents = new LinkedHashMap<>();
            for (StudentRow student : allStudents) {
                String regKey = student.registerNumber().trim().toUpperCase();
                if (uniqueStudents.containsKey(regKey)) {
                    errors.add(new ParseError(student.sourcePageNumber(), student.sourceLineNumber(),
                            ParseErrorCode.DUPLICATE_REGISTER, "WARNING",
                            "Duplicate student register number '" + student.registerNumber() + "' at row "
                                    + student.sourceLineNumber() + " was skipped.",
                            student.registerNumber()));
                } else {
                    uniqueStudents.put(regKey, student);
                }
            }

            List<StudentRow> deduplicatedStudents = new ArrayList<>(uniqueStudents.values());

            if (deduplicatedStudents.isEmpty()) {
                return new ParseResult(ParseStatus.FAILED, List.of(),
                        List.of(new ParseError(1, 0, ParseErrorCode.ZERO_STUDENTS, "FATAL",
                                "Could not find any student records in the uploaded Excel file. " +
                                "Please ensure the table contains columns for 'Roll/Register Number', 'Name', 'Department', and 'Subject Code'.",
                                null)),
                        hash, 1, totalRowsScanned);
            }

            logger.info("Excel parsed successfully: {} rows extracted, {} unique students, {} warnings",
                    allStudents.size(), deduplicatedStudents.size(), errors.size());

            ParseStatus status = errors.isEmpty() ? ParseStatus.PASSED : ParseStatus.PASSED_WITH_WARNINGS;
            return new ParseResult(status, deduplicatedStudents, errors, hash, 1, totalRowsScanned);

        } catch (Exception e) {
            logger.error("Excel parsing failed", e);
            String errorMsg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "The uploaded file could not be parsed as an Excel workbook.";
            return new ParseResult(ParseStatus.FAILED, List.of(),
                    List.of(new ParseError(0, 0, ParseErrorCode.EMPTY_PAGE, "FATAL",
                            "Failed to parse Excel file: " + errorMsg, null)),
                    hash, 0, 0);
        }
    }

    /**
     * Tries to identify column roles in a single row.
     */
    private Map<String, Integer> tryParseHeader(Row row, DataFormatter formatter) {
        Map<String, Integer> mapping = new HashMap<>();
        for (int c = 0; c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell == null) continue;
            String text = formatter.formatCellValue(cell).trim();
            if (text.isBlank()) continue;

            String role = identifyColumnRole(text);
            if (role != null) {
                mapping.putIfAbsent(role, c);
            }
        }
        return mapping;
    }

    /**
     * Tries to identify column roles spanning two consecutive header rows (merged headers).
     */
    private Map<String, Integer> tryParseCombinedHeader(Row row1, Row row2, DataFormatter formatter) {
        Map<String, Integer> mapping = tryParseHeader(row1, formatter);
        int maxCells = Math.max(row1.getLastCellNum(), row2.getLastCellNum());
        for (int c = 0; c < maxCells; c++) {
            Cell c2 = row2.getCell(c);
            if (c2 == null) continue;
            String text2 = formatter.formatCellValue(c2).trim();
            if (text2.isBlank()) continue;

            Cell c1 = row1.getCell(c);
            String text1 = c1 != null ? formatter.formatCellValue(c1).trim() : "";
            String combined = (text1 + " " + text2).trim();

            String role = identifyColumnRole(combined);
            if (role == null) {
                role = identifyColumnRole(text2);
            }
            if (role != null) {
                mapping.putIfAbsent(role, c);
            }
        }
        return mapping;
    }

    /**
     * Determines the semantic role of an Excel column header.
     * Evaluates specific composite phrases first to avoid false-positive substring steals.
     */
    public static String identifyColumnRole(String headerText) {
        if (headerText == null || headerText.isBlank()) return null;

        String clean = headerText.toLowerCase()
                .replaceAll("[^a-z0-9]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (clean.isBlank()) return null;

        // 1. Roll / Register Number (matches before anything else)
        if (clean.matches("^(reg(ister|istration)?|roll|univ(ersity)?\\s*reg|student|candidate|applicant)\\s*(no|num|number|id|code)?$")
                || clean.matches("^(regno|rollno|reg_no|roll_no|usn|htno|studid|adm_no|admno)$")
                || clean.equals("register number") || clean.equals("registration number")
                || clean.equals("reg no") || clean.equals("roll no") || clean.equals("roll number")
                || clean.equals("register no") || clean.equals("student id") || clean.equals("stud id")
                || clean.equals("univ reg no") || clean.equals("university reg no")
                || clean.equals("hall ticket") || clean.equals("hall ticket no") || clean.equals("ht no")
                || clean.equals("enrollment no") || clean.equals("enrollment number")
                || clean.equals("admission no") || clean.equals("adm no")
                || clean.equals("reg") || clean.equals("roll")) {
            return "registerNumber";
        }

        // 2. Department / Branch / Degree (Evaluated BEFORE 'name' to prevent 'Department Name' matching 'name')
        if (clean.matches(".*\\b(department|dept|branch|programme|program|discipline|specialization|stream)\\b.*")
                || clean.contains("degree branch") || clean.contains("degree and branch")
                || clean.equals("course branch")) {
            return "department";
        }

        // 3. Subject Code (Evaluated BEFORE 'subjectName' and 'name')
        if (clean.matches(".*\\b(sub|subject|course|paper|qp)\\s*(code|no|num)\\b.*")
                || clean.equals("subcode") || clean.equals("subjectcode")
                || clean.equals("code")) {
            return "subjectCode";
        }

        // 4. Subject Name / Title
        if (clean.matches(".*\\b(sub|subject|course|paper)\\s*(name|title|desc|description)\\b.*")
                || clean.equals("title of the paper") || clean.equals("title of paper")
                || clean.equals("subject") || clean.equals("course") || clean.equals("paper")) {
            return "subjectName";
        }

        // 5. Student Name
        if ((clean.matches(".*\\b(student|candidate|applicant)\\s*name\\b.*")
                || clean.equals("name") || clean.equals("name of student") || clean.equals("name of the student")
                || clean.equals("name of candidate") || clean.equals("name of the candidate")
                || clean.equals("full name"))
                && !clean.contains("staff") && !clean.contains("faculty") && !clean.contains("father") && !clean.contains("mother")) {
            return "name";
        }

        // 6. Class / Section
        if (clean.matches(".*\\b(class|section|sec)\\b.*")
                || clean.equals("class name") || clean.equals("class sec") || clean.equals("year sec") || clean.equals("batch")) {
            return "className";
        }

        // 7. Semester
        if (clean.matches(".*\\b(semester|sem)\\b.*")) {
            return "semester";
        }

        // 8. Regulation
        if (clean.matches(".*\\b(regulation|regn)\\b.*")) {
            return "regulation";
        }

        return null;
    }

    /**
     * Resolves standard college department code from free text (e.g. banners or sheet names).
     */
    public static String resolveDepartmentFromText(String text) {
        if (text == null || text.isBlank()) return null;
        String upper = text.toUpperCase();

        if (upper.contains("ARTIFICIAL INTELLIGENCE") || upper.contains("AI&DS") || upper.contains("AI & DS") || upper.contains("AIDS")) {
            return "AIDS";
        }
        if (upper.contains("AIML") || upper.contains("AI & ML") || upper.contains("MACHINE LEARNING")) {
            return "AIML";
        }
        if (upper.contains("BUSINESS SYSTEMS") || upper.contains("CSBS") || upper.contains("CS & BS")) {
            return "CSBS";
        }
        if (upper.contains("COMPUTER SCIENCE") || upper.contains("CSE")) {
            return "CSE";
        }
        if (upper.contains("ELECTRONICS AND COMMUNICATION") || upper.contains("ELECTRONICS & COMMUNICATION") || upper.contains("ECE")) {
            return "ECE";
        }
        if (upper.contains("ELECTRICAL AND ELECTRONICS") || upper.contains("ELECTRICAL & ELECTRONICS") || upper.contains("EEE")) {
            return "EEE";
        }
        if (upper.contains("INFORMATION TECHNOLOGY") || upper.contains("IT")) {
            // Guard against words containing IT like "LIMIT" or "COMMITTEE"
            if (upper.matches(".*\\bIT\\b.*") || upper.contains("INFORMATION TECH")) {
                return "IT";
            }
        }
        if (upper.contains("MECHANICAL") || upper.contains("MECH")) {
            return "MECH";
        }
        if (upper.contains("CIVIL")) {
            return "CIVIL";
        }
        if (upper.contains("MECHATRONICS") || upper.contains("MCT")) {
            return "MCT";
        }
        if (upper.contains("MBA")) {
            return "MBA";
        }
        if (upper.contains("MCA")) {
            return "MCA";
        }
        return null;
    }

    /**
     * Infers department acronym from a student register number.
     */
    public static String inferDepartmentFromRegNo(String regNo) {
        if (regNo == null || regNo.isBlank()) return null;
        String cleaned = regNo.replaceAll("\\s+", "").toUpperCase();

        // 1. Autonomous Alphanumeric pattern (e.g. 8115U24AD001 -> AD -> AIDS)
        Matcher autoMatcher = AUTONOMOUS_REG_DEPT_PATTERN.matcher(cleaned);
        if (autoMatcher.matches()) {
            String code = autoMatcher.group(1);
            return switch (code) {
                case "AD" -> "AIDS";
                case "AM" -> "AIML";
                case "CB" -> "CSBS";
                case "CE" -> "CIVIL";
                case "CS" -> "CSE";
                case "EC" -> "ECE";
                case "EE" -> "EEE";
                case "IT" -> "IT";
                case "ME" -> "MECH";
                default -> code;
            };
        }

        // 2. Anna University 12-digit numeric pattern (e.g. 811521104001 -> 104 -> CSE)
        Matcher annaMatcher = ANNA_UNIV_REG_PATTERN.matcher(cleaned);
        if (annaMatcher.matches()) {
            String code = annaMatcher.group(1);
            return switch (code) {
                case "104" -> "CSE";
                case "106" -> "ECE";
                case "105" -> "EEE";
                case "114" -> "MECH";
                case "205" -> "IT";
                case "243" -> "AIDS";
                case "244" -> "AIML";
                case "103" -> "CIVIL";
                case "115" -> "MCT";
                default -> code;
            };
        }

        return null;
    }

    private boolean isNonStudentRow(String regNo) {
        if (regNo == null || regNo.length() < 3) return true;
        String upper = regNo.toUpperCase().trim();
        for (String kw : NON_STUDENT_KEYWORDS) {
            if (upper.contains(kw)) return true;
        }
        return false;
    }

    private String getCellFormatted(Row row, Integer colIdx, DataFormatter formatter) {
        if (colIdx == null || row == null) return null;
        Cell cell = row.getCell(colIdx);
        if (cell == null) return null;
        String val = formatter.formatCellValue(cell).trim();
        return val.isBlank() ? null : val;
    }

    private String getRowFullText(Row row, DataFormatter formatter) {
        StringBuilder sb = new StringBuilder();
        for (int c = 0; c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null) {
                String val = formatter.formatCellValue(cell).trim();
                if (!val.isBlank()) {
                    sb.append(val).append(" ");
                }
            }
        }
        return sb.toString().trim();
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

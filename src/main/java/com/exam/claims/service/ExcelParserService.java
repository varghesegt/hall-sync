package com.exam.claims.service;

import com.exam.claims.entity.ClaimRecord;
import com.exam.claims.entity.CollegeDistance;
import com.exam.claims.entity.ScriptDetail;
import com.exam.claims.repository.CollegeDistanceRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses uploaded Excel files containing Google Form responses + script details.
 * Expected columns (0-indexed):
 *   0: S.NO
 *   1: Exam Month
 *   2: Valuation Date
 *   3: Sessions Attended (FN and AN / FN / AN)
 *   4: Name of the Board
 *   5: Post Held
 *   6: Mobile No.
 *   7: Name Title
 *   8: Staff Name
 *   9: Internal/External
 *  10: Designation
 *  11: Institution in which Working with place
 *  12: Issue Reg. Page No.
 *  13: Bank Account Number
 *  14: IFSC Code
 *  15: Bank Name
 *  16: Branch
 *
 * Script details can start from column 17 onwards as pairs:
 *  17: FN Subject Code 1
 *  18: FN No. of Scripts 1
 *  19: FN Subject Code 2
 *  20: FN No. of Scripts 2
 *  ... (up to 15 FN pairs)
 *  Then AN pairs follow.
 *
 * Alternative: The system also looks for a second sheet named "Scripts"
 * with Examiner Code, FN details, and AN details.
 */
@Service
public class ExcelParserService {

    private static final Logger log = LoggerFactory.getLogger(ExcelParserService.class);

    private static final Pattern INSTITUTION_CODE_PATTERN = Pattern.compile("\\(([\\d]+)\\)");
    private static final DateTimeFormatter[] DATE_FORMATS = {
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("d-M-yyyy"),
        DateTimeFormatter.ofPattern("dd-MMM-yyyy")
    };

    @Autowired
    private CollegeDistanceRepository collegeDistanceRepository;

    /**
     * Parse the uploaded Excel file and return a list of ClaimRecords.
     * Also returns warnings for any issues found during parsing.
     */
    public ParseResult parseExcel(MultipartFile file) throws IOException {
        List<ClaimRecord> records = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet mainSheet = workbook.getSheetAt(0);
            Map<String, List<ScriptDetail>> scriptsByExaminerCode = new HashMap<>();

            // Check for separate scripts sheet
            Sheet scriptsSheet = null;
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                String sheetName = workbook.getSheetAt(i).getSheetName().toLowerCase();
                if (sheetName.contains("script") || sheetName.contains("detail")) {
                    if (i != 0) {
                        scriptsSheet = workbook.getSheetAt(i);
                        break;
                    }
                }
            }

            // Parse scripts from separate sheet if available
            if (scriptsSheet != null) {
                scriptsByExaminerCode = parseScriptsSheet(scriptsSheet, warnings);
                log.info("Found separate scripts sheet with {} examiner entries", scriptsByExaminerCode.size());
            }

            // Parse main data sheet
            int startRow = findDataStartRow(mainSheet);
            log.info("Starting data parse from row {}", startRow);

            int summaryColStart = -1;
            int subjectColStart = -1;
            Map<String, Integer> colMap = new HashMap<>();
            Row headerRow = mainSheet.getRow(Math.max(0, startRow - 1));
            if (headerRow != null) {
                for (int col = 0; col < headerRow.getLastCellNum(); col++) {
                    String headerVal = getStringValue(headerRow, col);
                    if (headerVal != null) {
                        String upper = headerVal.toUpperCase().trim();
                        // Detect summary column start
                        if (upper.contains("SCRIPT AMOUNT") || upper.contains("SCRIPT_AMOUNT") ||
                            upper.contains("TRAVELLING ALLOWANCE") || upper.contains("TRAVELING ALLOWANCE") ||
                            upper.contains("DEARNESS ALLOWANCE") ||
                            upper.contains("TOTAL AMOUNT") || upper.contains("TOTAL_AMOUNT") ||
                            upper.contains("AMOUNT IN WORDS") || upper.contains("AMOUNT_IN_WORDS") ||
                            upper.equals("TA") || upper.equals("DA") || upper.equals("TOTAL") ||
                            upper.equals("WORDS") || upper.equals("SCRIPT AMOUNT (RS.)")) {
                            if (summaryColStart == -1) {
                                summaryColStart = col;
                                log.info("Detected summary column '{}' at index {}", headerVal, col);
                            }
                        }
                        
                        // Detect first subject column
                        if (subjectColStart == -1 && (upper.contains("SUBJECT 1") || upper.contains("SUBJECT1") || (upper.startsWith("SUBJECT") && upper.contains("CODE")))) {
                            subjectColStart = col;
                            log.info("Detected Subject columns starting at index {}", col);
                        }

                        // Map standard fields
                        String normalized = upper.replaceAll("[._]", " ");
                        if (normalized.equals("S NO") || normalized.equals("SNO") || normalized.equals("SL NO") || normalized.equals("S N")) {
                            colMap.put("serialNumber", col);
                        } else if (normalized.contains("VALUATION DATE") || normalized.contains("DATE OF VALUATION")) {
                            colMap.put("valuationDate", col);
                        } else if (normalized.contains("SESSION")) {
                            colMap.put("sessionsAttended", col);
                        } else if (normalized.contains("BOARD")) {
                            colMap.put("boardName", col);
                        } else if (normalized.contains("POST HELD") || normalized.equals("POST")) {
                            colMap.put("postHeld", col);
                        } else if (normalized.contains("MOBILE") || normalized.contains("PHONE")) {
                            colMap.put("mobileNo", col);
                        } else if (normalized.contains("NAME TITLE") || normalized.contains("TITLE")) {
                            colMap.put("nameTitle", col);
                        } else if (normalized.contains("STAFF NAME")) {
                            colMap.put("staffName", col);
                        } else if (normalized.contains("INTERNAL/EXTERNAL") || normalized.contains("INT/EXT") || normalized.contains("FACULTY TYPE")) {
                            colMap.put("facultyType", col);
                        } else if (normalized.equals("DESIGNATION")) {
                            colMap.put("designation", col);
                        } else if (normalized.contains("INSTITUTION") || normalized.contains("WORKING WITH PLACE")) {
                            colMap.put("institutionName", col);
                        } else if (normalized.contains("REG PAGE") || normalized.contains("PAGE NO") || upper.contains("ISSUE")) {
                            colMap.put("issueRegPageNo", col);
                        } else if (normalized.contains("ACCOUNT NUMBER") || normalized.contains("ACCOUNT NO")) {
                            colMap.put("bankAccountNumber", col);
                        } else if (normalized.contains("IFSC")) {
                            colMap.put("ifscCode", col);
                        } else if (normalized.contains("BANK NAME")) {
                            colMap.put("bankName", col);
                        } else if (normalized.contains("BRANCH")) {
                            colMap.put("branch", col);
                        } else if (normalized.contains("EXAMINER CODE")) {
                            colMap.put("examinerCode", col);
                        }
                    }
                }
            }

            // Fuzzy/fallback for staffName if exact not mapped
            if (!colMap.containsKey("staffName") && headerRow != null) {
                for (int col = 0; col < headerRow.getLastCellNum(); col++) {
                    String headerVal = getStringValue(headerRow, col);
                    if (headerVal != null) {
                        String upper = headerVal.toUpperCase().trim();
                        if (upper.contains("NAME") && !upper.contains("BANK") && !upper.contains("BOARD") && !upper.contains("TITLE")) {
                            colMap.put("staffName", col);
                            break;
                        }
                    }
                }
            }

            for (int i = startRow; i <= mainSheet.getLastRowNum(); i++) {
                Row row = mainSheet.getRow(i);
                if (row == null || isRowEmpty(row)) continue;

                try {
                    ClaimRecord record = parseRow(row, i, colMap, warnings);
                    if (record != null) {
                        // Try to find script details
                        addScriptDetailsToRecord(record, row, scriptsByExaminerCode, summaryColStart, subjectColStart, colMap, warnings, i);

                        // Look up distance from college_distances table
                        lookupDistance(record, warnings, i);

                        records.add(record);
                    }
                } catch (Exception e) {
                    warnings.add("Row " + (i + 1) + ": Error parsing - " + e.getMessage());
                    log.warn("Error parsing row {}: {}", i + 1, e.getMessage());
                }
            }
        }

        log.info("Parsed {} records with {} warnings", records.size(), warnings.size());
        return new ParseResult(records, warnings);
    }

    private String getStringMapped(Row row, String key, Map<String, Integer> colMap, int fallbackIndex) {
        Integer col = colMap.get(key);
        return getStringValue(row, col != null ? col : fallbackIndex);
    }

    private Integer getIntMapped(Row row, String key, Map<String, Integer> colMap, int fallbackIndex, int defaultValue) {
        Integer col = colMap.get(key);
        return getIntValue(row, col != null ? col : fallbackIndex, defaultValue);
    }

    private LocalDate parseDateMapped(Row row, String key, Map<String, Integer> colMap, int fallbackIndex, List<String> warnings, int rowIndex) {
        Integer col = colMap.get(key);
        return parseDateValue(row, col != null ? col : fallbackIndex, warnings, rowIndex);
    }

    /**
     * Parse a single row into a ClaimRecord
     */
    private ClaimRecord parseRow(Row row, int rowIndex, Map<String, Integer> colMap, List<String> warnings) {
        ClaimRecord record = new ClaimRecord();

        record.setSerialNumber(getIntMapped(row, "serialNumber", colMap, 0, rowIndex + 1));
        record.setValuationDate(parseDateMapped(row, "valuationDate", colMap, 2, warnings, rowIndex));
        record.setSessionsAttended(getStringMapped(row, "sessionsAttended", colMap, 3));
        record.setBoardName(getStringMapped(row, "boardName", colMap, 4));
        record.setPostHeld(getStringMapped(row, "postHeld", colMap, 5));
        record.setMobileNo(getStringMapped(row, "mobileNo", colMap, 6));
        record.setNameTitle(getStringMapped(row, "nameTitle", colMap, 7));
        record.setStaffName(getStringMapped(row, "staffName", colMap, 8));
        record.setFacultyType(getStringMapped(row, "facultyType", colMap, 9));
        record.setDesignation(getStringMapped(row, "designation", colMap, 10));

        String institution = getStringMapped(row, "institutionName", colMap, 11);
        record.setInstitutionName(institution);
        record.setInstitutionCode(extractInstitutionCode(institution));

        record.setIssueRegPageNo(getStringMapped(row, "issueRegPageNo", colMap, 12));
        record.setBankAccountNumber(getStringMapped(row, "bankAccountNumber", colMap, 13));
        record.setIfscCode(getStringMapped(row, "ifscCode", colMap, 14));
        record.setBankName(getStringMapped(row, "bankName", colMap, 15));
        record.setBranch(getStringMapped(row, "branch", colMap, 16));

        // Validate required fields
        if (record.getStaffName() == null || record.getStaffName().trim().isEmpty()) {
            log.debug("Row {}: Missing staff name, silently skipping empty/incomplete row", rowIndex + 1);
            return null;
        }
        if (record.getPostHeld() == null || record.getPostHeld().trim().isEmpty()) {
            warnings.add("Row " + (rowIndex + 1) + ": Missing post held for " + record.getStaffName() + ", defaulting to EXAMINER");
            record.setPostHeld("EXAMINER");
        }

        return record;
    }

    /**
     * Add script details from inline columns or from the scripts sheet
     */
    private void addScriptDetailsToRecord(ClaimRecord record, Row row,
                                           Map<String, List<ScriptDetail>> scriptsByCode,
                                           int summaryColStart,
                                           int subjectColStart,
                                           Map<String, Integer> colMap,
                                           List<String> warnings, int rowIndex) {
        // First check if there's an examiner code and matching scripts from sheet
        String examinerCode = null;
        Integer examinerCodeCol = colMap.get("examinerCode");
        if (examinerCodeCol != null) {
            examinerCode = getStringValue(row, examinerCodeCol);
        } else {
            examinerCode = getStringValue(row, 17);
        }
        
        if (examinerCode != null && !examinerCode.isEmpty() && scriptsByCode.containsKey(examinerCode)) {
            for (ScriptDetail detail : scriptsByCode.get(examinerCode)) {
                ScriptDetail copy = new ScriptDetail(
                    detail.getSessionType(), detail.getSerialNumber(),
                    detail.getSubjectCode(), detail.getNoOfScripts()
                );
                record.addScriptDetail(copy);
            }
            return;
        }

        // Try parsing inline script details starting from subjectColStart
        int startCol = (subjectColStart > 0) ? subjectColStart : 17;
        parseInlineScriptDetails(record, row, startCol, summaryColStart, warnings, rowIndex);

        // Self-Healing Mechanism (Production-Grade Resiliency):
        // If a "Script Amount" column is found, we calculate the total scripts using (Script Amount / 30).
        // If the sum of parsed inline scripts doesn't match this number, we adjust the inline count.
        // This handles cases where the coordinator manually updated total scripts/amounts but left individual details incomplete.
        if (summaryColStart > 0) {
            String scriptAmtStr = getStringValue(row, summaryColStart);
            if (scriptAmtStr != null && !scriptAmtStr.trim().isEmpty()) {
                try {
                    String cleanAmt = scriptAmtStr.replaceAll("[^\\d.]", "");
                    if (!cleanAmt.isEmpty()) {
                        BigDecimal scriptAmount = new BigDecimal(cleanAmt);
                        if (scriptAmount.compareTo(BigDecimal.ZERO) > 0) {
                            // Total scripts = Script Amount / 30
                            int totalScriptsFromAmt = scriptAmount.divide(new BigDecimal("30"), 0, java.math.RoundingMode.HALF_UP).intValue();
                            
                            int currentSum = record.getScriptDetails().stream().mapToInt(ScriptDetail::getNoOfScripts).sum();
                            if (currentSum > 0 && currentSum != totalScriptsFromAmt) {
                                log.info("Row {}: Inline scripts sum ({}) does not match Script Amount total scripts ({})", 
                                    rowIndex + 1, currentSum, totalScriptsFromAmt);
                                
                                if (record.getScriptDetails().size() == 1) {
                                    record.getScriptDetails().get(0).setNoOfScripts(totalScriptsFromAmt);
                                    log.info("Adjusted single script detail count to match Script Amount: {}", totalScriptsFromAmt);
                                }
                            } else if (currentSum == 0) {
                                record.addScriptDetail(new ScriptDetail("FN", 1, "VALUATION", totalScriptsFromAmt));
                                log.info("No inline scripts parsed. Added default script detail from Script Amount: {}", totalScriptsFromAmt);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Row {}: Could not parse Script Amount '{}': {}", rowIndex + 1, scriptAmtStr, e.getMessage());
                }
            }
        }
    }

    /**
     * Parse script details from inline columns (pairs of subject code + count)
     * Format: FN pairs first, then AN pairs
     * Each pair = (Subject Code, No. of Scripts)
     */
    private void parseInlineScriptDetails(ClaimRecord record, Row row, int startCol, int summaryColLimit,
                                           List<String> warnings, int rowIndex) {
        int lastCol = row.getLastCellNum();
        if (summaryColLimit > 0 && summaryColLimit < lastCol) {
            lastCol = summaryColLimit;
        }
        if (startCol >= lastCol) return;

        // Determine if it's a triplet (Google Forms structure: Code, FN, AN) or a pair
        boolean isTriplet = false;
        Row headerRow = row.getSheet().getRow(Math.max(0, findDataStartRow(row.getSheet()) - 1));
        if (headerRow != null && startCol + 2 < headerRow.getLastCellNum()) {
            String nextHeader = getStringValue(headerRow, startCol + 1);
            String nextNextHeader = getStringValue(headerRow, startCol + 2);
            if (nextHeader != null && nextNextHeader != null) {
                String u1 = nextHeader.toUpperCase();
                String u2 = nextNextHeader.toUpperCase();
                if ((u1.contains("FN") && u2.contains("AN")) || (u1.contains("AN") && u2.contains("FN"))) {
                    isTriplet = true;
                }
            }
        }

        if (isTriplet) {
            log.info("Row {}: Parsing inline scripts in TRIPLET format", rowIndex + 1);
            int snFn = 1;
            int snAn = 1;
            for (int col = startCol; col < lastCol - 2; col += 3) {
                String code = getStringValue(row, col);
                if (code == null || code.trim().isEmpty()) continue;
                
                Integer fnCount = getIntValue(row, col + 1, 0);
                Integer anCount = getIntValue(row, col + 2, 0);
                
                if (fnCount != null && fnCount > 0) {
                    record.addScriptDetail(new ScriptDetail("FN", snFn++, code.trim().toUpperCase(), fnCount));
                }
                if (anCount != null && anCount > 0) {
                    record.addScriptDetail(new ScriptDetail("AN", snAn++, code.trim().toUpperCase(), anCount));
                }
            }
        } else {
            log.info("Row {}: Parsing inline scripts in PAIR format", rowIndex + 1);
            List<String[]> allPairs = new ArrayList<>();
            for (int col = startCol; col < lastCol - 1; col += 2) {
                String code = getStringValue(row, col);
                Integer count = getIntValue(row, col + 1, 0);
                if (code != null && !code.trim().isEmpty() && count != null && count > 0) {
                    allPairs.add(new String[]{code.trim().toUpperCase(), String.valueOf(count)});
                }
            }

            if (allPairs.isEmpty()) return;

            int half = allPairs.size() / 2;
            if (half == 0) half = allPairs.size();

            int sn = 1;
            for (int i = 0; i < half && i < allPairs.size(); i++) {
                record.addScriptDetail(new ScriptDetail("FN", sn++,
                    allPairs.get(i)[0], Integer.parseInt(allPairs.get(i)[1])));
            }

            sn = 1;
            for (int i = half; i < allPairs.size(); i++) {
                record.addScriptDetail(new ScriptDetail("AN", sn++,
                    allPairs.get(i)[0], Integer.parseInt(allPairs.get(i)[1])));
            }
        }
    }

    /**
     * Parse scripts from a separate sheet.
     * Expected format: Examiner Code | FN S.N. | FN Subject Code | FN Scripts | AN S.N. | AN Subject Code | AN Scripts
     */
    private Map<String, List<ScriptDetail>> parseScriptsSheet(Sheet sheet, List<String> warnings) {
        Map<String, List<ScriptDetail>> result = new HashMap<>();
        String currentExaminerCode = null;

        int startRow = findDataStartRow(sheet);

        for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isRowEmpty(row)) continue;

            // Check for examiner code in first column
            String code = getStringValue(row, 0);
            if (code != null && !code.trim().isEmpty()) {
                currentExaminerCode = code.trim();
            }

            if (currentExaminerCode == null) continue;

            result.computeIfAbsent(currentExaminerCode, k -> new ArrayList<>());

            // Parse FN details (columns 1-3: S.N., Subject Code, No. of Scripts)
            String fnSubCode = getStringValue(row, 2);
            Integer fnScripts = getIntValue(row, 3, 0);
            if (fnSubCode != null && !fnSubCode.trim().isEmpty() && fnScripts != null && fnScripts > 0) {
                Integer fnSn = getIntValue(row, 1, 0);
                result.get(currentExaminerCode).add(
                    new ScriptDetail("FN", fnSn, fnSubCode.trim(), fnScripts)
                );
            }

            // Parse AN details (columns 4-6: S.N., Subject Code, No. of Scripts)
            String anSubCode = getStringValue(row, 5);
            Integer anScripts = getIntValue(row, 6, 0);
            if (anSubCode != null && !anSubCode.trim().isEmpty() && anScripts != null && anScripts > 0) {
                Integer anSn = getIntValue(row, 4, 0);
                result.get(currentExaminerCode).add(
                    new ScriptDetail("AN", anSn, anSubCode.trim(), anScripts)
                );
            }
        }

        return result;
    }

    /**
     * Look up distance from the college_distances table
     */
    private void lookupDistance(ClaimRecord record, List<String> warnings, int rowIndex) {
        String code = record.getInstitutionCode();
        String name = record.getInstitutionName();

        // Try by institution code first
        if (code != null && !code.isEmpty()) {
            Optional<CollegeDistance> byCode = collegeDistanceRepository.findByInstitutionCode(code);
            if (byCode.isPresent()) {
                record.setDistanceKm(byCode.get().getDistanceKm());
                return;
            }

            // Try finding code in name field
            List<CollegeDistance> byCodeInName = collegeDistanceRepository.findByCodeInName(code);
            if (!byCodeInName.isEmpty()) {
                record.setDistanceKm(byCodeInName.get(0).getDistanceKm());
                return;
            }
        }

        // Try by institution name
        if (name != null && !name.isEmpty()) {
            // Clean name for search
            String searchName = name.replaceAll("\\(.*\\)", "").trim();
            List<CollegeDistance> byName = collegeDistanceRepository.searchByName(searchName);
            if (!byName.isEmpty()) {
                record.setDistanceKm(byName.get(0).getDistanceKm());
                return;
            }

            // Try with first few words
            String[] words = searchName.split("\\s+");
            if (words.length >= 2) {
                String shortName = words[0] + " " + words[1];
                byName = collegeDistanceRepository.searchByName(shortName);
                if (!byName.isEmpty()) {
                    record.setDistanceKm(byName.get(0).getDistanceKm());
                    return;
                }
            }

            // Fuzzy Normalization Fallback Lookup (production-level resiliency)
            String normalizedSearch = normalizeCollegeName(searchName);
            if (!normalizedSearch.isEmpty()) {
                List<CollegeDistance> allColleges = collegeDistanceRepository.findAll();
                for (CollegeDistance college : allColleges) {
                    String normalizedDbName = normalizeCollegeName(college.getInstitutionName());
                    if (!normalizedDbName.isEmpty() && 
                        (normalizedSearch.contains(normalizedDbName) || normalizedDbName.contains(normalizedSearch))) {
                        record.setDistanceKm(college.getDistanceKm());
                        log.info("Fuzzy matched '{}' to database college '{}' with distance {} Km", 
                            name, college.getInstitutionName(), college.getDistanceKm());
                        return;
                    }
                }
            }
        }

        warnings.add("Row " + (rowIndex + 1) + ": Could not find distance for institution: "
            + name + " (" + code + "). TA will be 0.");
        record.setDistanceKm(BigDecimal.ZERO);
    }

    /**
     * Normalizes institution names by removing spaces, special chars, common words, and double letters.
     * This allows resilient matching regardless of typing format or spelling differences (e.g. Velalar vs Vellalar).
     */
    private String normalizeCollegeName(String name) {
        if (name == null) return "";
        return name.toLowerCase()
            .replaceAll("\\s+", "")
            .replaceAll("[.,()&\\-]", "")
            .replaceAll("autonomous", "")
            .replaceAll("institution", "")
            .replaceAll("l+", "l")
            .replaceAll("s+", "s")
            .replaceAll("e+", "e")
            .trim();
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Extract institution code from name like "BANNARI AMMAN INSTITUTE OF TECHNOLOGY (7376)"
     */
    private String extractInstitutionCode(String institutionName) {
        if (institutionName == null) return null;
        Matcher matcher = INSTITUTION_CODE_PATTERN.matcher(institutionName.trim());
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Find the first data row (skip headers)
     */
    private int findDataStartRow(Sheet sheet) {
        for (int i = 0; i <= Math.min(5, sheet.getLastRowNum()); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            String firstCell = getStringValue(row, 0);
            if (firstCell != null) {
                String upper = firstCell.toUpperCase().trim();
                if (upper.equals("S.NO") || upper.equals("S.NO.") || upper.equals("SNO")
                    || upper.equals("SL.NO") || upper.equals("SL. NO") || upper.equals("SL NO")
                    || upper.equals("EXAMINER CODE") || upper.equals("S.N.") || upper.equals("S.N")) {
                    return i + 1;
                }
            }
        }
        return 1; // Default: skip first row (assumed header)
    }

    private boolean isRowEmpty(Row row) {
        for (int i = 0; i < Math.min(10, row.getLastCellNum()); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String val = getStringValue(row, i);
                if (val != null && !val.trim().isEmpty()) return false;
            }
        }
        return true;
    }

    private String getStringValue(Row row, int colIndex) {
        if (row == null) return null;
        Cell cell = row.getCell(colIndex);
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toLocalDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                }
                double num = cell.getNumericCellValue();
                if (num == Math.floor(num) && !Double.isInfinite(num)) {
                    return String.valueOf((long) num);
                }
                return String.valueOf(num);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue().trim();
                } catch (Exception e) {
                    try {
                        double fNum = cell.getNumericCellValue();
                        if (fNum == Math.floor(fNum) && !Double.isInfinite(fNum)) {
                            return String.valueOf((long) fNum);
                        }
                        return String.valueOf(fNum);
                    } catch (Exception e2) {
                        return null;
                    }
                }
            default:
                return null;
        }
    }

    private Integer getIntValue(Row row, int colIndex, int defaultValue) {
        String val = getStringValue(row, colIndex);
        if (val == null || val.trim().isEmpty()) return defaultValue;
        try {
            return (int) Double.parseDouble(val.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private LocalDate parseDateValue(Row row, int colIndex, List<String> warnings, int rowIndex) {
        Cell cell = row.getCell(colIndex);
        if (cell == null) return null;

        // Try date cell
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            try {
                return cell.getLocalDateTimeCellValue().toLocalDate();
            } catch (Exception e) {
                // Fall through to string parsing
            }
        }

        // Try string parsing
        String dateStr = getStringValue(row, colIndex);
        if (dateStr == null || dateStr.trim().isEmpty()) return null;

        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return LocalDate.parse(dateStr.trim(), fmt);
            } catch (DateTimeParseException e) {
                // Try next format
            }
        }

        warnings.add("Row " + (rowIndex + 1) + ": Could not parse date: " + dateStr);
        return null;
    }

    // ==================== RESULT CLASS ====================

    public static class ParseResult {
        private final List<ClaimRecord> records;
        private final List<String> warnings;

        public ParseResult(List<ClaimRecord> records, List<String> warnings) {
            this.records = records;
            this.warnings = warnings;
        }

        public List<ClaimRecord> getRecords() { return records; }
        public List<String> getWarnings() { return warnings; }
    }
}

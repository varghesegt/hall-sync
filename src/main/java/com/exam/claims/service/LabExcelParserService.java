package com.exam.claims.service;

import com.exam.claims.entity.CollegeDistance;
import com.exam.claims.entity.LabClaimRecord;
import com.exam.claims.repository.CollegeDistanceRepository;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LabExcelParserService {

    private static final Logger log = LoggerFactory.getLogger(LabExcelParserService.class);

    private static final Pattern INSTITUTION_CODE_PATTERN = Pattern.compile("\\(([\\d]+)\\)");
    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ofPattern("M/d/yy"),
            DateTimeFormatter.ofPattern("M/d/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("d-M-yyyy"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("d.M.yyyy")
    };

    @Autowired
    private CollegeDistanceRepository collegeDistanceRepository;

    @Autowired
    private LabClaimCalculationService calculationService;

    public ParseResult parseExcel(MultipartFile file) throws IOException {
        List<LabClaimRecord> records = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            int startRow = findDataStartRow(sheet);
            Map<String, Integer> colMap = buildColumnMapping(sheet, startRow);

            boolean isGoogleFormFormat = colMap.containsKey("extStaffName") || colMap.containsKey("intStaffName");
            log.info("Parsing sheet. Detected Google Form Format: {}", isGoogleFormFormat);

            for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) continue;

                try {
                    if (isGoogleFormFormat) {
                        List<LabClaimRecord> pair = parseGoogleFormRow(row, i, colMap, warnings);
                        for (LabClaimRecord record : pair) {
                            lookupDistance(record, warnings, i);
                            calculationService.calculateLabClaim(record);
                            records.add(record);
                        }
                    } else {
                        LabClaimRecord record = parseStandardRow(row, i, colMap, warnings);
                        if (record != null) {
                            lookupDistance(record, warnings, i);
                            calculationService.calculateLabClaim(record);
                            records.add(record);
                        }
                    }
                } catch (Exception e) {
                    warnings.add("Row " + (i + 1) + ": Error parsing lab claim - " + e.getMessage());
                    log.warn("Error parsing lab row {}: {}", i + 1, e.getMessage());
                }
            }
        }

        log.info("Parsed {} lab claim records with {} warnings", records.size(), warnings.size());
        return new ParseResult(records, warnings);
    }

    private Map<String, Integer> buildColumnMapping(Sheet sheet, int startRow) {
        Map<String, Integer> map = new HashMap<>();
        Row headerRow = sheet.getRow(Math.max(0, startRow - 1));
        if (headerRow == null) return map;

        for (int col = 0; col < headerRow.getLastCellNum(); col++) {
            String val = getStringValue(headerRow, col);
            if (val == null) continue;
            String norm = val.toUpperCase().replaceAll("[._]", " ").trim();

            // Common Google Form & Standard fields
            if (norm.contains("DATE OF EXAM") || norm.contains("EXAM DATE") || norm.equals("DATE")) map.putIfAbsent("examDate", col);
            else if (norm.contains("NO OF SESSIONS") || norm.contains("SESSION")) map.putIfAbsent("session", col);
            else if (norm.contains("DEPARTMENT OF CANDIDATE") || norm.contains("DEPARTMENT") || norm.contains("DEPT") || norm.contains("BRANCH")) map.putIfAbsent("department", col);
            else if (norm.contains("SEMESTER") || norm.contains("SEM")) map.putIfAbsent("semester", col);
            else if (norm.contains("SUBJECT CODE") || norm.contains("SUB CODE")) map.putIfAbsent("subjectCode", col);
            else if (norm.contains("SUBJECT NAME") || norm.contains("SUB NAME") || norm.contains("LAB NAME")) map.putIfAbsent("subjectName", col);
            else if (norm.contains("NO OF CANDS REGD") || norm.contains("REGISTERED")) map.putIfAbsent("registeredCount", col);
            else if (norm.contains("NO OF CANDS EXAMINED") || norm.contains("PRESENT") || norm.contains("EXAMINED")) map.putIfAbsent("presentCount", col);

            // Google Form External Examiner specific
            else if (norm.contains("TITLE OF EXTERNAL")) map.put("extTitle", col);
            else if (norm.contains("NAME OF EXTERNAL EXAMINER") || norm.contains("EXTERNAL EXAMINER NAME")) map.put("extStaffName", col);
            else if (norm.contains("PHONE NO OF EXTERNAL")) map.put("extMobile", col);
            else if (norm.contains("DEPARTMENT OF EXTERNAL")) map.put("extDept", col);
            else if (norm.contains("DESIGNATION OF EXTERNAL")) map.put("extDesignation", col);
            else if (norm.contains("COLLEGE OF EXTERNAL")) map.put("extCollege", col);
            else if (norm.contains("BANK ACCOUNT NO") && norm.contains("EXTERNAL")) map.put("extAccount", col);
            else if (norm.contains("BANK NAME") && norm.contains("EXTERNAL")) map.put("extBankName", col);
            else if (norm.contains("IFSC CODE") && norm.contains("EXTERNAL")) map.put("extIfsc", col);
            else if (norm.contains("BANK BRANCH") && norm.contains("EXTERNAL")) map.put("extBranch", col);

            // Google Form Internal Examiner specific
            else if (norm.contains("TITLE OF INTERNAL")) map.put("intTitle", col);
            else if (norm.contains("NAME OF INTERNAL EXAMINER") || norm.contains("INTERNAL EXAMINER NAME")) map.put("intStaffName", col);
            else if (norm.contains("PHONE NO OF INTERNAL")) map.put("intMobile", col);
            else if (norm.contains("DEPARTMENT OF INTERNAL")) map.put("intDept", col);
            else if (norm.contains("DESIGNATION OF INTERNAL")) map.put("intDesignation", col);
            else if (norm.contains("BANK ACCOUNT NO") && norm.contains("INTERNAL")) map.put("intAccount", col);
            else if (norm.contains("BANK NAME") && norm.contains("INTERNAL")) map.put("intBankName", col);
            else if (norm.contains("IFSC CODE") && norm.contains("INTERNAL")) map.put("intIfsc", col);
            else if (norm.contains("BANK BRANCH") && norm.contains("INTERNAL")) map.put("intBranch", col);

            // Google Form Skilled Assistant specific
            else if (norm.contains("TITLE OF SKILLED")) map.put("skilledTitle", col);
            else if (norm.contains("NAME OF SKILLED")) map.put("skilledStaffName", col);
            else if (norm.contains("PHONE NO OF SKILLED")) map.put("skilledMobile", col);
            else if (norm.contains("DEPARTMENT OF SKILLED")) map.put("skilledDept", col);
            else if (norm.contains("DESIGNATION OF SKILLED")) map.put("skilledDesignation", col);
            else if (norm.contains("BANK ACCOUNT NO") && norm.contains("SKILLED")) map.put("skilledAccount", col);
            else if (norm.contains("BANK NAME") && norm.contains("SKILLED")) map.put("skilledBankName", col);
            else if (norm.contains("IFSC CODE") && norm.contains("SKILLED")) map.put("skilledIfsc", col);
            else if (norm.contains("BANK BRANCH") && norm.contains("SKILLED")) map.put("skilledBranch", col);

            // Google Form Lab Technician specific
            else if (norm.contains("TITLE OF LAB TECHNICIAN") || norm.contains("TITLE OF TECHNICIAN")) map.put("techTitle", col);
            else if (norm.contains("NAME OF LAB TECHNICIAN") || norm.contains("TECHNICIAN NAME")) map.put("techStaffName", col);
            else if (norm.contains("PHONE NO OF LAB TECHNICIAN") || norm.contains("PHONE NO OF TECHNICIAN")) map.put("techMobile", col);
            else if (norm.contains("DEPARTMENT OF LAB TECHNICIAN") || norm.contains("DEPARTMENT OF TECHNICIAN")) map.put("techDept", col);
            else if (norm.contains("BANK ACCOUNT NO") && norm.contains("TECHNICIAN")) map.put("techAccount", col);
            else if (norm.contains("BANK NAME") && norm.contains("TECHNICIAN")) map.put("techBankName", col);
            else if (norm.contains("IFSC") && norm.contains("TECHNICIAN")) map.put("techIfsc", col);
            else if (norm.contains("BRANCH") && norm.contains("TECHNICIAN")) map.put("techBranch", col);

            // Standard Single-Staff Row Fallbacks
            else if (norm.contains("STAFF NAME") || norm.contains("EXAMINER NAME") || norm.equals("NAME")) map.putIfAbsent("staffName", col);
            else if (norm.contains("TITLE")) map.putIfAbsent("nameTitle", col);
            else if (norm.contains("ROLE") || norm.contains("CATEGORY") || norm.contains("STAFF TYPE")) map.putIfAbsent("staffRole", col);
            else if (norm.contains("DESIGNATION")) map.putIfAbsent("designation", col);
            else if (norm.contains("COLLEGE") || norm.contains("INSTITUTION")) map.putIfAbsent("institutionName", col);
            else if (norm.contains("MOBILE") || norm.contains("PHONE")) map.putIfAbsent("mobileNo", col);
            else if (norm.contains("ACCOUNT")) map.putIfAbsent("bankAccountNumber", col);
            else if (norm.contains("IFSC")) map.putIfAbsent("ifscCode", col);
            else if (norm.contains("BANK")) map.putIfAbsent("bankName", col);
            else if (norm.contains("BRANCH")) map.putIfAbsent("branch", col);
        }
        return map;
    }

    private List<LabClaimRecord> parseGoogleFormRow(Row row, int rowIndex, Map<String, Integer> colMap, List<String> warnings) {
        List<LabClaimRecord> records = new ArrayList<>();

        LocalDate examDate = parseDateMapped(row, "examDate", colMap, 2, warnings, rowIndex);
        String session = getStringMapped(row, "session", colMap, 3);
        String department = getStringMapped(row, "department", colMap, 8);
        String semester = getStringMapped(row, "semester", colMap, 9);
        String subjectCode = getStringMapped(row, "subjectCode", colMap, 6);
        String subjectName = getStringMapped(row, "subjectName", colMap, 7);

        Integer regCount = getIntMapped(row, "registeredCount", colMap, 11, 30);
        Integer presCount = getIntMapped(row, "presentCount", colMap, 12, 30);

        // 1. External Examiner Record
        String extName = getStringMapped(row, "extStaffName", colMap, 14);
        if (extName != null && !extName.trim().isEmpty()) {
            LabClaimRecord ext = new LabClaimRecord();
            ext.setExamDate(examDate);
            ext.setSession(session);
            ext.setDepartment(department);
            ext.setSemester(semester);
            ext.setSubjectCode(subjectCode);
            ext.setSubjectName(subjectName);
            ext.setRegisteredCount(regCount);
            ext.setPresentCount(presCount);

            ext.setNameTitle(getStringMapped(row, "extTitle", colMap, 13));
            ext.setStaffName(cleanStaffName(ext.getNameTitle(), extName));
            ext.setStaffRole("EXTERNAL_EXAMINER");
            ext.setMobileNo(getStringMapped(row, "extMobile", colMap, 15));
            ext.setDesignation(getStringMapped(row, "extDesignation", colMap, 17));

            String college = getStringMapped(row, "extCollege", colMap, 18);
            ext.setInstitutionName(college);
            ext.setInstitutionCode(extractInstitutionCode(college));

            ext.setBankAccountNumber(getStringMapped(row, "extAccount", colMap, 19));
            ext.setBankName(getStringMapped(row, "extBankName", colMap, 20));
            ext.setIfscCode(getStringMapped(row, "extIfsc", colMap, 21));
            ext.setBranch(getStringMapped(row, "extBranch", colMap, 22));

            records.add(ext);
        }

        // 2. Internal Examiner Record
        String intName = getStringMapped(row, "intStaffName", colMap, 24);
        if (intName != null && !intName.trim().isEmpty()) {
            LabClaimRecord intRec = new LabClaimRecord();
            intRec.setExamDate(examDate);
            intRec.setSession(session);
            intRec.setDepartment(department);
            intRec.setSemester(semester);
            intRec.setSubjectCode(subjectCode);
            intRec.setSubjectName(subjectName);
            intRec.setRegisteredCount(regCount);
            intRec.setPresentCount(presCount);

            intRec.setNameTitle(getStringMapped(row, "intTitle", colMap, 23));
            intRec.setStaffName(cleanStaffName(intRec.getNameTitle(), intName));
            intRec.setStaffRole("INTERNAL_EXAMINER");
            intRec.setMobileNo(getStringMapped(row, "intMobile", colMap, 25));
            intRec.setDesignation(getStringMapped(row, "intDesignation", colMap, 27));
            intRec.setInstitutionName("K.Ramakrishnan College of Engineering");

            intRec.setBankAccountNumber(getStringMapped(row, "intAccount", colMap, 28));
            intRec.setBankName(getStringMapped(row, "intBankName", colMap, 29));
            intRec.setIfscCode(getStringMapped(row, "intIfsc", colMap, 30));
            intRec.setBranch(getStringMapped(row, "intBranch", colMap, 31));

            records.add(intRec);
        }

        // 3. Skilled Assistant Record
        String skilledName = getStringMapped(row, "skilledStaffName", colMap, 33);
        if (skilledName != null && !skilledName.trim().isEmpty()) {
            LabClaimRecord skilledRec = new LabClaimRecord();
            skilledRec.setExamDate(examDate);
            skilledRec.setSession(session);
            skilledRec.setDepartment(department);
            skilledRec.setSemester(semester);
            skilledRec.setSubjectCode(subjectCode);
            skilledRec.setSubjectName(subjectName);
            skilledRec.setRegisteredCount(regCount);
            skilledRec.setPresentCount(presCount);

            skilledRec.setNameTitle(getStringMapped(row, "skilledTitle", colMap, 32));
            skilledRec.setStaffName(cleanStaffName(skilledRec.getNameTitle(), skilledName));
            skilledRec.setStaffRole("SKILLED_ASSISTANT");
            skilledRec.setMobileNo(getStringMapped(row, "skilledMobile", colMap, 34));
            skilledRec.setDesignation(getStringMapped(row, "skilledDesignation", colMap, 36));
            skilledRec.setInstitutionName("K.Ramakrishnan College of Engineering");

            skilledRec.setBankAccountNumber(getStringMapped(row, "skilledAccount", colMap, 37));
            skilledRec.setBankName(getStringMapped(row, "skilledBankName", colMap, 38));
            skilledRec.setIfscCode(getStringMapped(row, "skilledIfsc", colMap, 39));
            skilledRec.setBranch(getStringMapped(row, "skilledBranch", colMap, 40));

            records.add(skilledRec);
        }

        // 4. Lab Technician Record
        String techName = getStringMapped(row, "techStaffName", colMap, 42);
        if (techName != null && !techName.trim().isEmpty()) {
            LabClaimRecord techRec = new LabClaimRecord();
            techRec.setExamDate(examDate);
            techRec.setSession(session);
            techRec.setDepartment(department);
            techRec.setSemester(semester);
            techRec.setSubjectCode(subjectCode);
            techRec.setSubjectName(subjectName);
            techRec.setRegisteredCount(regCount);
            techRec.setPresentCount(presCount);

            techRec.setNameTitle(getStringMapped(row, "techTitle", colMap, 41));
            techRec.setStaffName(cleanStaffName(techRec.getNameTitle(), techName));
            techRec.setStaffRole("LAB_ATTENDER");
            techRec.setMobileNo(getStringMapped(row, "techMobile", colMap, 43));
            techRec.setDesignation("Lab Technician");
            techRec.setInstitutionName("K.Ramakrishnan College of Engineering");

            techRec.setBankAccountNumber(getStringMapped(row, "techAccount", colMap, 45));
            techRec.setBankName(getStringMapped(row, "techBankName", colMap, 46));
            techRec.setIfscCode(getStringMapped(row, "techIfsc", colMap, 47));
            techRec.setBranch(getStringMapped(row, "techBranch", colMap, 48));

            records.add(techRec);
        }

        return records;
    }

    private LabClaimRecord parseStandardRow(Row row, int rowIndex, Map<String, Integer> colMap, List<String> warnings) {
        LabClaimRecord record = new LabClaimRecord();

        record.setExamDate(parseDateMapped(row, "examDate", colMap, 1, warnings, rowIndex));
        record.setSession(getStringMapped(row, "session", colMap, 2));
        record.setDepartment(getStringMapped(row, "department", colMap, 3));
        record.setSemester(getStringMapped(row, "semester", colMap, 4));
        record.setSubjectCode(getStringMapped(row, "subjectCode", colMap, 5));
        record.setSubjectName(getStringMapped(row, "subjectName", colMap, 6));
        record.setBatchNumber(getStringMapped(row, "batchNumber", colMap, 7));

        record.setRegisteredCount(getIntMapped(row, "registeredCount", colMap, 8, 30));
        record.setPresentCount(getIntMapped(row, "presentCount", colMap, 9, 30));

        String name = getStringMapped(row, "staffName", colMap, 10);
        String title = getStringMapped(row, "nameTitle", colMap, 11);
        record.setNameTitle(title);
        record.setStaffName(cleanStaffName(title, name));

        String rawRole = getStringMapped(row, "staffRole", colMap, 12);
        record.setStaffRole(normalizeRole(rawRole));

        record.setDesignation(getStringMapped(row, "designation", colMap, 13));

        String institution = getStringMapped(row, "institutionName", colMap, 14);
        record.setInstitutionName(institution);
        record.setInstitutionCode(extractInstitutionCode(institution));

        record.setMobileNo(getStringMapped(row, "mobileNo", colMap, 15));
        record.setBankAccountNumber(getStringMapped(row, "bankAccountNumber", colMap, 16));
        record.setIfscCode(getStringMapped(row, "ifscCode", colMap, 17));
        record.setBankName(getStringMapped(row, "bankName", colMap, 18));
        record.setBranch(getStringMapped(row, "branch", colMap, 19));

        if (record.getStaffName() == null || record.getStaffName().trim().isEmpty()) {
            return null; // Silent skip for empty rows
        }

        return record;
    }

    private String cleanStaffName(String title, String name) {
        if (name == null || name.trim().isEmpty()) return "";
        String trimmedName = name.trim();
        if (title != null && !title.trim().isEmpty()) {
            String trimmedTitle = title.trim();
            if (!trimmedName.toLowerCase().startsWith(trimmedTitle.toLowerCase())) {
                return trimmedTitle + " " + trimmedName;
            }
        }
        return trimmedName;
    }

    private String normalizeRole(String rawRole) {
        if (rawRole == null) return "INTERNAL_EXAMINER";
        String u = rawRole.toUpperCase().trim();
        if (u.contains("EXTERNAL")) return "EXTERNAL_EXAMINER";
        if (u.contains("TECH") || u.contains("SKILLED")) return "SKILLED_ASSISTANT";
        if (u.contains("ATTEND") || u.contains("HELP")) return "LAB_ATTENDER";
        return "INTERNAL_EXAMINER";
    }

    private void lookupDistance(LabClaimRecord record, List<String> warnings, int rowIndex) {
        if (!"EXTERNAL_EXAMINER".equalsIgnoreCase(record.getStaffRole())) return;

        String code = record.getInstitutionCode();
        String name = record.getInstitutionName();

        if (code != null && !code.isEmpty()) {
            Optional<CollegeDistance> byCode = collegeDistanceRepository.findByInstitutionCode(code);
            if (byCode.isPresent()) {
                record.setDistanceKm(byCode.get().getDistanceKm());
                return;
            }
        }

        if (name != null && !name.isEmpty()) {
            String searchName = name.replaceAll("\\(.*\\)", "").trim();
            List<CollegeDistance> byName = collegeDistanceRepository.searchByName(searchName);
            if (!byName.isEmpty()) {
                record.setDistanceKm(byName.get(0).getDistanceKm());
                return;
            }
        }
        record.setDistanceKm(BigDecimal.ZERO);
    }

    private int findDataStartRow(Sheet sheet) {
        for (int i = 0; i <= Math.min(5, sheet.getLastRowNum()); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            String firstCell = getStringValue(row, 0);
            if (firstCell != null) {
                String u = firstCell.toUpperCase().trim();
                if (u.contains("S.NO") || u.contains("SL.NO") || u.contains("DATE") || u.contains("EXAM") || u.contains("TIMESTAMP")) {
                    return i + 1;
                }
            }
        }
        return 1;
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

    private String getStringMapped(Row row, String key, Map<String, Integer> colMap, int fallbackIndex) {
        Integer col = colMap.get(key);
        return getStringValue(row, col != null ? col : fallbackIndex);
    }

    private Integer getIntMapped(Row row, String key, Map<String, Integer> colMap, int fallbackIndex, int defaultValue) {
        Integer col = colMap.get(key);
        String val = getStringValue(row, col != null ? col : fallbackIndex);
        if (val == null || val.trim().isEmpty()) return defaultValue;
        try {
            return (int) Double.parseDouble(val.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private LocalDate parseDateMapped(Row row, String key, Map<String, Integer> colMap, int fallbackIndex, List<String> warnings, int rowIndex) {
        Integer col = colMap.get(key);
        int targetCol = col != null ? col : fallbackIndex;

        Cell cell = row.getCell(targetCol);
        if (cell != null && cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            try {
                return cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            } catch (Exception ignored) {}
        }

        String dateStr = getStringValue(row, targetCol);
        if (dateStr == null || dateStr.trim().isEmpty()) return null;

        // Strip timestamp portion if present (e.g. "3/30/2026 12:04:12" -> "3/30/2026")
        String dateOnly = dateStr.trim().split("\\s+")[0];

        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return LocalDate.parse(dateOnly, fmt);
            } catch (Exception ignored) {}
        }
        return null;
    }

    private String getStringValue(Row row, int colIndex) {
        if (row == null) return null;
        Cell cell = row.getCell(colIndex);
        if (cell == null) return null;
        return new DataFormatter().formatCellValue(cell);
    }

    private String extractInstitutionCode(String name) {
        if (name == null) return null;
        Matcher m = INSTITUTION_CODE_PATTERN.matcher(name.trim());
        return m.find() ? m.group(1) : null;
    }

    public static class ParseResult {
        private final List<LabClaimRecord> records;
        private final List<String> warnings;

        public ParseResult(List<LabClaimRecord> records, List<String> warnings) {
            this.records = records;
            this.warnings = warnings;
        }

        public List<LabClaimRecord> getRecords() { return records; }
        public List<String> getWarnings() { return warnings; }
    }
}

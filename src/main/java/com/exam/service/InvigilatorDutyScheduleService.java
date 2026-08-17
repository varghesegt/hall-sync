package com.exam.service;

import com.exam.entity.*;
import com.exam.repository.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates the "Invigilation Duty Schedule" Excel slip
 * matching the physical COE format used by the Controller of Examinations.
 *
 * Black, White & Light Grey layout for official printing.
 * Roll No column CENTER ALIGNED, compact A4 Landscape fit.
 *
 * Session Times:
 *   FN: 10.00 AM TO 12.00 PM
 *   AN: 02.40 PM TO 04.40 PM
 */
@Service
public class InvigilatorDutyScheduleService {

    private static final Logger logger = LoggerFactory.getLogger(InvigilatorDutyScheduleService.class);

    private final AllocationRepository allocationRepository;
    private final AllocationBatchRepository batchRepository;
    private final InvigilatorDutyRepository dutyRepository;

    public InvigilatorDutyScheduleService(AllocationRepository allocationRepository,
                                           AllocationBatchRepository batchRepository,
                                           InvigilatorDutyRepository dutyRepository) {
        this.allocationRepository = allocationRepository;
        this.batchRepository = batchRepository;
        this.dutyRepository = dutyRepository;
    }

    @Transactional(readOnly = true)
    public void generateDutyScheduleExcel(UUID batchId, OutputStream out) {
        AllocationBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalStateException("Batch not found: " + batchId));
        ExamSession session = batch.getExamSession();

        // Fetch all allocations for this batch
        List<Allocation> allocations = allocationRepository.findByBatchIdWithDetails(batchId);
        if (allocations.isEmpty()) {
            throw new IllegalStateException("No allocations found for batch: " + batchId);
        }

        // Try to fetch duties (optional — if assigned, fill staff name; otherwise leave blank)
        Map<String, InvigilatorDuty> hallDutyMap = new LinkedHashMap<>();
        try {
            List<InvigilatorDuty> duties = dutyRepository.findByBatchIdOrderByHallIdAsc(batchId);
            for (InvigilatorDuty duty : duties) {
                hallDutyMap.put(duty.getHall().getId(), duty);
            }
        } catch (Exception e) {
            logger.debug("No duties found for batch {}, staff columns will be blank", batchId);
        }

        // Build hall -> students mapping (ordered by hall name)
        Map<String, HallData> hallDataMap = new TreeMap<>();
        for (Allocation alloc : allocations) {
            Hall hall = alloc.getHall();
            Student student = alloc.getStudent();
            HallData hd = hallDataMap.computeIfAbsent(hall.getId(),
                    k -> new HallData(hall.getId(), hall.getName()));
            hd.addStudent(student.getRegisterNumber(), student.getDepartment());
        }

        // Sort halls by name
        List<HallData> sortedHalls = hallDataMap.values().stream()
                .sorted(Comparator.comparing(h -> h.hallName))
                .collect(Collectors.toList());

        // Get college name
        String tenantId = com.exam.config.tenant.TenantContext.getCurrentTenant();
        String collegeName = com.exam.config.tenant.TenantContext.getCollegeName(tenantId);
        if (collegeName == null) collegeName = "K.RAMAKRISHNAN COLLEGE OF ENGINEERING";

        String examTime = getExamTime(session.getSession(), session.getExamType());
        String dateStr = session.getExamDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        String sessionSuffix = session.getSession() != null ? session.getSession().toUpperCase() : "FN";

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Invigilation Duty Schedule");
            sheet.setDisplayGridlines(true);
            sheet.setPrintGridlines(true);

            // ═══════════════════ FONTS (BLACK & WHITE) ═══════════════════
            Font titleFont = workbook.createFont();
            titleFont.setFontName("Times New Roman");
            titleFont.setFontHeightInPoints((short) 13);
            titleFont.setBold(true);

            Font collegeFont = workbook.createFont();
            collegeFont.setFontName("Times New Roman");
            collegeFont.setFontHeightInPoints((short) 13);
            collegeFont.setBold(true);

            Font subFont = workbook.createFont();
            subFont.setFontName("Times New Roman");
            subFont.setFontHeightInPoints((short) 10);
            subFont.setBold(true);

            Font metaFont = workbook.createFont();
            metaFont.setFontName("Times New Roman");
            metaFont.setFontHeightInPoints((short) 10);
            metaFont.setBold(true);

            Font colHeaderFont = workbook.createFont();
            colHeaderFont.setFontName("Times New Roman");
            colHeaderFont.setFontHeightInPoints((short) 10);
            colHeaderFont.setBold(true);

            Font dataFont = workbook.createFont();
            dataFont.setFontName("Times New Roman");
            dataFont.setFontHeightInPoints((short) 9.5);

            Font dataBoldFont = workbook.createFont();
            dataBoldFont.setFontName("Times New Roman");
            dataBoldFont.setFontHeightInPoints((short) 9.5);
            dataBoldFont.setBold(true);

            Font sigFont = workbook.createFont();
            sigFont.setFontName("Times New Roman");
            sigFont.setFontHeightInPoints((short) 10);
            sigFont.setBold(true);

            // ═══════════════════ STYLES (BLACK & WHITE + GREY HEADINGS) ═══════════════════

            // Header titles (Pure White BG, Black text, Centered)
            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle collegeStyle = workbook.createCellStyle();
            collegeStyle.setFont(collegeFont);
            collegeStyle.setAlignment(HorizontalAlignment.CENTER);
            collegeStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle subStyle = workbook.createCellStyle();
            subStyle.setFont(subFont);
            subStyle.setAlignment(HorizontalAlignment.CENTER);
            subStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // Meta info (Date/Time)
            CellStyle metaLeftStyle = workbook.createCellStyle();
            metaLeftStyle.setFont(metaFont);
            metaLeftStyle.setAlignment(HorizontalAlignment.LEFT);
            metaLeftStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle metaRightStyle = workbook.createCellStyle();
            metaRightStyle.setFont(metaFont);
            metaRightStyle.setAlignment(HorizontalAlignment.RIGHT);
            metaRightStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // Table Column Headers (LIGHT GREY BG, Black text, Centered, Thin Borders)
            CellStyle colHeaderStyle = workbook.createCellStyle();
            colHeaderStyle.setFont(colHeaderFont);
            colHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
            colHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            colHeaderStyle.setWrapText(true);
            colHeaderStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            colHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setBorders(colHeaderStyle, BorderStyle.THIN);

            // Data Cells — Centered
            CellStyle dataCenterStyle = workbook.createCellStyle();
            dataCenterStyle.setFont(dataFont);
            dataCenterStyle.setAlignment(HorizontalAlignment.CENTER);
            dataCenterStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            dataCenterStyle.setWrapText(true);
            setBorders(dataCenterStyle, BorderStyle.THIN);

            // Data Cells — Left
            CellStyle dataLeftStyle = workbook.createCellStyle();
            dataLeftStyle.setFont(dataFont);
            dataLeftStyle.setAlignment(HorizontalAlignment.LEFT);
            dataLeftStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            dataLeftStyle.setWrapText(true);
            setBorders(dataLeftStyle, BorderStyle.THIN);

            // Data Cells — Bold Centered
            CellStyle dataBoldCenterStyle = workbook.createCellStyle();
            dataBoldCenterStyle.setFont(dataBoldFont);
            dataBoldCenterStyle.setAlignment(HorizontalAlignment.CENTER);
            dataBoldCenterStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            dataBoldCenterStyle.setWrapText(true);
            setBorders(dataBoldCenterStyle, BorderStyle.THIN);

            // Data Cells — Bold Left
            CellStyle dataBoldLeftStyle = workbook.createCellStyle();
            dataBoldLeftStyle.setFont(dataBoldFont);
            dataBoldLeftStyle.setAlignment(HorizontalAlignment.LEFT);
            dataBoldLeftStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            dataBoldLeftStyle.setWrapText(true);
            setBorders(dataBoldLeftStyle, BorderStyle.THIN);

            // Signature style
            CellStyle sigStyle = workbook.createCellStyle();
            sigStyle.setFont(sigFont);
            sigStyle.setAlignment(HorizontalAlignment.CENTER);
            sigStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            int totalCols = 11;

            // ═══════════════════ HEADER SECTION ═══════════════════
            int rowNum = 0;

            // Row 0: "Office of the Controller of Examinations"
            Row r0 = sheet.createRow(rowNum++);
            r0.setHeightInPoints(20);
            createMergedCell(sheet, r0, 0, totalCols, "Office of the Controller of Examinations", titleStyle, rowNum - 1);

            // Row 1: College Name
            Row r1 = sheet.createRow(rowNum++);
            r1.setHeightInPoints(20);
            createMergedCell(sheet, r1, 0, totalCols, collegeName.toUpperCase(), collegeStyle, rowNum - 1);

            // Row 2: "(AUTONOMOUS)"
            Row r2 = sheet.createRow(rowNum++);
            r2.setHeightInPoints(15);
            createMergedCell(sheet, r2, 0, totalCols, "(AUTONOMOUS)", subStyle, rowNum - 1);

            // Row 3: "SAMAYAPURAM, TRICHY"
            Row r3 = sheet.createRow(rowNum++);
            r3.setHeightInPoints(15);
            createMergedCell(sheet, r3, 0, totalCols, "SAMAYAPURAM, TRICHY", subStyle, rowNum - 1);

            // Row 4: Semester / Year info (e.g. INT -2)
            Row r4 = sheet.createRow(rowNum++);
            r4.setHeightInPoints(15);
            createMergedCell(sheet, r4, 0, totalCols, buildIntLabel(session), subStyle, rowNum - 1);

            // Row 5: CIA LABEL (e.g. EXAM 2 - INVIGILATION DUTY SCHEDULE)
            Row r5 = sheet.createRow(rowNum++);
            r5.setHeightInPoints(17);
            createMergedCell(sheet, r5, 0, totalCols, buildCiaLabel(session), subStyle, rowNum - 1);

            // Row 6: DATE and TIME
            Row r6 = sheet.createRow(rowNum++);
            r6.setHeightInPoints(18);
            Cell dateCell = r6.createCell(0);
            dateCell.setCellValue("DATE : " + dateStr + " / " + sessionSuffix);
            dateCell.setCellStyle(metaLeftStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 4));

            Cell timeCell = r6.createCell(5);
            timeCell.setCellValue("TIME : " + examTime);
            timeCell.setCellStyle(metaRightStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 5, totalCols - 1));

            // Row 7: Spacer
            sheet.createRow(rowNum++).setHeightInPoints(4);

            // ═══════════════════ COLUMN HEADERS ═══════════════════
            Row headerRow = sheet.createRow(rowNum++);
            headerRow.setHeightInPoints(24);
            String[] headers = {"S.No", "Staff Name", "Dept.", "Allotted Staff", "Hall No.",
                    "Roll No.", "Strength", "Time", "Sign", "Absentees", "Sign"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(colHeaderStyle);
            }

            // ═══════════════════ DATA ROWS ═══════════════════
            int sno = 1;
            for (HallData hd : sortedHalls) {
                String rollNoText = hd.buildRollNoBreakdown();
                int deptCount = hd.getDepartmentCount();

                Row dataRow = sheet.createRow(rowNum++);
                // Optimized compact row height to fit A4 Landscape perfectly: 14pt per line + 6pt padding
                dataRow.setHeightInPoints(Math.max(25, deptCount * 14 + 6));

                // Col 0: S.No
                Cell c0 = dataRow.createCell(0);
                c0.setCellValue(sno++);
                c0.setCellStyle(dataCenterStyle);

                // Col 1: Staff Name (from duties if available, otherwise blank for manual fill)
                Cell c1 = dataRow.createCell(1);
                InvigilatorDuty duty = hallDutyMap.get(hd.hallId);
                c1.setCellValue(duty != null ? duty.getFaculty().getName() : "");
                c1.setCellStyle(dataBoldLeftStyle);

                // Col 2: Dept (staff department if duty assigned, otherwise blank)
                Cell c2 = dataRow.createCell(2);
                c2.setCellValue(duty != null ? duty.getFaculty().getDepartment() : "");
                c2.setCellStyle(dataCenterStyle);

                // Col 3: Allotted Staff (blank for manual fill)
                Cell c3 = dataRow.createCell(3);
                c3.setCellValue("");
                c3.setCellStyle(dataCenterStyle);

                // Col 4: Hall No.
                Cell c4 = dataRow.createCell(4);
                c4.setCellValue(hd.hallName);
                c4.setCellStyle(dataBoldCenterStyle);

                // Col 5: Roll No. (CENTER ALIGNED, Single line per dept: FirstRegNo-LastRegNo(Count))
                Cell c5 = dataRow.createCell(5);
                c5.setCellValue(rollNoText);
                c5.setCellStyle(dataCenterStyle);

                // Col 6: Strength
                Cell c6 = dataRow.createCell(6);
                c6.setCellValue(hd.getTotalStudents());
                c6.setCellStyle(dataCenterStyle);

                // Col 7-10: Time, Sign, Absentees, Slip (blank for manual fill)
                for (int col = 7; col <= 10; col++) {
                    Cell c = dataRow.createCell(col);
                    c.setCellValue("");
                    c.setCellStyle(dataCenterStyle);
                }
            }

            // ═══════════════════ COLUMN WIDTHS (OPTIMIZED A4 LANDSCAPE FIT) ═══════════════════
            sheet.setColumnWidth(0, 2000);   // S.No
            sheet.setColumnWidth(1, 6500);   // Staff Name
            sheet.setColumnWidth(2, 3200);   // Dept.
            sheet.setColumnWidth(3, 3800);   // Allotted Staff
            sheet.setColumnWidth(4, 3200);   // Hall No.
            sheet.setColumnWidth(5, 14000);  // Roll No. (CENTER ALIGNED, perfect fit)
            sheet.setColumnWidth(6, 2800);   // Strength
            sheet.setColumnWidth(7, 2800);   // Time
            sheet.setColumnWidth(8, 2800);   // Sign
            sheet.setColumnWidth(9, 3600);   // Absentees
            sheet.setColumnWidth(10, 2600);  // Slip

            // ═══════════════════ SIGNATURE SECTION ═══════════════════
            rowNum += 1;
            Row sigRow = sheet.createRow(rowNum);
            sigRow.setHeightInPoints(20);

            Cell sigLeft = sigRow.createCell(0);
            sigLeft.setCellValue("Exam Cell Coordinator");
            sigLeft.setCellStyle(sigStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 0, 3));

            Cell sigRight = sigRow.createCell(7);
            sigRight.setCellValue("Controller of Examinations");
            sigRight.setCellStyle(sigStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 7, 10));

            // Print setup
            sheet.getPrintSetup().setLandscape(true);
            sheet.getPrintSetup().setFitWidth((short) 1);
            sheet.getPrintSetup().setFitHeight((short) 0);
            sheet.setFitToPage(true);
            sheet.setMargin(Sheet.LeftMargin, 0.3);
            sheet.setMargin(Sheet.RightMargin, 0.3);
            sheet.setMargin(Sheet.TopMargin, 0.4);
            sheet.setMargin(Sheet.BottomMargin, 0.4);

            workbook.write(out);
            logger.info("Generated A4 Landscape Centered Invigilation Duty Schedule Excel for batch {}: {} halls, {} students",
                    batchId, sortedHalls.size(), allocations.size());

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Invigilation Duty Schedule Excel", e);
        }
    }

    // ═══════════════════ HELPER METHODS ═══════════════════

    private void setBorders(CellStyle style, BorderStyle bs) {
        style.setBorderTop(bs);
        style.setBorderBottom(bs);
        style.setBorderLeft(bs);
        style.setBorderRight(bs);
    }

    private void createMergedCell(Sheet sheet, Row row, int startCol, int totalCols, String value, CellStyle style, int rowIdx) {
        Cell cell = row.createCell(startCol);
        cell.setCellValue(value);
        cell.setCellStyle(style);
        for (int i = startCol + 1; i < totalCols; i++) {
            Cell filler = row.createCell(i);
            filler.setCellStyle(style);
        }
        if (totalCols > 1) {
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, startCol, totalCols - 1));
        }
    }

    private String buildIntLabel(ExamSession session) {
        if (session != null && "SEMESTER".equalsIgnoreCase(session.getExamType())) {
            return session.getSeasonId() != null ? session.getSeasonId().toUpperCase() : "END SEMESTER EXAMINATIONS";
        }
        String name = session != null ? session.getName() : null;
        if (name != null && !name.isBlank()) {
            String upper = name.toUpperCase().trim();
            String num = upper.replaceAll("[^0-9]", "");
            if (!num.isEmpty()) {
                return "INT -" + num;
            }
        }
        return "INT -2";
    }

    private String buildCiaLabel(ExamSession session) {
        if (session != null && "SEMESTER".equalsIgnoreCase(session.getExamType())) {
            return "END SEMESTER EXAMINATIONS - INVIGILATION DUTY SCHEDULE";
        }
        String name = session != null ? session.getName() : null;
        if (name != null && !name.isBlank()) {
            String upper = name.toUpperCase().trim();
            String num = upper.replaceAll("[^0-9]", "");
            if (!num.isEmpty()) {
                return "EXAM " + num + " - INVIGILATION DUTY SCHEDULE";
            }
            if (upper.equals("CIA") || upper.equals("INT")) {
                return "EXAM 2 - INVIGILATION DUTY SCHEDULE";
            }
            if (!upper.contains("DUTY")) {
                return upper + " - INVIGILATION DUTY SCHEDULE";
            }
            return upper;
        }
        return "EXAM 2 - INVIGILATION DUTY SCHEDULE";
    }

    private String getExamTime(String sessionType, String examType) {
        boolean isInternal = "INTERNAL".equalsIgnoreCase(examType);
        if (sessionType == null) {
            return isInternal ? "10.00 AM TO 12.00 PM" : "10.00 AM TO 01.00 PM";
        }
        String s = sessionType.trim().toUpperCase();
        boolean isAN = s.contains("AN") || s.equals("AFTERNOON");

        if (isInternal) {
            return isAN ? "02.40 PM TO 04.40 PM" : "10.00 AM TO 12.00 PM";
        } else {
            // Semester Exam Mode timings: FN is 10.00 AM TO 01.00 PM, AN is 01.45 PM TO 04.45 PM
            return isAN ? "01.45 PM TO 04.45 PM" : "10.00 AM TO 01.00 PM";
        }
    }

    // ═══════════════════ INNER DATA CLASS ═══════════════════

    private static class HallData {
        final String hallId;
        final String hallName;
        final Map<String, List<String>> deptStudents = new TreeMap<>();

        HallData(String hallId, String hallName) {
            this.hallId = hallId;
            this.hallName = hallName;
        }

        void addStudent(String registerNumber, String department) {
            deptStudents.computeIfAbsent(department, k -> new ArrayList<>()).add(registerNumber);
        }

        int getTotalStudents() {
            return deptStudents.values().stream().mapToInt(List::size).sum();
        }

        int getDepartmentCount() {
            return Math.max(1, deptStudents.size());
        }

        /**
         * Formatted exactly as in reference COE excel (Center Aligned):
         * 8115U23EC024-8115U23EC045(20)
         * 8115U23EE001-8115U23EE020(20)
         */
        String buildRollNoBreakdown() {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, List<String>> entry : deptStudents.entrySet()) {
                List<String> regNos = new ArrayList<>(entry.getValue());
                Collections.sort(regNos);

                String first = regNos.get(0);
                String last = regNos.get(regNos.size() - 1);
                int count = regNos.size();

                if (sb.length() > 0) sb.append("\n");
                if (count == 1) {
                    sb.append(first).append("(").append(count).append(")");
                } else {
                    sb.append(first).append("-").append(last).append("(").append(count).append(")");
                }
            }
            return sb.toString();
        }
    }
}

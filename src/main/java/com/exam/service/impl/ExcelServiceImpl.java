package com.exam.service.impl;

import com.exam.dto.PdfAllocationView;
import com.exam.dto.SummaryAllocationView;
import com.exam.entity.AllocationBatch;
import com.exam.entity.ExamSession;
import com.exam.service.ExcelService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExcelServiceImpl implements ExcelService {

    @Override
    public void generateBatchExcel(AllocationBatch batch, List<PdfAllocationView> allocations, OutputStream out) {
        ExamSession session = batch.getExamSession();

        Map<String, List<PdfAllocationView>> groupedByHall = allocations.stream()
                .sorted(Comparator.comparing(PdfAllocationView::hallName))
                .collect(Collectors.groupingBy(PdfAllocationView::hallName));

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Seating Arrangement");

            // Define Styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle boldStyle = createBoldStyle(workbook);
            CellStyle metaBoldStyle = createMetaBoldStyle(workbook);
            CellStyle gridHeaderStyle = createGridHeaderStyle(workbook);
            CellStyle gridDataStyle = createGridDataStyle(workbook);
            CellStyle hallNameStyle = createHallNameStyle(workbook);

            List<String> sortedHalls = groupedByHall.keySet().stream().sorted().toList();
            int currentRow = 0;

            for (String hallName : sortedHalls) {
                List<PdfAllocationView> hallAllocations = groupedByHall.get(hallName);

                // --- Hall Header ---
                Row row1 = sheet.createRow(currentRow++);
                createMergedCell(sheet, row1, 0, 9, "Office of the Controller of Examinations", headerStyle);

                Row row2 = sheet.createRow(currentRow++);
                createMergedCell(sheet, row2, 0, 9, resolveCollegeName().toUpperCase(), titleStyle);

                Row row3 = sheet.createRow(currentRow++);
                createMergedCell(sheet, row3, 0, 9, "Seating Arrangement", headerStyle);

                currentRow++; // Spacer

                // --- Metadata Line ---
                Row metaRow = sheet.createRow(currentRow++);
                Cell hallCell = metaRow.createCell(0);
                hallCell.setCellValue(hallName);
                hallCell.setCellStyle(hallNameStyle);
                sheet.addMergedRegion(new CellRangeAddress(currentRow - 1, currentRow + 1, 0, 2));

                String dateStr = session.getExamDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                Cell dateCell = metaRow.createCell(7);
                dateCell.setCellValue("Date: " + dateStr);
                dateCell.setCellStyle(metaBoldStyle);
                sheet.addMergedRegion(new CellRangeAddress(currentRow - 1, currentRow - 1, 7, 9));

                Row sessionRow = sheet.createRow(currentRow++);
                Cell sessionCell = sessionRow.createCell(7);
                sessionCell.setCellValue("Session: " + session.getSession());
                sessionCell.setCellStyle(metaBoldStyle);
                sheet.addMergedRegion(new CellRangeAddress(currentRow - 1, currentRow - 1, 7, 9));

                currentRow++; // Jump past the merged hall name box
                currentRow++; // Spacer

                // Determine dimensions dynamically from hallAllocations
                int maxRow = 5;
                int maxColIndex = 4; // default to V
                String[] engineColNames = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII"};
                String[] columnLabels = {"I Row", "II Row", "III Row", "IV Row", "V Row", "VI Row", "VII Row", "VIII Row"};

                for (PdfAllocationView a : hallAllocations) {
                    if (a.seatRow() > maxRow) maxRow = a.seatRow();
                    for (int i = 0; i < engineColNames.length; i++) {
                        if (engineColNames[i].equals(a.seatCol()) && i > maxColIndex) {
                            maxColIndex = i;
                        }
                    }
                }
                int colCount = maxColIndex + 1;

                // --- Grid Table (Dynamic Columns) ---
                Row gridLabelRow = sheet.createRow(currentRow++);
                createMergedCell(sheet, gridLabelRow, 0, (colCount * 2) - 1, "REGISTER NO. OF THE CANDIDATES", boldStyle);

                // Column Headers
                Row gridHeaderRow = sheet.createRow(currentRow++);

                for (int c = 0; c < colCount; c++) {
                    int colBase = c * 2;
                    Cell snoHeader = gridHeaderRow.createCell(colBase);
                    snoHeader.setCellValue("SNO");
                    snoHeader.setCellStyle(gridHeaderStyle);

                    final String colName = engineColNames[c];
                    String depts = hallAllocations.stream()
                            .filter(a -> colName.equals(a.seatCol()))
                            .map(a -> shortenDept(a.department(), a.registerNumber()))
                            .distinct()
                            .collect(Collectors.joining("/"));

                    Cell colHeader = gridHeaderRow.createCell(colBase + 1);
                    colHeader.setCellValue(columnLabels[c] + (depts.isEmpty() ? "" : " (" + depts + ")"));
                    colHeader.setCellStyle(gridHeaderStyle);
                }

                // Grid Body
                Map<String, Map<Integer, PdfAllocationView>> seatMap = hallAllocations.stream()
                        .collect(Collectors.groupingBy(PdfAllocationView::seatCol,
                                Collectors.toMap(PdfAllocationView::seatRow, a -> a, (first, second) -> first)));

                for (int r = 1; r <= maxRow; r++) {
                    Row dataRow = sheet.createRow(currentRow++);
                    for (int c = 0; c < colCount; c++) {
                        String colName = engineColNames[c];
                        int sno = (c * maxRow) + r;
                        int colBase = c * 2;

                        Cell snoCell = dataRow.createCell(colBase);
                        snoCell.setCellValue(sno);
                        snoCell.setCellStyle(gridDataStyle);

                        Cell regCell = dataRow.createCell(colBase + 1);
                        PdfAllocationView student = seatMap.getOrDefault(colName, Collections.emptyMap()).get(r);
                        regCell.setCellValue(student != null ? student.registerNumber() : "-");
                        regCell.setCellStyle(gridDataStyle);
                    }
                }

                currentRow++; // Small gap before signature area

                // --- Footer Signatures (SAME ROW, MASSIVE HEIGHT, EXTREME ENDS) ---
                Row footerRow = sheet.createRow(currentRow++);
                footerRow.setHeightInPoints(70); // MASSIVE SPACE FOR PEN SIGNATURE AND RUBBER STAMP

                CellStyle sigLeftStyle = createSignatureStyle(workbook, HorizontalAlignment.LEFT,
                        VerticalAlignment.BOTTOM);
                CellStyle sigRightStyle = createSignatureStyle(workbook, HorizontalAlignment.RIGHT,
                        VerticalAlignment.BOTTOM);

                Cell sigLeft = footerRow.createCell(0);
                sigLeft.setCellValue("Name and signature of the Hall superintendent");
                sigLeft.setCellStyle(sigLeftStyle);
                sheet.addMergedRegion(new CellRangeAddress(footerRow.getRowNum(), footerRow.getRowNum(), 0, 4));

                Cell sigRight = footerRow.createCell(5);
                sigRight.setCellValue("Signature of Chief Superintendent with college seal");
                sigRight.setCellStyle(sigRightStyle);
                sheet.addMergedRegion(new CellRangeAddress(footerRow.getRowNum(), footerRow.getRowNum(), 5, 9));

                currentRow++; // Bottom space
                Row separatorRow = sheet.createRow(currentRow++);
                createMergedCell(sheet, separatorRow, 0, 9,
                        "----------------------------------------------------------------------------------------------------------------------------------------------------------------",
                        null);
                currentRow++;
            }

            // Explicit column widths: S.No (1800) and Register No/Header (5200)
            // Avoids sheet.autoSizeColumn() native AWT font metrics which causes OOM crash on Render
            for (int c = 0; c < 8; c++) {
                sheet.setColumnWidth(c * 2, 1800);      // S.No
                sheet.setColumnWidth(c * 2 + 1, 5200);  // Register No / Dept
            }

            workbook.write(out);
        } catch (Exception e) {
            throw new RuntimeException("Excel Generation Failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void generateSummaryExcel(AllocationBatch batch, List<SummaryAllocationView> summaryData, OutputStream out) {
        ExamSession session = batch != null ? batch.getExamSession() : null;

        // 1. Pivot Data
        // Get unique subjects (Dept + Code)
        record SubjectKey(String dept, String code) implements Comparable<SubjectKey> {
            @Override
            public int compareTo(SubjectKey o) {
                int c = dept.compareTo(o.dept);
                return c != 0 ? c : code.compareTo(o.code);
            }
        }

        List<SubjectKey> subjects = summaryData.stream()
                .map(s -> {
                    String dept = (s.department() != null && !s.department().isBlank()) ? s.department().trim() : "N/A";
                    String code = resolveSubjectCode(s.subjectCode(), s.subjectName());
                    return new SubjectKey(dept, code);
                })
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        // Group by Hall
        Map<String, List<SummaryAllocationView>> hallGroups = summaryData.stream()
                .collect(Collectors.groupingBy(SummaryAllocationView::hallName));
        List<String> sortedHalls = hallGroups.keySet().stream().sorted().toList();

        // Calculate Totals
        Map<SubjectKey, Long> subjectTotals = summaryData.stream()
                .collect(Collectors.groupingBy(
                        s -> {
                            String dept = (s.department() != null && !s.department().isBlank()) ? s.department().trim() : "N/A";
                            String code = resolveSubjectCode(s.subjectCode(), s.subjectName());
                            return new SubjectKey(dept, code);
                        },
                        Collectors.summingLong(SummaryAllocationView::studentCount)
                ));
        long grandTotal = summaryData.stream().mapToLong(SummaryAllocationView::studentCount).sum();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Count Opening");

            // Styles
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle subTitleStyle = createBoldStyle(workbook);
            CellStyle gridHeaderStyle = createGridHeaderStyle(workbook);
            CellStyle gridDataStyle = createGridDataStyle(workbook);
            CellStyle boldStyle = createBoldStyle(workbook);
            boldStyle.setBorderBottom(BorderStyle.THIN);
            boldStyle.setBorderTop(BorderStyle.THIN);
            boldStyle.setBorderLeft(BorderStyle.THIN);
            boldStyle.setBorderRight(BorderStyle.THIN);

            int currentRow = 0;
            int totalCol = subjects.size() + 2;

            // --- HEADER SECTION ---
            Row row0 = sheet.createRow(currentRow++);
            createMergedCell(sheet, row0, 0, totalCol, resolveCollegeName().toUpperCase(), titleStyle);

            Row row1 = sheet.createRow(currentRow++);
            createMergedCell(sheet, row1, 0, totalCol, "(AUTONOMOUS)", subTitleStyle);

            Row row2 = sheet.createRow(currentRow++);
            createMergedCell(sheet, row2, 0, totalCol, "Count Opening", titleStyle);

            Row row3 = sheet.createRow(currentRow++);
            String sessionName = (session != null && session.getName() != null) ? session.getName().toUpperCase() : "SAMPLE";
            createMergedCell(sheet, row3, 0, totalCol, sessionName + " - EXAMINATIONS", subTitleStyle);

            Row row4 = sheet.createRow(currentRow++);
            String examDateStr = (session != null && session.getExamDate() != null)
                    ? session.getExamDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                    : java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            Cell dateLabel = row4.createCell(0);
            dateLabel.setCellValue("DATE: " + examDateStr);
            dateLabel.setCellStyle(subTitleStyle);

            String rawSessionType = (session != null && session.getSession() != null && !session.getSession().isBlank())
                    ? session.getSession().trim()
                    : "AN";
            String sessionTypeStr = rawSessionType.toUpperCase().startsWith("SESSION")
                    ? rawSessionType.toUpperCase()
                    : "SESSION: " + rawSessionType.toUpperCase();
            Cell sessLabel = row4.createCell(2);
            sessLabel.setCellValue(sessionTypeStr); // e.g. SESSION: FN / SESSION: AN
            sessLabel.setCellStyle(subTitleStyle);

            currentRow++; // Spacer

            // --- TABLE HEADER ROWS ---
            Row deptHeaderRow = sheet.createRow(currentRow++);
            Row codeHeaderRow = sheet.createRow(currentRow++);
            Row countHeaderRow = sheet.createRow(currentRow++);

            int rStart = deptHeaderRow.getRowNum();
            int rEnd = countHeaderRow.getRowNum();

            // SL NO Header (Merged vertically across 3 rows)
            Cell slNoH1 = deptHeaderRow.createCell(0);
            slNoH1.setCellValue("SL. NO.");
            slNoH1.setCellStyle(gridHeaderStyle);
            Cell slNoH2 = codeHeaderRow.createCell(0);
            slNoH2.setCellStyle(gridHeaderStyle);
            Cell slNoH3 = countHeaderRow.createCell(0);
            slNoH3.setCellStyle(gridHeaderStyle);
            sheet.addMergedRegion(new CellRangeAddress(rStart, rEnd, 0, 0));

            // HALL NO / CODE Header (Merged vertically across 3 rows)
            Cell hallH1 = deptHeaderRow.createCell(1);
            hallH1.setCellValue("HALL NO / CODE");
            hallH1.setCellStyle(gridHeaderStyle);
            Cell hallH2 = codeHeaderRow.createCell(1);
            hallH2.setCellStyle(gridHeaderStyle);
            Cell hallH3 = countHeaderRow.createCell(1);
            hallH3.setCellStyle(gridHeaderStyle);
            sheet.addMergedRegion(new CellRangeAddress(rStart, rEnd, 1, 1));

            // Build Department Header Row & Merge Same Depts (e.g. Electives)
            int colIndex = 2;
            while (colIndex < totalCol) {
                int startCol = colIndex;
                String currentDept = subjects.get(startCol - 2).dept();
                int endCol = startCol;
                while (endCol + 1 < totalCol && subjects.get(endCol + 1 - 2).dept().equals(currentDept)) {
                    endCol++;
                }

                for (int c = startCol; c <= endCol; c++) {
                    Cell deptCell = deptHeaderRow.createCell(c);
                    deptCell.setCellStyle(gridHeaderStyle);
                    if (c == startCol) {
                        deptCell.setCellValue(currentDept);
                    }
                }
                if (startCol < endCol) {
                    sheet.addMergedRegion(new CellRangeAddress(rStart, rStart, startCol, endCol));
                }
                colIndex = endCol + 1;
            }

            // Subject Code Row
            for (int i = 0; i < subjects.size(); i++) {
                SubjectKey sk = subjects.get(i);
                int col = i + 2;
                Cell codeCell = codeHeaderRow.createCell(col);
                codeCell.setCellValue(sk.code());
                codeCell.setCellStyle(gridHeaderStyle);
            }

            // Top Total Count Row
            for (int i = 0; i < subjects.size(); i++) {
                SubjectKey sk = subjects.get(i);
                int col = i + 2;
                Cell countCell = countHeaderRow.createCell(col);
                countCell.setCellValue(subjectTotals.get(sk));
                countCell.setCellStyle(boldStyle);
            }

            // TOTAL Header
            Cell totalH1 = deptHeaderRow.createCell(totalCol);
            totalH1.setCellValue("TOTAL");
            totalH1.setCellStyle(gridHeaderStyle);
            Cell totalH2 = codeHeaderRow.createCell(totalCol);
            totalH2.setCellStyle(gridHeaderStyle);
            sheet.addMergedRegion(new CellRangeAddress(rStart, rStart + 1, totalCol, totalCol));

            Cell grandTotalTop = countHeaderRow.createCell(totalCol);
            grandTotalTop.setCellValue(grandTotal);
            grandTotalTop.setCellStyle(boldStyle);

            // --- DATA ROWS ---
            int slNo = 1;
            for (String hallName : sortedHalls) {
                Row row = sheet.createRow(currentRow++);

                Cell cSl = row.createCell(0);
                cSl.setCellValue(slNo++);
                cSl.setCellStyle(gridDataStyle);

                Cell cHall = row.createCell(1);
                cHall.setCellValue(hallName);
                cHall.setCellStyle(gridDataStyle);

                long hallTotal = 0;
                List<SummaryAllocationView> hallData = hallGroups.get(hallName);
                Map<SubjectKey, Long> counts = hallData.stream().collect(Collectors.groupingBy(
                        s -> {
                            String dept = (s.department() != null && !s.department().isBlank()) ? s.department().trim() : "N/A";
                            String code = resolveSubjectCode(s.subjectCode(), s.subjectName());
                            return new SubjectKey(dept, code);
                        },
                        Collectors.summingLong(SummaryAllocationView::studentCount)
                ));

                for (int i = 0; i < subjects.size(); i++) {
                    SubjectKey sk = subjects.get(i);
                    Long count = counts.get(sk);
                    Cell cCount = row.createCell(i + 2);
                    if (count != null && count > 0) {
                        cCount.setCellValue(count);
                        hallTotal += count;
                    }
                    cCount.setCellStyle(gridDataStyle);
                }

                Cell cTotal = row.createCell(totalCol);
                cTotal.setCellValue(hallTotal);
                cTotal.setCellStyle(boldStyle);
            }

            // --- FOOTER ROW ---
            Row footerRow = sheet.createRow(currentRow++);
            Cell grandTotalLabel = footerRow.createCell(0);
            grandTotalLabel.setCellValue("Grand TOTAL");
            grandTotalLabel.setCellStyle(gridHeaderStyle);

            Cell emptyCol1 = footerRow.createCell(1);
            emptyCol1.setCellStyle(gridHeaderStyle);
            sheet.addMergedRegion(new CellRangeAddress(footerRow.getRowNum(), footerRow.getRowNum(), 0, 1));

            for (int i = 0; i < subjects.size(); i++) {
                SubjectKey sk = subjects.get(i);
                Cell c = footerRow.createCell(i + 2);
                c.setCellValue(subjectTotals.get(sk));
                c.setCellStyle(gridHeaderStyle);
            }

            Cell cFinalTotal = footerRow.createCell(totalCol);
            cFinalTotal.setCellValue(grandTotal);
            cFinalTotal.setCellStyle(gridHeaderStyle);

            currentRow += 2;

            // --- SIGNATURE AREA ---
            Row sigRow = sheet.createRow(currentRow++);
            sigRow.setHeightInPoints(40);
            int sigStartCol = Math.max(0, totalCol - 2);
            Cell sigCell = sigRow.createCell(sigStartCol);
            sigCell.setCellValue("Signature of Chief Superintendent");
            sigCell.setCellStyle(createSignatureStyle(workbook, HorizontalAlignment.RIGHT, VerticalAlignment.BOTTOM));
            sheet.addMergedRegion(new CellRangeAddress(sigRow.getRowNum(), sigRow.getRowNum(), sigStartCol, totalCol));

            // Explicit column widths avoiding sheet.autoSizeColumn() AWT font crash
            sheet.setColumnWidth(0, 3600); // Hall
            sheet.setColumnWidth(1, 6000); // Department
            for (int i = 2; i < totalCol; i++) {
                sheet.setColumnWidth(i, 3600); // Subject Code counts
            }
            sheet.setColumnWidth(totalCol, 3200); // Grand Total

            workbook.write(out);
        } catch (Exception e) {
            throw new RuntimeException("Matrix Excel Generation Failed: " + e.getMessage(), e);
        }
    }

    private void createMergedCell(Sheet sheet, Row row, int startCol, int endCol, String value, CellStyle style) {
        Cell cell = row.createCell(startCol);
        cell.setCellValue(value);
        if (style != null)
            cell.setCellStyle(style);
        sheet.addMergedRegion(new CellRangeAddress(row.getRowNum(), row.getRowNum(), startCol, endCol));
    }

    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontName("Times New Roman");
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createTitleStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontName("Times New Roman");
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createHallNameStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontName("Times New Roman");
        font.setBold(true);
        font.setFontHeightInPoints((short) 24);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.MEDIUM);
        style.setBorderTop(BorderStyle.MEDIUM);
        style.setBorderLeft(BorderStyle.MEDIUM);
        style.setBorderRight(BorderStyle.MEDIUM);
        return style;
    }

    private CellStyle createBoldStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontName("Times New Roman");
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createMetaBoldStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontName("Times New Roman");
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    private CellStyle createGridHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = wb.createFont();
        font.setFontName("Times New Roman");
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle createGridDataStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        Font font = wb.createFont();
        font.setFontName("Times New Roman");
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        return style;
    }

    private CellStyle createSignatureStyle(Workbook wb, HorizontalAlignment align, VerticalAlignment vAlign) {
        CellStyle style = wb.createCellStyle();
        style.setAlignment(align);
        style.setVerticalAlignment(vAlign);
        style.setWrapText(false);
        Font font = wb.createFont();
        font.setFontName("Times New Roman");
        font.setBold(true);
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        return style;
    }

    private String shortenDept(String dept, String regNo) {
        if (regNo == null || regNo.length() < 9) return dept != null ? dept : "N/A";
        
        String upperReg = regNo.toUpperCase();
        char degreeType = upperReg.charAt(4);
        String code = upperReg.substring(7, 9);
        
        if (degreeType == 'P') {
            return switch (code) {
                case "PS" -> "ME(PSE)";
                case "ED" -> "ME(ED)";
                case "CM" -> "ME(CM)";
                case "CS" -> "ME(CSE)";
                case "MB" -> "MBA";
                default -> "ME(" + code + ")";
            };
        }
        
        return switch (code) {
            case "CS" -> "CSE";
            case "CB" -> "CSBS";
            case "EC" -> "ECE";
            case "ME" -> "MECH";
            case "EE" -> "EEE";
            case "MB" -> "MBA";
            case "AD" -> "AIDS";
            case "AM" -> "AIML";
            case "IT" -> "IT";
            default -> dept != null ? dept : code;
        };
    }

    private String resolveCollegeName() {
        String tenantId = com.exam.config.tenant.TenantContext.getCurrentTenant();
        if (tenantId != null) {
            String cachedName = com.exam.config.tenant.TenantContext.getCollegeName(tenantId);
            if (cachedName != null && !cachedName.trim().isEmpty()) {
                return cachedName;
            }
        }
        return com.exam.claims.util.ClaimConstants.COLLEGE_NAME;
    }

    private String resolveSubjectCode(String subjectCode, String subjectName) {
        if (subjectCode != null && !subjectCode.isBlank() && !"N/A".equalsIgnoreCase(subjectCode.trim())) {
            return subjectCode.replaceAll("\\s*\\(.*?\\)", "").trim();
        }
        if (subjectName != null && !subjectName.isBlank()) {
            String trimmedName = subjectName.trim();
            // Try extracting code pattern like CS3501, AD3351, EC8551, GE3151, MA3151 etc.
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\b([A-Za-z]{2,6}\\s*[0-9]{3,5}[A-Za-z0-9-]*)\\b");
            java.util.regex.Matcher m = p.matcher(trimmedName);
            if (m.find()) {
                return m.group(1).replaceAll("\\s+", "").toUpperCase();
            }
            if (trimmedName.contains("-")) {
                String[] parts = trimmedName.split("-");
                for (String part : parts) {
                    java.util.regex.Matcher m2 = p.matcher(part.trim());
                    if (m2.find()) {
                        return m2.group(1).replaceAll("\\s+", "").toUpperCase();
                    }
                }
            }
            return trimmedName;
        }
        return "N/A";
    }
}

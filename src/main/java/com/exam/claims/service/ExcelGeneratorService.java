package com.exam.claims.service;

import com.exam.claims.entity.ClaimRecord;
import com.exam.claims.util.ClaimConstants;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExcelGeneratorService {

    /**
     * CONSOLIDATED SHEET 1: Detailed Valuation Examiner Claim Details
     */
    public byte[] generateBatchExcel1(List<ClaimRecord> records) throws IOException {
        if (records == null || records.isEmpty()) {
            return new byte[0];
        }

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Consolidated Claims 1");
            sheet.setDisplayGridlines(true);

            // Define Fonts
            Font fontRegular = workbook.createFont();
            fontRegular.setFontName("Times New Roman");
            fontRegular.setFontHeightInPoints((short) 11);

            Font fontBold = workbook.createFont();
            fontBold.setFontName("Times New Roman");
            fontBold.setFontHeightInPoints((short) 11);
            fontBold.setBold(true);

            Font fontTitle = workbook.createFont();
            fontTitle.setFontName("Times New Roman");
            fontTitle.setFontHeightInPoints((short) 14);
            fontTitle.setBold(true);

            Font fontCollege = workbook.createFont();
            fontCollege.setFontName("Times New Roman");
            fontCollege.setFontHeightInPoints((short) 16);
            fontCollege.setBold(true);

            // Define Cell Styles
            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(fontTitle);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle collegeStyle = workbook.createCellStyle();
            collegeStyle.setFont(fontCollege);
            collegeStyle.setAlignment(HorizontalAlignment.CENTER);
            collegeStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle subtitleStyle = workbook.createCellStyle();
            Font fontSubtitle = workbook.createFont();
            fontSubtitle.setFontName("Times New Roman");
            fontSubtitle.setFontHeightInPoints((short) 12);
            fontSubtitle.setBold(false);
            subtitleStyle.setFont(fontSubtitle);
            subtitleStyle.setAlignment(HorizontalAlignment.CENTER);
            subtitleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle sectionHeaderStyle = workbook.createCellStyle();
            Font fontSecHeader = workbook.createFont();
            fontSecHeader.setFontName("Times New Roman");
            fontSecHeader.setFontHeightInPoints((short) 12);
            fontSecHeader.setBold(true);
            fontSecHeader.setUnderline(Font.U_SINGLE);
            sectionHeaderStyle.setFont(fontSecHeader);
            sectionHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
            sectionHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle dateLabelStyle = workbook.createCellStyle();
            Font fontItalic = workbook.createFont();
            fontItalic.setFontName("Times New Roman");
            fontItalic.setFontHeightInPoints((short) 11);
            fontItalic.setItalic(true);
            fontItalic.setBold(true);
            dateLabelStyle.setFont(fontItalic);
            dateLabelStyle.setAlignment(HorizontalAlignment.LEFT);

            CellStyle dateValStyle = workbook.createCellStyle();
            dateValStyle.setFont(fontBold);
            dateValStyle.setAlignment(HorizontalAlignment.LEFT);

            // Table Header Styles
            CellStyle thStyle = workbook.createCellStyle();
            thStyle.setFont(fontBold);
            thStyle.setAlignment(HorizontalAlignment.CENTER);
            thStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            thStyle.setWrapText(true);
            setBorders(thStyle, BorderStyle.THIN);

            // Data Styles
            CellStyle cellLeft = workbook.createCellStyle();
            cellLeft.setFont(fontRegular);
            cellLeft.setAlignment(HorizontalAlignment.LEFT);
            cellLeft.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(cellLeft, BorderStyle.THIN);

            CellStyle cellCenter = workbook.createCellStyle();
            cellCenter.setFont(fontRegular);
            cellCenter.setAlignment(HorizontalAlignment.CENTER);
            cellCenter.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(cellCenter, BorderStyle.THIN);

            CellStyle cellCenterBold = workbook.createCellStyle();
            cellCenterBold.setFont(fontBold);
            cellCenterBold.setAlignment(HorizontalAlignment.CENTER);
            cellCenterBold.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(cellCenterBold, BorderStyle.THIN);

            // Number Formats
            DataFormat format = workbook.createDataFormat();
            short numFormatIndex = format.getFormat("#,##0.00");

            CellStyle cellNumber = workbook.createCellStyle();
            cellNumber.setFont(fontRegular);
            cellNumber.setAlignment(HorizontalAlignment.RIGHT);
            cellNumber.setVerticalAlignment(VerticalAlignment.CENTER);
            cellNumber.setDataFormat(numFormatIndex);
            setBorders(cellNumber, BorderStyle.THIN);

            CellStyle cellNumberBold = workbook.createCellStyle();
            cellNumberBold.setFont(fontBold);
            cellNumberBold.setAlignment(HorizontalAlignment.RIGHT);
            cellNumberBold.setVerticalAlignment(VerticalAlignment.CENTER);
            cellNumberBold.setDataFormat(numFormatIndex);
            setBorders(cellNumberBold, BorderStyle.THIN);

            // Total row style
            CellStyle totalRowLabelStyle = workbook.createCellStyle();
            totalRowLabelStyle.setFont(fontBold);
            totalRowLabelStyle.setAlignment(HorizontalAlignment.RIGHT);
            totalRowLabelStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(totalRowLabelStyle, BorderStyle.THIN);

            // 1. Office of the Controller of Examinations
            Row r1 = sheet.createRow(0);
            r1.setHeightInPoints(24);
            Cell c1 = r1.createCell(0);
            c1.setCellValue("Office of the Controller of Examinations");
            c1.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 12));

            // 2. K. RAMAKRISHNAN COLLEGE OF ENGINEERING
            Row r2 = sheet.createRow(1);
            r2.setHeightInPoints(26);
            Cell c2 = r2.createCell(0);
            c2.setCellValue(ClaimConstants.COLLEGE_NAME);
            c2.setCellStyle(collegeStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 12));

            // 3. (Autonomous)
            Row r3 = sheet.createRow(2);
            r3.setHeightInPoints(20);
            Cell c3 = r3.createCell(0);
            c3.setCellValue(ClaimConstants.COLLEGE_AUTONOMY);
            c3.setCellStyle(subtitleStyle);
            sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, 12));

            // 4. VALUATION EXAMINER CLAIM DETAILS
            Row r4 = sheet.createRow(3);
            r4.setHeightInPoints(22);
            Cell c4 = r4.createCell(0);
            c4.setCellValue("VALUATION EXAMINER CLAIM DETAILS");
            c4.setCellStyle(sectionHeaderStyle);
            sheet.addMergedRegion(new CellRangeAddress(3, 3, 0, 12));

            // 5. Season (E.g. NOVEMBER / DECEMBER 2025)
            Row r5 = sheet.createRow(4);
            r5.setHeightInPoints(22);
            Cell c5 = r5.createCell(0);
            String season = records.get(0).getExamSeason() != null ? records.get(0).getExamSeason().toUpperCase() : "MAY 2026";
            c5.setCellValue(season + " EXAMINATIONS");
            c5.setCellStyle(sectionHeaderStyle);
            sheet.addMergedRegion(new CellRangeAddress(4, 4, 0, 12));

            // Space
            sheet.createRow(5).setHeightInPoints(10);

            // 6. Date of Valuation: 20-05-2026
            Row r7 = sheet.createRow(6);
            r7.setHeightInPoints(20);
            Cell c7Lbl = r7.createCell(0);
            c7Lbl.setCellValue("Date of Valuation:");
            c7Lbl.setCellStyle(dateLabelStyle);

            Cell c7Val = r7.createCell(1);
            String valDate = records.get(0).getValuationDate() != null
                    ? records.get(0).getValuationDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
                    : "";
            c7Val.setCellValue(valDate);
            c7Val.setCellStyle(dateValStyle);

            // Space
            sheet.createRow(7).setHeightInPoints(10);

            // Table Headers (Rows 8 and 9)
            Row h1 = sheet.createRow(8);
            h1.setHeightInPoints(28);
            Row h2 = sheet.createRow(9);
            h2.setHeightInPoints(28);

            // Header Names and Ranges
            createHeaderCell(sheet, h1, h2, 0, "S. NO.", thStyle);
            createHeaderCell(sheet, h1, h2, 1, "POSSITION", thStyle);
            createHeaderCell(sheet, h1, h2, 2, "BOARD NAME", thStyle);

            // STAFF NAME (Spans D8:E8)
            Cell staffNameCell = h1.createCell(3);
            staffNameCell.setCellValue("STAFF NAME");
            staffNameCell.setCellStyle(thStyle);
            h1.createCell(4).setCellStyle(thStyle); // empty cell for merge
            sheet.addMergedRegion(new CellRangeAddress(8, 8, 3, 4));
            
            // Sub headers under STAFF NAME (empty cells to look unified)
            Cell titleHeader = h2.createCell(3);
            titleHeader.setCellValue("");
            titleHeader.setCellStyle(thStyle);
            Cell nameHeader = h2.createCell(4);
            nameHeader.setCellValue("");
            nameHeader.setCellStyle(thStyle);

            createHeaderCell(sheet, h1, h2, 5, "TOTAL NO. OF SCRIPTS VALUED", thStyle);
            createHeaderCell(sheet, h1, h2, 6, "VALUATION AMOUNT", thStyle);
            createHeaderCell(sheet, h1, h2, 7, "LUMPSUM AMOUNT", thStyle);
            createHeaderCell(sheet, h1, h2, 8, "10% OF MAX SCRIPT AMOUNT\n(for Chief Examiner only)", thStyle);
            createHeaderCell(sheet, h1, h2, 9, "TOTAL AMOUNT", thStyle);

            // ACCOUNT DETAILS (Spans K8:M8)
            Cell acDetailsCell = h1.createCell(10);
            acDetailsCell.setCellValue("ACCOUNT DETAILS");
            acDetailsCell.setCellStyle(thStyle);
            h1.createCell(11).setCellStyle(thStyle);
            h1.createCell(12).setCellStyle(thStyle);
            sheet.addMergedRegion(new CellRangeAddress(8, 8, 10, 12));

            Cell acNoCell = h2.createCell(10);
            acNoCell.setCellValue("ACCOUNT NO.");
            acNoCell.setCellStyle(thStyle);

            Cell ifscCell = h2.createCell(11);
            ifscCell.setCellValue("IFSC CODE");
            ifscCell.setCellStyle(thStyle);

            Cell bankCell = h2.createCell(12);
            bankCell.setCellValue("BANK NAME");
            bankCell.setCellStyle(thStyle);

            // Data Rows (Start from row 10)
            int rowIdx = 10;
            int sNo = 1;

            int totalScriptsSum = 0;
            BigDecimal totalValuationSum = BigDecimal.ZERO;
            BigDecimal totalLumpsumSum = BigDecimal.ZERO;
            BigDecimal totalChiefBonusSum = BigDecimal.ZERO;
            BigDecimal totalAmountSum = BigDecimal.ZERO;

            for (ClaimRecord record : records) {
                Row row = sheet.createRow(rowIdx++);
                row.setHeightInPoints(24);

                // S. NO.
                Cell cellSNo = row.createCell(0);
                cellSNo.setCellValue(sNo++);
                cellSNo.setCellStyle(cellCenterBold);

                // POSSITION (with double S to exactly match user's image spelling)
                Cell cellPos = row.createCell(1);
                cellPos.setCellValue(record.getPostHeld() != null ? record.getPostHeld().toUpperCase() : "");
                cellPos.setCellStyle(cellCenter);

                // BOARD NAME
                Cell cellBoard = row.createCell(2);
                cellBoard.setCellValue(record.getBoardName() != null ? record.getBoardName().toUpperCase() : "");
                cellBoard.setCellStyle(cellCenter);

                // Title
                Cell cellTitle = row.createCell(3);
                cellTitle.setCellValue(record.getNameTitle() != null ? record.getNameTitle().trim() : "");
                cellTitle.setCellStyle(cellCenter);

                // Staff Name
                Cell cellName = row.createCell(4);
                cellName.setCellValue(record.getStaffName() != null ? record.getStaffName().toUpperCase() : "");
                cellName.setCellStyle(cellLeft);

                // Calculations
                boolean isAssistant = record.isAssistantExaminer();
                Cell cellScripts = row.createCell(5);
                Cell cellValuation = row.createCell(6);
                Cell cellLumpsum = row.createCell(7);
                Cell cellChiefBonus = row.createCell(8);
                Cell cellTotal = row.createCell(9);
                
                BigDecimal total = record.getTotalAmount() != null ? record.getTotalAmount() : BigDecimal.ZERO;

                if (isAssistant) {
                    setDashOrValueInt(cellScripts, 0, cellCenter);
                    setDashOrValue(cellValuation, BigDecimal.ZERO, cellNumber);
                    setDashOrValue(cellLumpsum, total, cellNumber);
                    totalLumpsumSum = totalLumpsumSum.add(total);
                    setDashOrValue(cellChiefBonus, BigDecimal.ZERO, cellNumber);
                } else {
                    setDashOrValueInt(cellScripts, record.getTotalScripts(), cellCenter);
                    totalScriptsSum += (record.getTotalScripts() != null ? record.getTotalScripts() : 0);

                    setDashOrValue(cellValuation, record.getScriptAmount(), cellNumber);
                    totalValuationSum = totalValuationSum.add(record.getScriptAmount() != null ? record.getScriptAmount() : BigDecimal.ZERO);

                    BigDecimal ta = record.getTravellingAllowance() != null ? record.getTravellingAllowance() : BigDecimal.ZERO;
                    BigDecimal da = record.getDearnessAllowance() != null ? record.getDearnessAllowance() : BigDecimal.ZERO;
                    BigDecimal lumpsum = ta.add(da);
                    setDashOrValue(cellLumpsum, lumpsum, cellNumber);
                    totalLumpsumSum = totalLumpsumSum.add(lumpsum);

                    if (record.isChiefExaminer()) {
                        setDashOrValue(cellChiefBonus, record.getTenPercentAmount(), cellNumber);
                        totalChiefBonusSum = totalChiefBonusSum.add(record.getTenPercentAmount() != null ? record.getTenPercentAmount() : BigDecimal.ZERO);
                    } else {
                        setDashOrValue(cellChiefBonus, BigDecimal.ZERO, cellNumber);
                    }
                }

                // Total Amount
                cellTotal.setCellValue(total.doubleValue());
                cellTotal.setCellStyle(cellNumber);
                totalAmountSum = totalAmountSum.add(total);

                // Account No (Write as String text explicitly to prevent formatting error)
                Cell cellAcNo = row.createCell(10);
                cellAcNo.setCellValue(record.getBankAccountNumber() != null ? record.getBankAccountNumber().trim() : "");
                cellAcNo.setCellStyle(cellCenter);

                // IFSC Code
                Cell cellIfsc = row.createCell(11);
                cellIfsc.setCellValue(record.getIfscCode() != null ? record.getIfscCode().trim().toUpperCase() : "");
                cellIfsc.setCellStyle(cellCenter);

                // Bank Name
                Cell cellBank = row.createCell(12);
                cellBank.setCellValue(record.getBankName() != null ? record.getBankName().toUpperCase() : "");
                cellBank.setCellStyle(cellLeft);
            }

            // Add Total Row at the bottom
            Row totalRow = sheet.createRow(rowIdx);
            totalRow.setHeightInPoints(26);

            for (int i = 0; i <= 12; i++) {
                Cell c = totalRow.createCell(i);
                setBorders(c.getCellStyle(), BorderStyle.THIN);
                c.setCellStyle(cellCenterBold);
            }

            Cell totalLabel = totalRow.getCell(4);
            totalLabel.setCellValue("TOTAL");
            totalLabel.setCellStyle(totalRowLabelStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 4));

            Cell totalScriptsCell = totalRow.getCell(5);
            totalScriptsCell.setCellValue(totalScriptsSum);
            totalScriptsCell.setCellStyle(cellCenterBold);

            Cell totalValuationCell = totalRow.getCell(6);
            totalValuationCell.setCellValue(totalValuationSum.doubleValue());
            totalValuationCell.setCellStyle(cellNumberBold);

            Cell totalLumpsumCell = totalRow.getCell(7);
            totalLumpsumCell.setCellValue(totalLumpsumSum.doubleValue());
            totalLumpsumCell.setCellStyle(cellNumberBold);

            Cell totalChiefBonusCell = totalRow.getCell(8);
            totalChiefBonusCell.setCellValue(totalChiefBonusSum.doubleValue());
            totalChiefBonusCell.setCellStyle(cellNumberBold);

            Cell totalAmountCell = totalRow.getCell(9);
            totalAmountCell.setCellValue(totalAmountSum.doubleValue());
            totalAmountCell.setCellStyle(cellNumberBold);

            // Empty slots in account columns of the total row should have dashes
            totalRow.getCell(10).setCellValue("-");
            totalRow.getCell(11).setCellValue("-");
            totalRow.getCell(12).setCellValue("-");

            // Setup column auto-widths for production alignment
            sheet.setColumnWidth(0, 2400);  // S. No
            sheet.setColumnWidth(1, 4800);  // POSSITION
            sheet.setColumnWidth(2, 3200);  // BOARD
            sheet.setColumnWidth(3, 1400);  // Title
            sheet.setColumnWidth(4, 7200);  // Staff Name
            sheet.setColumnWidth(5, 3400);  // Total scripts
            sheet.setColumnWidth(6, 4200);  // Valuation Amount
            sheet.setColumnWidth(7, 4200);  // Lumpsum Amount
            sheet.setColumnWidth(8, 5400);  // 10% Chief Bonus
            sheet.setColumnWidth(9, 4400);  // Total Amount
            sheet.setColumnWidth(10, 5200); // Account No
            sheet.setColumnWidth(11, 4000); // IFSC Code
            sheet.setColumnWidth(12, 6000); // Bank Name

            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * CONSOLIDATED SHEET 2: Reduced Fields "END SEMESTER VALUATION CLAIM"
     */
    public byte[] generateBatchExcel2(List<ClaimRecord> records) throws IOException {
        if (records == null || records.isEmpty()) {
            return new byte[0];
        }

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Consolidated Claims 2");
            sheet.setDisplayGridlines(true);

            // Define Fonts
            Font fontRegular = workbook.createFont();
            fontRegular.setFontName("Times New Roman");
            fontRegular.setFontHeightInPoints((short) 11);

            Font fontBold = workbook.createFont();
            fontBold.setFontName("Times New Roman");
            fontBold.setFontHeightInPoints((short) 11);
            fontBold.setBold(true);

            Font fontTitle = workbook.createFont();
            fontTitle.setFontName("Times New Roman");
            fontTitle.setFontHeightInPoints((short) 14);
            fontTitle.setBold(true);

            Font fontCollege = workbook.createFont();
            fontCollege.setFontName("Times New Roman");
            fontCollege.setFontHeightInPoints((short) 16);
            fontCollege.setBold(true);

            // Define Cell Styles
            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(fontTitle);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle collegeStyle = workbook.createCellStyle();
            collegeStyle.setFont(fontCollege);
            collegeStyle.setAlignment(HorizontalAlignment.CENTER);
            collegeStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle subtitleStyle = workbook.createCellStyle();
            Font fontSubtitle = workbook.createFont();
            fontSubtitle.setFontName("Times New Roman");
            fontSubtitle.setFontHeightInPoints((short) 12);
            fontSubtitle.setBold(false);
            subtitleStyle.setFont(fontSubtitle);
            subtitleStyle.setAlignment(HorizontalAlignment.CENTER);
            subtitleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle sectionHeaderStyle = workbook.createCellStyle();
            Font fontSecHeader = workbook.createFont();
            fontSecHeader.setFontName("Times New Roman");
            fontSecHeader.setFontHeightInPoints((short) 12);
            fontSecHeader.setBold(true);
            fontSecHeader.setUnderline(Font.U_SINGLE);
            sectionHeaderStyle.setFont(fontSecHeader);
            sectionHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
            sectionHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle dateLabelStyle = workbook.createCellStyle();
            Font fontItalic = workbook.createFont();
            fontItalic.setFontName("Times New Roman");
            fontItalic.setFontHeightInPoints((short) 11);
            fontItalic.setItalic(true);
            fontItalic.setBold(true);
            dateLabelStyle.setFont(fontItalic);
            dateLabelStyle.setAlignment(HorizontalAlignment.LEFT);

            CellStyle dateValStyle = workbook.createCellStyle();
            dateValStyle.setFont(fontBold);
            dateValStyle.setAlignment(HorizontalAlignment.LEFT);

            // Table Header Styles
            CellStyle thStyle = workbook.createCellStyle();
            thStyle.setFont(fontBold);
            thStyle.setAlignment(HorizontalAlignment.CENTER);
            thStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            thStyle.setWrapText(true);
            setBorders(thStyle, BorderStyle.THIN);

            // Data Styles
            CellStyle cellLeft = workbook.createCellStyle();
            cellLeft.setFont(fontRegular);
            cellLeft.setAlignment(HorizontalAlignment.LEFT);
            cellLeft.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(cellLeft, BorderStyle.THIN);

            CellStyle cellCenter = workbook.createCellStyle();
            cellCenter.setFont(fontRegular);
            cellCenter.setAlignment(HorizontalAlignment.CENTER);
            cellCenter.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(cellCenter, BorderStyle.THIN);

            CellStyle cellCenterBold = workbook.createCellStyle();
            cellCenterBold.setFont(fontBold);
            cellCenterBold.setAlignment(HorizontalAlignment.CENTER);
            cellCenterBold.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(cellCenterBold, BorderStyle.THIN);

            // Number Formats
            DataFormat format = workbook.createDataFormat();
            short numFormatIndex = format.getFormat("#,##0.00");

            CellStyle cellNumberBold = workbook.createCellStyle();
            cellNumberBold.setFont(fontBold);
            cellNumberBold.setAlignment(HorizontalAlignment.RIGHT);
            cellNumberBold.setVerticalAlignment(VerticalAlignment.CENTER);
            cellNumberBold.setDataFormat(numFormatIndex);
            setBorders(cellNumberBold, BorderStyle.THIN);

            CellStyle cellNumberRegular = workbook.createCellStyle();
            cellNumberRegular.setFont(fontRegular);
            cellNumberRegular.setAlignment(HorizontalAlignment.RIGHT);
            cellNumberRegular.setVerticalAlignment(VerticalAlignment.CENTER);
            cellNumberRegular.setDataFormat(numFormatIndex);
            setBorders(cellNumberRegular, BorderStyle.THIN);

            // Total row style
            CellStyle totalRowLabelStyle = workbook.createCellStyle();
            totalRowLabelStyle.setFont(fontBold);
            totalRowLabelStyle.setAlignment(HorizontalAlignment.RIGHT);
            totalRowLabelStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(totalRowLabelStyle, BorderStyle.THIN);

            // 1. Office of the Controller of Examinations
            Row r1 = sheet.createRow(0);
            r1.setHeightInPoints(24);
            Cell c1 = r1.createCell(0);
            c1.setCellValue("Office of the Controller of Examinations");
            c1.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));

            // 2. K.Ramakrishnan College of Engineering (Mixed Case like IMG 1)
            Row r2 = sheet.createRow(1);
            r2.setHeightInPoints(26);
            Cell c2 = r2.createCell(0);
            c2.setCellValue("K.Ramakrishnan College of Engineering");
            c2.setCellStyle(collegeStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 6));

            // 3. (AUTONOMOUS)
            Row r3 = sheet.createRow(2);
            r3.setHeightInPoints(20);
            Cell c3 = r3.createCell(0);
            c3.setCellValue("(AUTONOMOUS)");
            c3.setCellStyle(subtitleStyle);
            sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, 6));

            // 4. END SEMESTER VALUATION CLAIM (IMG 1 Title)
            Row r4 = sheet.createRow(3);
            r4.setHeightInPoints(22);
            Cell c4 = r4.createCell(0);
            c4.setCellValue("END SEMESTER VALUATION CLAIM");
            c4.setCellStyle(sectionHeaderStyle);
            sheet.addMergedRegion(new CellRangeAddress(3, 3, 0, 6));

            // 5. Season (E.g. APRIL / MAY - 2026)
            Row r5 = sheet.createRow(4);
            r5.setHeightInPoints(22);
            Cell c5 = r5.createCell(0);
            String season = records.get(0).getExamSeason() != null ? records.get(0).getExamSeason().toUpperCase() : "MAY 2026";
            c5.setCellValue(season + " EXAMINATIONS");
            c5.setCellStyle(sectionHeaderStyle);
            sheet.addMergedRegion(new CellRangeAddress(4, 4, 0, 6));

            // Space
            sheet.createRow(5).setHeightInPoints(10);

            // 6. DATE : 20.05.2026 (Dots format)
            Row r7 = sheet.createRow(6);
            r7.setHeightInPoints(20);
            Cell c7Val = r7.createCell(0);
            String valDate = records.get(0).getValuationDate() != null
                    ? records.get(0).getValuationDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                    : "";
            c7Val.setCellValue("DATE : " + valDate);
            c7Val.setCellStyle(dateValStyle);
            sheet.addMergedRegion(new CellRangeAddress(6, 6, 0, 2));

            // Space
            sheet.createRow(7).setHeightInPoints(10);

            // Table Headers (Row 8)
            Row h = sheet.createRow(8);
            h.setHeightInPoints(28);

            String[] headers = {
                "S.NO",
                "EXAMINER NAME",
                "AMOUNT",
                "BANK NAME",
                "BRANCH",
                "IFSC CODE",
                "ACCOUNT NUMBER"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = h.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(thStyle);
            }

            // Data Rows (Start from row 9)
            int rowIdx = 9;
            int sNo = 1;
            BigDecimal totalAmountSum = BigDecimal.ZERO;

            for (ClaimRecord record : records) {
                Row row = sheet.createRow(rowIdx++);
                row.setHeightInPoints(24);

                // S.NO
                Cell cellSNo = row.createCell(0);
                cellSNo.setCellValue(sNo++);
                cellSNo.setCellStyle(cellCenterBold);

                // EXAMINER NAME (Combined Title and Name)
                Cell cellName = row.createCell(1);
                String title = record.getNameTitle() != null ? record.getNameTitle().trim() + " " : "";
                String name = record.getStaffName() != null ? record.getStaffName().toUpperCase() : "";
                cellName.setCellValue(title + name);
                cellName.setCellStyle(cellLeft);

                // AMOUNT (Bolded in the image!)
                Cell cellAmt = row.createCell(2);
                BigDecimal total = record.getTotalAmount() != null ? record.getTotalAmount() : BigDecimal.ZERO;
                cellAmt.setCellValue(total.doubleValue());
                cellAmt.setCellStyle(cellNumberBold);
                totalAmountSum = totalAmountSum.add(total);

                // BANK NAME
                Cell cellBank = row.createCell(3);
                cellBank.setCellValue(record.getBankName() != null ? record.getBankName().toUpperCase() : "");
                cellBank.setCellStyle(cellLeft);

                // BRANCH
                Cell cellBranch = row.createCell(4);
                cellBranch.setCellValue(record.getBranch() != null ? record.getBranch().toUpperCase() : "");
                cellBranch.setCellStyle(cellLeft);

                // IFSC CODE
                Cell cellIfsc = row.createCell(5);
                cellIfsc.setCellValue(record.getIfscCode() != null ? record.getIfscCode().trim().toUpperCase() : "");
                cellIfsc.setCellStyle(cellCenter);

                // ACCOUNT NUMBER
                Cell cellAc = row.createCell(6);
                cellAc.setCellValue(record.getBankAccountNumber() != null ? record.getBankAccountNumber().trim() : "");
                cellAc.setCellStyle(cellCenter);
            }

            // Add Total Row at the bottom
            Row totalRow = sheet.createRow(rowIdx);
            totalRow.setHeightInPoints(26);

            for (int i = 0; i <= 6; i++) {
                Cell c = totalRow.createCell(i);
                setBorders(c.getCellStyle(), BorderStyle.THIN);
                c.setCellStyle(cellCenterBold);
            }

            Cell totalLabel = totalRow.getCell(1);
            totalLabel.setCellValue("TOTAL");
            totalLabel.setCellStyle(totalRowLabelStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 1));

            Cell totalAmountCell = totalRow.getCell(2);
            totalAmountCell.setCellValue(totalAmountSum.doubleValue());
            totalAmountCell.setCellStyle(cellNumberBold);

            // Empty fields in total row
            totalRow.getCell(3).setCellValue("-");
            totalRow.getCell(4).setCellValue("-");
            totalRow.getCell(5).setCellValue("-");
            totalRow.getCell(6).setCellValue("-");

            // Setup column auto-widths for production alignment
            sheet.setColumnWidth(0, 2400);  // S.NO
            sheet.setColumnWidth(1, 8000);  // EXAMINER NAME
            sheet.setColumnWidth(2, 4400);  // AMOUNT
            sheet.setColumnWidth(3, 6400);  // BANK NAME
            sheet.setColumnWidth(4, 5600);  // BRANCH
            sheet.setColumnWidth(5, 4200);  // IFSC CODE
            sheet.setColumnWidth(6, 5600);  // ACCOUNT NUMBER

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void createHeaderCell(Sheet sheet, Row h1, Row h2, int colIdx, String text, CellStyle thStyle) {
        Cell cell = h1.createCell(colIdx);
        cell.setCellValue(text);
        cell.setCellStyle(thStyle);

        Cell cellBottom = h2.createCell(colIdx);
        cellBottom.setCellStyle(thStyle);

        sheet.addMergedRegion(new CellRangeAddress(8, 9, colIdx, colIdx));
    }

    private void setDashOrValue(Cell cell, BigDecimal value, CellStyle style) {
        if (value == null || value.compareTo(BigDecimal.ZERO) == 0) {
            cell.setCellValue("-");
            Workbook wb = cell.getSheet().getWorkbook();
            CellStyle centerStyle = wb.createCellStyle();
            centerStyle.cloneStyleFrom(style);
            centerStyle.setAlignment(HorizontalAlignment.CENTER);
            setBorders(centerStyle, BorderStyle.THIN);
            cell.setCellStyle(centerStyle);
        } else {
            cell.setCellValue(value.doubleValue());
            cell.setCellStyle(style);
        }
    }

    private void setDashOrValueInt(Cell cell, Integer value, CellStyle style) {
        if (value == null || value == 0) {
            cell.setCellValue("-");
            Workbook wb = cell.getSheet().getWorkbook();
            CellStyle centerStyle = wb.createCellStyle();
            centerStyle.cloneStyleFrom(style);
            centerStyle.setAlignment(HorizontalAlignment.CENTER);
            setBorders(centerStyle, BorderStyle.THIN);
            cell.setCellStyle(centerStyle);
        } else {
            cell.setCellValue(value);
            cell.setCellStyle(style);
        }
    }

    private void setBorders(CellStyle style, BorderStyle border) {
        style.setBorderTop(border);
        style.setBorderBottom(border);
        style.setBorderLeft(border);
        style.setBorderRight(border);
    }
}

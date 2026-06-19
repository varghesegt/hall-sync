package com.exam.service;

import com.exam.config.tenant.TenantContext;
import com.exam.entity.Faculty;
import com.exam.repository.InvigilatorDutyRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class RemunerationReportService {

    private final InvigilatorDutyRepository dutyRepository;

    public RemunerationReportService(InvigilatorDutyRepository dutyRepository) {
        this.dutyRepository = dutyRepository;
    }

    @Transactional(readOnly = true)
    public void generateRemunerationExcel(LocalDate from, LocalDate to, OutputStream out) {
        String tenantId = TenantContext.getCurrentTenant();
        String collegeName = TenantContext.getCollegeName(tenantId);
        if (collegeName == null) collegeName = "COLLEGE OF ENGINEERING";

        Double rate = TenantContext.getRemunerationRate(tenantId);
        if (rate == null) rate = 150.0;

        List<Object[]> data = dutyRepository.getRemunerationDataBetween(from, to);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Remuneration");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 14);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle colHeaderStyle = workbook.createCellStyle();
            Font colFont = workbook.createFont();
            colFont.setBold(true);
            colHeaderStyle.setFont(colFont);
            colHeaderStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            colHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            colHeaderStyle.setBorderBottom(BorderStyle.THIN);
            colHeaderStyle.setBorderTop(BorderStyle.THIN);
            colHeaderStyle.setBorderLeft(BorderStyle.THIN);
            colHeaderStyle.setBorderRight(BorderStyle.THIN);

            CellStyle borderStyle = workbook.createCellStyle();
            borderStyle.setBorderBottom(BorderStyle.THIN);
            borderStyle.setBorderTop(BorderStyle.THIN);
            borderStyle.setBorderLeft(BorderStyle.THIN);
            borderStyle.setBorderRight(BorderStyle.THIN);

            CellStyle currencyStyle = workbook.createCellStyle();
            currencyStyle.cloneStyleFrom(borderStyle);
            DataFormat format = workbook.createDataFormat();
            currencyStyle.setDataFormat(format.getFormat("₹#,##0.00"));

            int rowNum = 0;
            Row titleRow = sheet.createRow(rowNum++);
            titleRow.setHeightInPoints(30);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(collegeName.toUpperCase());
            titleCell.setCellStyle(headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));

            Row subTitleRow = sheet.createRow(rowNum++);
            Cell subTitleCell = subTitleRow.createCell(0);
            subTitleCell.setCellValue("INVIGILATION REMUNERATION REPORT");
            CellStyle subHeaderStyle = workbook.createCellStyle();
            Font subHeaderFont = workbook.createFont();
            subHeaderFont.setBold(true);
            subHeaderFont.setFontHeightInPoints((short) 12);
            subHeaderStyle.setFont(subHeaderFont);
            subHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
            subTitleCell.setCellStyle(subHeaderStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 6));

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
            Row dateRow = sheet.createRow(rowNum++);
            dateRow.createCell(0).setCellValue("From: " + from.format(dtf) + " To: " + to.format(dtf));
            dateRow.createCell(4).setCellValue("Rate per Session: ₹" + String.format("%.2f", rate));

            rowNum++; // spacer

            Row colHeader = sheet.createRow(rowNum++);
            String[] headers = {"S.No", "Faculty Name", "Employee ID", "Department", "Total Sessions", "Rate (₹)", "Total Amount (₹)"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = colHeader.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(colHeaderStyle);
            }

            int sno = 1;
            double grandTotal = 0.0;
            for (Object[] rowData : data) {
                Faculty faculty = (Faculty) rowData[0];
                Long count = (Long) rowData[1];
                double totalAmount = count * rate;
                grandTotal += totalAmount;

                Row row = sheet.createRow(rowNum++);
                
                Cell c0 = row.createCell(0); c0.setCellValue(sno++); c0.setCellStyle(borderStyle);
                Cell c1 = row.createCell(1); c1.setCellValue(faculty.getName()); c1.setCellStyle(borderStyle);
                Cell c2 = row.createCell(2); c2.setCellValue(faculty.getEmployeeId()); c2.setCellStyle(borderStyle);
                Cell c3 = row.createCell(3); c3.setCellValue(faculty.getDepartment()); c3.setCellStyle(borderStyle);
                
                Cell c4 = row.createCell(4); c4.setCellValue(count); c4.setCellStyle(borderStyle);
                
                Cell c5 = row.createCell(5); c5.setCellValue(rate); c5.setCellStyle(currencyStyle);
                
                Cell c6 = row.createCell(6); c6.setCellValue(totalAmount); c6.setCellStyle(currencyStyle);
            }

            // Grand Total Row
            Row totalRow = sheet.createRow(rowNum++);
            Cell totalLabelCell = totalRow.createCell(5);
            totalLabelCell.setCellValue("GRAND TOTAL:");
            totalLabelCell.setCellStyle(colHeaderStyle);
            
            Cell grandTotalCell = totalRow.createCell(6);
            grandTotalCell.setCellValue(grandTotal);
            CellStyle boldCurrency = workbook.createCellStyle();
            boldCurrency.cloneStyleFrom(currencyStyle);
            boldCurrency.setFont(colFont);
            grandTotalCell.setCellStyle(boldCurrency);

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            rowNum += 4;
            Row sigRow = sheet.createRow(rowNum);
            sigRow.createCell(1).setCellValue("Accountant / Cashier");
            sigRow.createCell(5).setCellValue("Chief Superintendent");

            workbook.write(out);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate remuneration report", e);
        }
    }
}

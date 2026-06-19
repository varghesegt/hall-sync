package com.exam.service;

import com.exam.config.tenant.TenantContext;
import com.exam.entity.AllocationBatch;
import com.exam.entity.ExamSession;
import com.exam.entity.StudentAttendance;
import com.exam.repository.AllocationBatchRepository;
import com.exam.repository.StudentAttendanceRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class AttendanceConsolidationService {

    private final StudentAttendanceRepository attendanceRepository;
    private final AllocationBatchRepository batchRepository;

    public AttendanceConsolidationService(StudentAttendanceRepository attendanceRepository, AllocationBatchRepository batchRepository) {
        this.attendanceRepository = attendanceRepository;
        this.batchRepository = batchRepository;
    }

    @Transactional(readOnly = true)
    public void generateAbsenteeReport(UUID batchId, OutputStream out) {
        AllocationBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalStateException("Batch not found"));
        ExamSession session = batch.getExamSession();

        String tenantId = TenantContext.getCurrentTenant();
        String collegeName = TenantContext.getCollegeName(tenantId);
        if (collegeName == null) collegeName = "COLLEGE OF ENGINEERING";

        List<StudentAttendance> absentees = attendanceRepository.findAbsenteesByBatch(batchId);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Consolidated Absentees");

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

            int rowNum = 0;
            Row titleRow = sheet.createRow(rowNum++);
            titleRow.setHeightInPoints(30);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(collegeName.toUpperCase());
            titleCell.setCellStyle(headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));

            Row subTitleRow = sheet.createRow(rowNum++);
            Cell subTitleCell = subTitleRow.createCell(0);
            subTitleCell.setCellValue("CONSOLIDATED ABSENTEE REPORT");
            CellStyle subHeaderStyle = workbook.createCellStyle();
            Font subHeaderFont = workbook.createFont();
            subHeaderFont.setBold(true);
            subHeaderFont.setFontHeightInPoints((short) 12);
            subHeaderStyle.setFont(subHeaderFont);
            subHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
            subTitleCell.setCellStyle(subHeaderStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 4));

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
            Row dateRow = sheet.createRow(rowNum++);
            dateRow.createCell(0).setCellValue("Exam Date: " + session.getExamDate().format(dtf));
            dateRow.createCell(3).setCellValue("Session: " + session.getSession());

            rowNum++; // spacer

            Row colHeader = sheet.createRow(rowNum++);
            String[] headers = {"S.No", "Register Number", "Student Name", "Department", "Hall Allocated"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = colHeader.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(colHeaderStyle);
            }

            if (absentees.isEmpty()) {
                Row emptyRow = sheet.createRow(rowNum++);
                Cell emptyCell = emptyRow.createCell(0);
                emptyCell.setCellValue("NIL - NO ABSENTEES RECORDED FOR THIS SESSION");
                sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 4));
            } else {
                int sno = 1;
                for (StudentAttendance att : absentees) {
                    Row row = sheet.createRow(rowNum++);
                    Cell c0 = row.createCell(0); c0.setCellValue(sno++); c0.setCellStyle(borderStyle);
                    Cell c1 = row.createCell(1); c1.setCellValue(att.getStudent().getRegisterNumber()); c1.setCellStyle(borderStyle);
                    Cell c2 = row.createCell(2); c2.setCellValue(att.getStudent().getName()); c2.setCellStyle(borderStyle);
                    Cell c3 = row.createCell(3); c3.setCellValue(att.getStudent().getDepartment()); c3.setCellStyle(borderStyle);
                    Cell c4 = row.createCell(4); c4.setCellValue(att.getHall().getName()); c4.setCellStyle(borderStyle);
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            rowNum += 4;
            Row sigRow = sheet.createRow(rowNum);
            sigRow.createCell(0).setCellValue("Exam Cell Coordinator");
            sigRow.createCell(3).setCellValue("Chief Superintendent");

            workbook.write(out);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate absentee report", e);
        }
    }
}

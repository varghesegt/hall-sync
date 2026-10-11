package com.exam.parser;

import com.exam.parser.model.ParseResult;
import com.exam.parser.model.ParseStatus;
import com.exam.parser.model.StudentRow;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

public class ExcelParserPipelineTest {

    @Test
    public void testStandardRosterWithDepartmentNameAndSubjectCode() throws Exception {
        byte[] bytes = createExcel(sheet -> {
            // Row 0: College title banner
            Row r0 = sheet.createRow(0);
            r0.createCell(0).setCellValue("K. RAMAKRISHNAN COLLEGE OF ENGINEERING");

            // Row 1: Exam banner
            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue("CONTINUOUS INTERNAL ASSESSMENT TEST - I");

            // Row 2: Empty
            sheet.createRow(2);

            // Row 3: Table Header with tricky column names
            Row header = sheet.createRow(3);
            header.createCell(0).setCellValue("S.No");
            header.createCell(1).setCellValue("Register Number");
            header.createCell(2).setCellValue("Student Name");
            header.createCell(3).setCellValue("Department Name"); // Contains 'Name'
            header.createCell(4).setCellValue("Class & Section");
            header.createCell(5).setCellValue("Subject Code");    // Contains 'Subject'
            header.createCell(6).setCellValue("Subject Name");
            header.createCell(7).setCellValue("Semester");
            header.createCell(8).setCellValue("Regulation");

            // Data Rows
            Row d1 = sheet.createRow(4);
            d1.createCell(0).setCellValue("1");
            d1.createCell(1).setCellValue("811521104001");
            d1.createCell(2).setCellValue("Aravind S");
            d1.createCell(3).setCellValue("CSE");
            d1.createCell(4).setCellValue("III CSE A");
            d1.createCell(5).setCellValue("CS3301");
            d1.createCell(6).setCellValue("Data Structures");
            d1.createCell(7).setCellValue("5");
            d1.createCell(8).setCellValue("2021");

            Row d2 = sheet.createRow(5);
            d2.createCell(0).setCellValue("2");
            d2.createCell(1).setCellValue("811521104002");
            d2.createCell(2).setCellValue("Bhavani M");
            d2.createCell(3).setCellValue("CSE");
            d2.createCell(4).setCellValue("III CSE A");
            d2.createCell(5).setCellValue("CS3301");
            d2.createCell(6).setCellValue("Data Structures");
            d2.createCell(7).setCellValue("5");
            d2.createCell(8).setCellValue("2021");
        });

        ExcelParserPipeline parser = new ExcelParserPipeline();
        ParseResult result = parser.parse(bytes);

        assertEquals(ParseStatus.PASSED, result.status());
        assertEquals(2, result.students().size());

        StudentRow s1 = result.students().get(0);
        assertEquals("811521104001", s1.registerNumber());
        assertEquals("Aravind S", s1.studentName());
        assertEquals("CSE", s1.department());
        assertEquals("CS3301", s1.subjectCode());
        assertEquals("Data Structures", s1.subjectName());
    }

    @Test
    public void testSheetWithoutDepartmentColumn_InfersFromTitleOrRegNo() throws Exception {
        byte[] bytes = createExcel(sheet -> {
            // Title block
            Row r0 = sheet.createRow(0);
            r0.createCell(0).setCellValue("DEPARTMENT OF ARTIFICIAL INTELLIGENCE AND DATA SCIENCE");
            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue("CLASS: III AIDS A");

            // Header at row 5 (No Department column!)
            Row header = sheet.createRow(5);
            header.createCell(0).setCellValue("Roll No.");
            header.createCell(1).setCellValue("Candidate Name");
            header.createCell(2).setCellValue("Sub Code");
            header.createCell(3).setCellValue("Subject Title");

            // Student with KRCE alphanumeric reg no
            Row d1 = sheet.createRow(6);
            d1.createCell(0).setCellValue("8115U24AD001");
            d1.createCell(1).setCellValue("Deepak R");
            d1.createCell(2).setCellValue("AD3491");
            d1.createCell(3).setCellValue("Cryptography & Network Security");
        });

        ExcelParserPipeline parser = new ExcelParserPipeline();
        ParseResult result = parser.parse(bytes);

        assertNotEquals(ParseStatus.FAILED, result.status());
        assertEquals(1, result.students().size());

        StudentRow s = result.students().get(0);
        assertEquals("8115U24AD001", s.registerNumber());
        assertEquals("Deepak R", s.studentName());
        assertEquals("AIDS", s.department()); // Inferred from title or reg no!
        assertEquals("AD3491", s.subjectCode());
    }

    @Test
    public void testMultiSheetWorkbook_SkipsEmptySheet0() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (Workbook wb = new XSSFWorkbook()) {
            // Sheet 0: Cover / Summary
            Sheet s0 = wb.createSheet("Summary");
            Row r0 = s0.createRow(0);
            r0.createCell(0).setCellValue("Notice: Internal exams October 2026");

            // Sheet 1: Actual Students
            Sheet s1 = wb.createSheet("ECE Students");
            Row header = s1.createRow(2);
            header.createCell(0).setCellValue("Reg No");
            header.createCell(1).setCellValue("Name");
            header.createCell(2).setCellValue("Branch");
            header.createCell(3).setCellValue("Subject");

            Row d1 = s1.createRow(3);
            d1.createCell(0).setCellValue("8115U24EC005");
            d1.createCell(1).setCellValue("Karthik M");
            d1.createCell(2).setCellValue("ECE");
            d1.createCell(3).setCellValue("EC3351 Control Systems");

            wb.write(out);
        }

        ExcelParserPipeline parser = new ExcelParserPipeline();
        ParseResult result = parser.parse(out.toByteArray());

        assertNotEquals(ParseStatus.FAILED, result.status());
        assertEquals(1, result.students().size());
        assertEquals("8115U24EC005", result.students().get(0).registerNumber());
        assertEquals("ECE", result.students().get(0).department());
    }

    private byte[] createExcel(java.util.function.Consumer<Sheet> populator) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Students");
            populator.accept(sheet);
            wb.write(out);
        }
        return out.toByteArray();
    }
}

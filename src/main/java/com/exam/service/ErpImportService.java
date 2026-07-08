package com.exam.service;

import com.exam.dto.ImportResult;
import com.exam.entity.ExamSession;
import com.exam.entity.Student;
import com.exam.repository.ExamSessionRepository;
import com.exam.repository.StudentRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ErpImportService {

    private static final Logger log = LoggerFactory.getLogger(ErpImportService.class);

    private final StudentRepository studentRepository;
    private final ExamSessionRepository sessionRepository;

    public ErpImportService(StudentRepository studentRepository,
                            ExamSessionRepository sessionRepository) {
        this.studentRepository = studentRepository;
        this.sessionRepository = sessionRepository;
    }

    /**
     * Import students from Excel (.xlsx) file.
     * Expected columns: Register Number, Name, Department, Semester, Subject Code, Subject Name
     */
    public ImportResult importFromExcel(MultipartFile file, UUID sessionId) {
        ImportResult result = new ImportResult();
        ExamSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            int lastRow = sheet.getLastRowNum();
            result.setTotalRows(lastRow); // excluding header

            List<Student> studentsToSave = new ArrayList<>();
            List<String> registerNumbers = new ArrayList<>();

            for (int i = 1; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    String regNo = getCellString(row, 0);
                    String name = getCellString(row, 1);
                    String department = getCellString(row, 2);
                    String semester = getCellString(row, 3);
                    String subjectCode = getCellString(row, 4);
                    String subjectName = getCellString(row, 5);

                    if (regNo == null || regNo.isBlank()) {
                        result.addError("Row " + (i + 1) + ": Missing register number");
                        result.incrementFailed();
                        continue;
                    }
                    if (department == null || department.isBlank()) {
                        result.addError("Row " + (i + 1) + ": Missing department for " + regNo);
                        result.incrementFailed();
                        continue;
                    }

                    Student student = new Student(UUID.randomUUID(), session, regNo, name,
                            department, null, subjectName, subjectCode, semester, null);
                    studentsToSave.add(student);
                    registerNumbers.add(regNo);
                } catch (Exception e) {
                    result.addError("Row " + (i + 1) + ": " + e.getMessage());
                    result.incrementFailed();
                }
            }

            // Bulk deduplication
            if (!registerNumbers.isEmpty()) {
                Set<String> existingRegNos = studentRepository.findByExamSessionIdAndRegisterNumberIn(sessionId, registerNumbers)
                        .stream().map(Student::getRegisterNumber).collect(Collectors.toSet());

                List<Student> validStudents = new ArrayList<>();
                for (Student s : studentsToSave) {
                    if (existingRegNos.contains(s.getRegisterNumber())) {
                        result.incrementSkipped();
                    } else {
                        validStudents.add(s);
                        // Prevent duplicates within the same file from being inserted twice
                        existingRegNos.add(s.getRegisterNumber());
                        result.incrementSuccess();
                    }
                }
                
                if (!validStudents.isEmpty()) {
                    studentRepository.saveAll(validStudents);
                }
            }
        } catch (Exception e) {
            result.addError("Failed to parse Excel file: " + e.getMessage());
            log.error("Excel import error", e);
        }
        return result;
    }

    /**
     * Import from Camu CSV format.
     * Expected columns: Camu ID, Student Name, Register No, Department, Semester, Subject Code, Subject Name
     */
    public ImportResult importFromCamuCsv(MultipartFile file, UUID sessionId) {
        return importFromCsv(file, sessionId, "CAMU", new int[]{2, 1, 3, 4, 5, 6});
    }

    /**
     * Import from iCloudEMS CSV format.
     * Expected columns: Enrollment No, Program, Batch, Subject Code, Subject Name
     */
    public ImportResult importFromICloudEmsCsv(MultipartFile file, UUID sessionId) {
        return importFromCsv(file, sessionId, "ICLOUDEMS", new int[]{0, -1, 1, 2, 3, 4});
    }

    private ImportResult importFromCsv(MultipartFile file, UUID sessionId, String format, int[] colMap) {
        ImportResult result = new ImportResult();
        ExamSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line = reader.readLine(); // skip header
            int rowNum = 1;

            List<Student> studentsToSave = new ArrayList<>();
            List<String> registerNumbers = new ArrayList<>();

            while ((line = reader.readLine()) != null) {
                rowNum++;
                if (line.isBlank()) continue;
                result.setTotalRows(result.getTotalRows() + 1);

                try {
                    String[] cols = line.split(",", -1);

                    String regNo = safeGet(cols, colMap[0]);
                    String name = colMap[1] >= 0 ? safeGet(cols, colMap[1]) : null;
                    String department = safeGet(cols, colMap[2]);
                    String semester = safeGet(cols, colMap[3]);
                    String subjectCode = safeGet(cols, colMap[4]);
                    String subjectName = colMap.length > 5 ? safeGet(cols, colMap[5]) : null;

                    if (regNo == null || regNo.isBlank()) {
                        result.addError("Row " + rowNum + ": Missing register number");
                        result.incrementFailed();
                        continue;
                    }
                    if (department == null || department.isBlank()) {
                        result.addError("Row " + rowNum + ": Missing department for " + regNo);
                        result.incrementFailed();
                        continue;
                    }

                    Student student = new Student(UUID.randomUUID(), session, regNo, name,
                            department, null, subjectName, subjectCode, semester, null);
                    studentsToSave.add(student);
                    registerNumbers.add(regNo);
                } catch (Exception e) {
                    result.addError("Row " + rowNum + ": " + e.getMessage());
                    result.incrementFailed();
                }
            }
            
            // Bulk deduplication
            if (!registerNumbers.isEmpty()) {
                Set<String> existingRegNos = studentRepository.findByExamSessionIdAndRegisterNumberIn(sessionId, registerNumbers)
                        .stream().map(Student::getRegisterNumber).collect(Collectors.toSet());

                List<Student> validStudents = new ArrayList<>();
                for (Student s : studentsToSave) {
                    if (existingRegNos.contains(s.getRegisterNumber())) {
                        result.incrementSkipped();
                    } else {
                        validStudents.add(s);
                        existingRegNos.add(s.getRegisterNumber());
                        result.incrementSuccess();
                    }
                }
                
                if (!validStudents.isEmpty()) {
                    studentRepository.saveAll(validStudents);
                }
            }
        } catch (Exception e) {
            result.addError("Failed to parse " + format + " CSV: " + e.getMessage());
            log.error(format + " CSV import error", e);
        }
        return result;
    }

    /**
     * Generate a blank template for download.
     */
    public byte[] generateTemplate(String type) throws IOException {
        switch (type.toLowerCase()) {
            case "excel":
                return generateExcelTemplate();
            case "camu":
                return "Camu ID,Student Name,Register No,Department,Semester,Subject Code,Subject Name\n".getBytes(StandardCharsets.UTF_8);
            case "icloudems":
                return "Enrollment No,Program,Batch,Subject Code,Subject Name\n".getBytes(StandardCharsets.UTF_8);
            default:
                throw new IllegalArgumentException("Unknown template type: " + type);
        }
    }

    private byte[] generateExcelTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Students");
            Row header = sheet.createRow(0);
            String[] headers = {"Register Number", "Name", "Department", "Semester", "Subject Code", "Subject Name"};
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 5000);
            }

            // Add sample row
            Row sample = sheet.createRow(1);
            sample.createCell(0).setCellValue("21CSE001");
            sample.createCell(1).setCellValue("John Doe");
            sample.createCell(2).setCellValue("CSE");
            sample.createCell(3).setCellValue("6");
            sample.createCell(4).setCellValue("CS6001");
            sample.createCell(5).setCellValue("Data Structures");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private String getCellString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue().trim();
    }

    private String safeGet(String[] arr, int idx) {
        if (idx < 0 || idx >= arr.length) return null;
        String val = arr[idx].trim();
        // Remove surrounding quotes
        if (val.startsWith("\"") && val.endsWith("\"")) {
            val = val.substring(1, val.length() - 1);
        }
        return val.isEmpty() ? null : val;
    }
}

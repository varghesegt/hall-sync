package com.exam.service;

import com.exam.entity.Faculty;
import com.exam.repository.FacultyRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

@Service
public class FacultyService {

    private static final Logger logger = LoggerFactory.getLogger(FacultyService.class);

    private final FacultyRepository facultyRepository;

    public FacultyService(FacultyRepository facultyRepository) {
        this.facultyRepository = facultyRepository;
    }

    public List<Faculty> getAllActiveFaculty() {
        return facultyRepository.findByIsActiveTrueOrderByDepartmentAscNameAsc();
    }

    public List<Faculty> getByDepartment(String department) {
        return facultyRepository.findByDepartmentAndIsActiveTrue(department);
    }

    public Optional<Faculty> getById(UUID id) {
        return facultyRepository.findById(id);
    }

    @Transactional
    public Faculty create(String name, String employeeId, String department,
                          String designation, String phone, String email,
                          String collegeName, Boolean isInternal, Boolean isAvailable) {
        Optional<Faculty> existingOpt = facultyRepository.findByEmployeeId(employeeId);
        if (existingOpt.isPresent()) {
            Faculty existing = existingOpt.get();
            if (existing.getIsActive() != null && existing.getIsActive()) {
                throw new IllegalArgumentException("Faculty with employee ID " + employeeId + " already exists.");
            }
            // Reactivate soft-deleted faculty member
            existing.setName(name);
            existing.setDepartment(department);
            existing.setDesignation(designation);
            existing.setPhone(phone);
            existing.setEmail(email);
            Boolean internal = isInternal != null ? isInternal : true;
            String resolvedCollegeName = collegeName;
            if (internal && (resolvedCollegeName == null || resolvedCollegeName.isBlank())) {
                String tenantId = com.exam.config.tenant.TenantContext.getCurrentTenant();
                resolvedCollegeName = com.exam.config.tenant.TenantContext.getCollegeName(tenantId);
                if (resolvedCollegeName == null) resolvedCollegeName = "Internal College";
            }
            existing.setCollegeName(resolvedCollegeName);
            existing.setIsInternal(internal);
            if (isAvailable != null) existing.setIsAvailable(isAvailable);
            existing.setIsActive(true);
            return facultyRepository.save(existing);
        }
        
        Boolean internal = isInternal != null ? isInternal : true;
        String resolvedCollegeName = collegeName;
        
        if (internal && (resolvedCollegeName == null || resolvedCollegeName.isBlank())) {
            String tenantId = com.exam.config.tenant.TenantContext.getCurrentTenant();
            resolvedCollegeName = com.exam.config.tenant.TenantContext.getCollegeName(tenantId);
            if (resolvedCollegeName == null) resolvedCollegeName = "Internal College";
        }

        Faculty faculty = new Faculty(UUID.randomUUID(), name, employeeId, department,
                designation, phone, email, resolvedCollegeName, internal, isAvailable);
        return facultyRepository.save(faculty);
    }

    @Transactional
    public Faculty update(UUID id, String name, String employeeId, String department,
                          String designation, String phone, String email,
                          String collegeName, Boolean isInternal, Boolean isAvailable) {
        Faculty faculty = facultyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Faculty not found: " + id));
        faculty.setName(name);
        faculty.setEmployeeId(employeeId);
        faculty.setDepartment(department);
        faculty.setDesignation(designation);
        faculty.setPhone(phone);
        faculty.setEmail(email);
        
        Boolean internal = isInternal != null ? isInternal : true;
        String resolvedCollegeName = collegeName;
        
        if (internal && (resolvedCollegeName == null || resolvedCollegeName.isBlank())) {
            String tenantId = com.exam.config.tenant.TenantContext.getCurrentTenant();
            resolvedCollegeName = com.exam.config.tenant.TenantContext.getCollegeName(tenantId);
            if (resolvedCollegeName == null) resolvedCollegeName = "Internal College";
        }
        
        faculty.setCollegeName(resolvedCollegeName);
        faculty.setIsInternal(internal);
        if (isAvailable != null) {
            faculty.setIsAvailable(isAvailable);
        }
        return facultyRepository.save(faculty);
    }

    @Transactional
    public void softDelete(UUID id) {
        Faculty faculty = facultyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Faculty not found: " + id));
        faculty.setIsActive(false);
        facultyRepository.save(faculty);
    }

    @Transactional
    public void softDeleteAll() {
        List<Faculty> active = facultyRepository.findByIsActiveTrueOrderByDepartmentAscNameAsc();
        for (Faculty f : active) {
            f.setIsActive(false);
        }
        facultyRepository.saveAll(active);
    }

    @Transactional
    public Map<String, Object> bulkImportFromExcel(MultipartFile file) {
        List<Faculty> toSave = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        Set<String> processedEmpIds = new HashSet<>();
        int rowCount = 0;

        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            // Skip header
            if (rows.hasNext()) rows.next();

            while (rows.hasNext()) {
                Row row = rows.next();
                rowCount++;

                String name = getCellString(row, 0);
                String employeeId = getCellString(row, 1);
                String department = getCellString(row, 2);
                String designation = getCellString(row, 3);
                String phone = getCellString(row, 4);
                String email = getCellString(row, 5);
                String collegeName = getCellString(row, 6);
                String isInternalStr = getCellString(row, 7);
                String isAvailableStr = getCellString(row, 8);

                if (name == null && employeeId == null && department == null) {
                    continue; // Skip entirely empty rows gracefully
                }

                if (name == null || name.isBlank() || employeeId == null || employeeId.isBlank()) {
                    skipped.add("Row " + (rowCount + 1) + ": missing name or employee ID");
                    continue;
                }

                String empIdClean = employeeId.trim();
                if (processedEmpIds.contains(empIdClean)) {
                    skipped.add("Row " + (rowCount + 1) + ": duplicate employee ID " + empIdClean + " in file");
                    continue;
                }
                processedEmpIds.add(empIdClean);

                boolean isInternal = isInternalStr == null || !isInternalStr.trim().equalsIgnoreCase("No");
                boolean isAvailable = isAvailableStr == null || !isAvailableStr.trim().equalsIgnoreCase("No");

                String resolvedCollegeName = collegeName;
                if (isInternal && (resolvedCollegeName == null || resolvedCollegeName.isBlank())) {
                    String tenantId = com.exam.config.tenant.TenantContext.getCurrentTenant();
                    resolvedCollegeName = com.exam.config.tenant.TenantContext.getCollegeName(tenantId);
                    if (resolvedCollegeName == null) resolvedCollegeName = "Internal College";
                }

                Optional<Faculty> existingOpt = facultyRepository.findByEmployeeId(empIdClean);
                if (existingOpt.isPresent()) {
                    Faculty existing = existingOpt.get();
                    existing.setName(name.trim());
                    existing.setDepartment(department != null ? department.trim() : "UNKNOWN");
                    existing.setDesignation(designation != null ? designation.trim() : null);
                    existing.setPhone(phone != null ? phone.trim() : null);
                    existing.setEmail(email != null ? email.trim() : null);
                    existing.setCollegeName(resolvedCollegeName);
                    existing.setIsInternal(isInternal);
                    existing.setIsAvailable(isAvailable);
                    existing.setIsActive(true);
                    toSave.add(existing);
                    continue;
                }

                Faculty faculty = new Faculty(UUID.randomUUID(), name.trim(), empIdClean,
                        department != null ? department.trim() : "UNKNOWN",
                        designation != null ? designation.trim() : null,
                        phone != null ? phone.trim() : null,
                        email != null ? email.trim() : null,
                        resolvedCollegeName, isInternal, isAvailable);
                toSave.add(faculty);
            }

            if (!toSave.isEmpty()) {
                facultyRepository.saveAll(toSave);
            }

        } catch (Exception e) {
            logger.error("Failed to parse faculty Excel file", e);
            throw new RuntimeException("Failed to parse Excel file: " + e.getMessage());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("imported", toSave.size());
        result.put("skipped", skipped.size());
        result.put("totalRows", rowCount);
        result.put("skippedDetails", skipped);
        return result;
    }

    public void generateTemplate(OutputStream out) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Faculty");
            Row header = sheet.createRow(0);
            String[] columns = {"Name", "Employee ID", "Department", "Designation", "Phone", "Email", "College Name", "Is Internal (Yes/No)", "Is Available (Yes/No)"};
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            for (int i = 0; i < columns.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 5000);
            }

            // Generate 30 mock Indian staff
            String[] firstNames = {"Rajesh", "Priya", "Amit", "Sneha", "Vikram", "Anjali", "Suresh", "Kavita", "Ramesh", "Pooja", "Arun", "Neha", "Manoj", "Swati", "Sanjay", "Deepa"};
            String[] lastNames = {"Kumar", "Sharma", "Singh", "Patel", "Reddy", "Iyer", "Nair", "Rao", "Das", "Verma"};
            String[] departments = {"CSE", "ECE", "MECH", "CIVIL", "EEE", "IT", "AI&DS", "MBA"};
            String[] designations = {"Professor", "Associate Professor", "Assistant Professor"};
            String tenantId = com.exam.config.tenant.TenantContext.getCurrentTenant();
            String defaultCollege = com.exam.config.tenant.TenantContext.getCollegeName(tenantId);
            if (defaultCollege == null) defaultCollege = "Internal College";

            for (int i = 1; i <= 35; i++) {
                Row row = sheet.createRow(i);
                String name = firstNames[i % firstNames.length] + " " + lastNames[i % lastNames.length];
                boolean isInternal = i <= 25; // First 25 are internal
                String college = isInternal ? defaultCollege : "External Engineering College";
                
                row.createCell(0).setCellValue(name);
                row.createCell(1).setCellValue("EMP" + (1000 + i));
                row.createCell(2).setCellValue(departments[i % departments.length]);
                row.createCell(3).setCellValue(designations[i % designations.length]);
                row.createCell(4).setCellValue("98765" + String.format("%05d", i));
                row.createCell(5).setCellValue(firstNames[i % firstNames.length].toLowerCase() + "." + lastNames[i % lastNames.length].toLowerCase() + "@example.com");
                row.createCell(6).setCellValue(college);
                row.createCell(7).setCellValue(isInternal ? "Yes" : "No");
                row.createCell(8).setCellValue("Yes");
            }

            workbook.write(out);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate template", e);
        }
    }

    private final DataFormatter dataFormatter = new DataFormatter();

    private String getCellString(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        return dataFormatter.formatCellValue(cell);
    }
}

package com.exam.service;

import com.exam.engine.invigilator.InvigilatorAllocationEngine;
import com.exam.entity.*;
import com.exam.repository.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InvigilatorDutyService {

    private static final Logger logger = LoggerFactory.getLogger(InvigilatorDutyService.class);

    private final InvigilatorDutyRepository dutyRepository;
    private final FacultyRepository facultyRepository;
    private final AllocationRepository allocationRepository;
    private final AllocationBatchRepository batchRepository;
    private final InvigilatorAllocationEngine engine = new InvigilatorAllocationEngine();

    public InvigilatorDutyService(InvigilatorDutyRepository dutyRepository,
                                  FacultyRepository facultyRepository,
                                  AllocationRepository allocationRepository,
                                  AllocationBatchRepository batchRepository) {
        this.dutyRepository = dutyRepository;
        this.facultyRepository = facultyRepository;
        this.allocationRepository = allocationRepository;
        this.batchRepository = batchRepository;
    }

    @Transactional
    public Map<String, Object> allocateInvigilators(UUID batchId) {
        AllocationBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + batchId));

        ExamSession session = batch.getExamSession();
        String shift = session.getSession();
        LocalDate examDate = session.getExamDate();

        // Get all active and available faculty
        List<Faculty> availableFaculty = facultyRepository.findByIsActiveTrueAndIsAvailableTrueOrderByDepartmentAscNameAsc();
        if (availableFaculty.isEmpty()) {
            throw new IllegalStateException("No active faculty found. Please add faculty before allocating duties.");
        }

        // Build hall -> departments map from allocations
        List<Allocation> allocations = allocationRepository.findByBatchIdWithDetails(batchId);
        Map<Hall, Set<String>> hallDepts = new LinkedHashMap<>();
        for (Allocation alloc : allocations) {
            hallDepts.computeIfAbsent(alloc.getHall(), k -> new HashSet<>())
                    .add(alloc.getStudent().getDepartment());
        }

        // Get existing duty counts for fairness
        LocalDate monthStart = examDate.withDayOfMonth(1);
        LocalDate monthEnd = examDate.withDayOfMonth(examDate.lengthOfMonth());
        Map<UUID, Long> existingDutyCount = new HashMap<>();
        for (Faculty f : availableFaculty) {
            long count = dutyRepository.countByFacultyIdAndDutyDateBetween(f.getId(), monthStart, monthEnd);
            existingDutyCount.put(f.getId(), count);
        }

        // Check who is already assigned for the other shift today
        Set<UUID> sameDayAssigned = dutyRepository.findAll().stream()
                .filter(d -> d.getDutyDate().equals(examDate) && !d.getShift().equals(shift))
                .map(d -> d.getFaculty().getId())
                .collect(Collectors.toSet());

        // Clear existing duties for this batch (re-allocation)
        dutyRepository.deleteByBatchId(batchId);
        dutyRepository.flush();

        // Run the engine
        InvigilatorAllocationEngine.AllocationResult result =
                engine.allocate(availableFaculty, hallDepts, existingDutyCount, sameDayAssigned);

        // Persist assignments
        List<InvigilatorDuty> duties = new ArrayList<>();
        for (InvigilatorAllocationEngine.DutyAssignment assignment : result.assignments()) {
            InvigilatorDuty duty = new InvigilatorDuty(
                    UUID.randomUUID(), batch, assignment.faculty(), assignment.hall(),
                    assignment.dutyType(), shift, examDate);
            duties.add(duty);
        }
        dutyRepository.saveAll(duties);

        logger.info("Allocated {} invigilators for batch {} with {} warnings",
                duties.size(), batchId, result.warnings().size());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("assigned", duties.size());
        response.put("totalHalls", hallDepts.size());
        response.put("warnings", result.warnings());
        return response;
    }

    public List<InvigilatorDuty> getDutiesByBatch(UUID batchId) {
        return dutyRepository.findByBatchIdOrderByHallIdAsc(batchId);
    }

    @Transactional
    public void markAttendance(UUID dutyId, boolean present) {
        InvigilatorDuty duty = dutyRepository.findById(dutyId)
                .orElseThrow(() -> new IllegalArgumentException("Duty not found: " + dutyId));
        duty.setIsPresent(present);
        duty.setMarkedAt(java.time.LocalDateTime.now());
        dutyRepository.save(duty);
    }

    @Transactional(readOnly = true)
    public void generateDutyExcel(UUID batchId, OutputStream out) {
        List<InvigilatorDuty> duties = dutyRepository.findByBatchIdOrderByHallIdAsc(batchId);
        if (duties.isEmpty()) {
            throw new IllegalStateException("No duties found for batch: " + batchId);
        }

        AllocationBatch batch = duties.get(0).getBatch();
        ExamSession session = batch.getExamSession();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Invigilation Duties");

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

            int rowNum = 0;

            String tenantId = com.exam.config.tenant.TenantContext.getCurrentTenant();
            String collegeName = com.exam.config.tenant.TenantContext.getCollegeName(tenantId);
            if (collegeName == null) collegeName = "COLLEGE OF ENGINEERING";

            Row titleRow = sheet.createRow(rowNum++);
            titleRow.setHeightInPoints(30);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(collegeName.toUpperCase());
            titleCell.setCellStyle(headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

            Row subTitleRow = sheet.createRow(rowNum++);
            Cell subTitleCell = subTitleRow.createCell(0);
            subTitleCell.setCellValue("INVIGILATION DUTY ALLOCATION");
            CellStyle subHeaderStyle = workbook.createCellStyle();
            Font subHeaderFont = workbook.createFont();
            subHeaderFont.setBold(true);
            subHeaderFont.setFontHeightInPoints((short) 12);
            subHeaderStyle.setFont(subHeaderFont);
            subHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
            subTitleCell.setCellStyle(subHeaderStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 5));

            Row dateRow = sheet.createRow(rowNum++);
            dateRow.createCell(0).setCellValue("Date: " +
                    session.getExamDate().format(java.time.format.DateTimeFormatter.ofPattern("dd-MMM-yyyy")));
            dateRow.createCell(3).setCellValue("Session: " + session.getSession());

            rowNum++; // spacer

            Row colHeader = sheet.createRow(rowNum++);
            String[] headers = {"S.No", "Faculty Name", "Employee ID", "Department", "Hall", "Duty Type"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = colHeader.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(colHeaderStyle);
            }

            CellStyle borderStyle = workbook.createCellStyle();
            borderStyle.setBorderTop(BorderStyle.THIN);
            borderStyle.setBorderBottom(BorderStyle.THIN);
            borderStyle.setBorderLeft(BorderStyle.THIN);
            borderStyle.setBorderRight(BorderStyle.THIN);

            int sno = 1;
            for (InvigilatorDuty duty : duties) {
                Row row = sheet.createRow(rowNum++);
                Cell c0 = row.createCell(0); c0.setCellValue(sno++); c0.setCellStyle(borderStyle);
                Cell c1 = row.createCell(1); c1.setCellValue(duty.getFaculty().getName()); c1.setCellStyle(borderStyle);
                Cell c2 = row.createCell(2); c2.setCellValue(duty.getFaculty().getEmployeeId()); c2.setCellStyle(borderStyle);
                Cell c3 = row.createCell(3); c3.setCellValue(duty.getFaculty().getDepartment()); c3.setCellStyle(borderStyle);
                Cell c4 = row.createCell(4); c4.setCellValue(duty.getHall().getName()); c4.setCellStyle(borderStyle);
                Cell c5 = row.createCell(5); c5.setCellValue(duty.getDutyType()); c5.setCellStyle(borderStyle);
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            rowNum += 3;
            Row sigRow = sheet.createRow(rowNum);
            Cell sigLeft = sigRow.createCell(0);
            sigLeft.setCellValue("Exam Cell Coordinator");
            
            Cell sigRight = sigRow.createCell(4);
            sigRight.setCellValue("Chief Superintendent");

            workbook.write(out);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate duty Excel", e);
        }
    }
}

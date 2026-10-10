package com.exam.service;

import com.exam.engine.invigilator.InvigilatorAllocationEngine;
import com.exam.entity.*;
import com.exam.repository.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
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
    public Map<String, Object> allocateInvigilators(UUID batchId, List<UUID> selectedFacultyIds) {
        AllocationBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + batchId));

        ExamSession session = batch.getExamSession();
        String shift = session.getSession();
        LocalDate examDate = session.getExamDate();

        // Get all active and available faculty
        List<Faculty> availableFaculty;
        if (selectedFacultyIds != null && !selectedFacultyIds.isEmpty()) {
            availableFaculty = facultyRepository.findAllById(selectedFacultyIds).stream()
                    .filter(f -> f.getIsActive() && f.getIsAvailable())
                    .collect(Collectors.toList());
        } else {
            availableFaculty = facultyRepository.findByIsActiveTrueAndIsAvailableTrueOrderByDepartmentAscNameAsc();
        }
        
        if (availableFaculty.isEmpty()) {
            throw new IllegalStateException("No active faculty found. Please select faculty before allocating duties.");
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

        // Count student strength per department for proportional duty allocation
        Map<String, Integer> deptStudentCounts = new HashMap<>();
        for (Allocation alloc : allocations) {
            String normDept = InvigilatorAllocationEngine.normalizeDept(alloc.getStudent().getDepartment());
            deptStudentCounts.merge(normDept, 1, Integer::sum);
        }

        // Track previously assigned halls for each faculty member in current CIA/month for Staff Hall Rotation
        Map<UUID, Set<String>> facultyPreviousHalls = new HashMap<>();
        for (Faculty f : availableFaculty) {
            Set<String> prevHalls = new HashSet<>();
            List<InvigilatorDuty> pastDuties = dutyRepository.findByFacultyId(f.getId());
            for (InvigilatorDuty d : pastDuties) {
                if (d.getHall() != null && d.getHall().getId() != null) {
                    prevHalls.add(d.getHall().getId());
                }
            }
            facultyPreviousHalls.put(f.getId(), prevHalls);
        }

        // Run the engine with student strength ratios, supporting dept caps, max CIA caps, and Staff Hall Rotation
        InvigilatorAllocationEngine.AllocationResult result =
                engine.allocate(availableFaculty, hallDepts, existingDutyCount, sameDayAssigned, deptStudentCounts, facultyPreviousHalls);

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
    public void clearDutiesByBatch(UUID batchId) {
        dutyRepository.deleteByBatchId(batchId);
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

            sheet.setColumnWidth(0, 2000);  // S.No
            sheet.setColumnWidth(1, 6500);  // Faculty Name
            sheet.setColumnWidth(2, 3500);  // Employee ID
            sheet.setColumnWidth(3, 3500);  // Department
            sheet.setColumnWidth(4, 3800);  // Hall Name
            sheet.setColumnWidth(5, 3500);  // Duty Type

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

    @Transactional(readOnly = true)
    public byte[] generateDutyWord(UUID batchId) {
        List<InvigilatorDuty> duties = dutyRepository.findByBatchIdOrderByHallIdAsc(batchId);
        if (duties.isEmpty()) {
            throw new IllegalStateException("No duties found for batch: " + batchId);
        }

        AllocationBatch batch = duties.get(0).getBatch();
        ExamSession session = batch.getExamSession();

        String tenantId = com.exam.config.tenant.TenantContext.getCurrentTenant();
        String collegeName = com.exam.config.tenant.TenantContext.getCollegeName(tenantId);
        if (collegeName == null) collegeName = "K.RAMAKRISHNAN COLLEGE OF ENGINEERING";

        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr sectPr = doc.getDocument().getBody().addNewSectPr();
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar pageMar = sectPr.addNewPgMar();
            pageMar.setTop(java.math.BigInteger.valueOf(720));
            pageMar.setBottom(java.math.BigInteger.valueOf(720));
            pageMar.setLeft(java.math.BigInteger.valueOf(720));
            pageMar.setRight(java.math.BigInteger.valueOf(720));

            XWPFTable headerTable = doc.createTable(1, 2);
            headerTable.setWidth("100%");
            headerTable.removeBorders();

            headerTable.getRow(0).getCell(0).getCTTc().addNewTcPr().addNewTcW().setW(java.math.BigInteger.valueOf(1800));
            headerTable.getRow(0).getCell(1).getCTTc().addNewTcPr().addNewTcW().setW(java.math.BigInteger.valueOf(8200));

            XWPFTableCell logoCell = headerTable.getRow(0).getCell(0);
            logoCell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
            XWPFParagraph logoPara = logoCell.getParagraphs().get(0);
            logoPara.setAlignment(ParagraphAlignment.CENTER);
            try (java.io.InputStream is = getClass().getResourceAsStream("/logo1.png")) {
                if (is != null) {
                    byte[] logoBytes = is.readAllBytes();
                    logoPara.createRun().addPicture(new java.io.ByteArrayInputStream(logoBytes),
                            Document.PICTURE_TYPE_PNG, "logo1.png",
                            org.apache.poi.util.Units.toEMU(90), org.apache.poi.util.Units.toEMU(90));
                }
            } catch (Exception e) {
                logger.warn("Could not insert logo into Duty Word", e);
            }

            XWPFTableCell titleCell = headerTable.getRow(0).getCell(1);
            titleCell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
            XWPFParagraph titlePara = titleCell.getParagraphs().get(0);
            titlePara.setAlignment(ParagraphAlignment.CENTER);

            XWPFRun officeRun = titlePara.createRun();
            officeRun.setText("Office of the Controller of Examinations\n");
            officeRun.setFontSize(15);
            officeRun.setBold(true);
            officeRun.setFontFamily("Times New Roman");

            XWPFRun collegeRun = titlePara.createRun();
            collegeRun.setText(collegeName + "\n");
            collegeRun.setFontSize(13);
            collegeRun.setBold(true);
            collegeRun.setFontFamily("Times New Roman");

            XWPFRun autoRun = titlePara.createRun();
            autoRun.setText("(AUTONOMOUS)\n");
            autoRun.setFontSize(10);
            autoRun.setBold(true);
            autoRun.setFontFamily("Times New Roman");

            XWPFRun titleRun = titlePara.createRun();
            titleRun.setText("INTERNAL ASSESSMENT / END SEMESTER EXAMINATIONS\nDUTY ALLOCATION CHART");
            titleRun.setFontSize(12);
            titleRun.setBold(true);
            titleRun.setUnderline(UnderlinePatterns.SINGLE);
            titleRun.setFontFamily("Times New Roman");

            XWPFParagraph line = doc.createParagraph();
            line.setBorderBottom(Borders.SINGLE);
            line.setSpacingAfter(100);

            XWPFParagraph metaPara = doc.createParagraph();
            metaPara.setSpacingAfter(100);
            XWPFRun dateRun = metaPara.createRun();
            dateRun.setText("Date: " + session.getExamDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) + "    |    Session: " + session.getSession() + "    |    Total Invigilators: " + duties.size());
            dateRun.setBold(true);
            dateRun.setFontSize(11);
            dateRun.setFontFamily("Times New Roman");

            XWPFTable dutyTable = doc.createTable(duties.size() + 1, 7);
            dutyTable.setWidth("100%");

            String[] headers = {"S.No", "Faculty Name", "Desig.", "Dept.", "Hall Allocated", "Duty Type", "Signature"};
            for (int i = 0; i < headers.length; i++) {
                setTableCell(dutyTable.getRow(0).getCell(i), headers[i], true, ParagraphAlignment.CENTER);
            }

            int sno = 1;
            for (InvigilatorDuty d : duties) {
                XWPFTableRow r = dutyTable.getRow(sno);
                setTableCell(r.getCell(0), String.valueOf(sno), false, ParagraphAlignment.CENTER);
                setTableCell(r.getCell(1), d.getFaculty().getName(), true, ParagraphAlignment.LEFT);
                setTableCell(r.getCell(2), d.getFaculty().getDesignation() != null ? d.getFaculty().getDesignation() : "Asst.Prof", false, ParagraphAlignment.CENTER);
                setTableCell(r.getCell(3), d.getFaculty().getDepartment(), false, ParagraphAlignment.CENTER);
                setTableCell(r.getCell(4), d.getHall().getName(), true, ParagraphAlignment.CENTER);
                setTableCell(r.getCell(5), d.getDutyType(), false, ParagraphAlignment.CENTER);
                setTableCell(r.getCell(6), "", false, ParagraphAlignment.LEFT);
                sno++;
            }

            XWPFParagraph instHeader = doc.createParagraph();
            instHeader.setSpacingBefore(150);
            instHeader.setSpacingAfter(40);
            XWPFRun instTitle = instHeader.createRun();
            instTitle.setText("Instructions to Invigilators:");
            instTitle.setBold(true);
            instTitle.setUnderline(UnderlinePatterns.SINGLE);
            instTitle.setFontSize(10.5);

            String[] instructions = {
                "1. Invigilators must report to the COE / Exam Cell 20 minutes before commencement of examination.",
                "2. Mobile phones, smart watches, and unauthorized materials are strictly prohibited inside examination halls.",
                "3. Verify student hall tickets, register numbers, and answer booklet seals prior to distribution.",
                "4. Ensure strict silence and report any malpractice immediately to the Chief Superintendent / Squad."
            };

            for (String inst : instructions) {
                XWPFParagraph ip = doc.createParagraph();
                ip.setSpacingBefore(20);
                ip.setSpacingAfter(20);
                XWPFRun ir = ip.createRun();
                ir.setText(inst);
                ir.setFontSize(9.5);
                ir.setFontFamily("Times New Roman");
            }

            XWPFParagraph sigPara = doc.createParagraph();
            sigPara.setSpacingBefore(300);
            sigPara.setAlignment(ParagraphAlignment.RIGHT);
            XWPFRun sigRun = sigPara.createRun();
            sigRun.setText("CONTROLLER OF EXAMINATIONS / PRINCIPAL");
            sigRun.setBold(true);
            sigRun.setFontSize(11);
            sigRun.setFontFamily("Times New Roman");

            doc.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate duty Word document", e);
        }
    }

    private void setTableCell(XWPFTableCell cell, String text, boolean bold, ParagraphAlignment align) {
        XWPFParagraph p = cell.getParagraphs().get(0);
        p.setAlignment(align);
        p.setSpacingBefore(30);
        p.setSpacingAfter(30);
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setBold(bold);
        r.setFontSize(10);
        r.setFontFamily("Times New Roman");
    }

    @Transactional
    public void swapDuty(UUID dutyId, UUID newFacultyId) {
        InvigilatorDuty duty = dutyRepository.findById(dutyId)
                .orElseThrow(() -> new IllegalArgumentException("Duty not found: " + dutyId));

        Faculty newFaculty = facultyRepository.findById(newFacultyId)
                .orElseThrow(() -> new IllegalArgumentException("Faculty not found: " + newFacultyId));

        // EDGE CASE 5: Department Isolation Validation on Swap
        List<Allocation> allocs = allocationRepository.findByBatchIdWithDetails(duty.getBatch().getId());
        Set<String> hallDepts = allocs.stream()
                .filter(a -> a.getHall().getId().equals(duty.getHall().getId()))
                .map(a -> a.getStudent().getDepartment())
                .collect(Collectors.toSet());

        if (InvigilatorAllocationEngine.hasDeptConflict(newFaculty, hallDepts)) {
            logger.warn("Manual Duty Swap WARNING: Faculty {} ({}) has department conflict with hall {}",
                    newFaculty.getName(), newFaculty.getDepartment(), duty.getHall().getName());
        }

        duty.setFaculty(newFaculty);
        dutyRepository.save(duty);
        logger.info("Swapped duty {} to faculty {}", dutyId, newFaculty.getName());
    }
}

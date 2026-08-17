package com.exam.controller;

import com.exam.entity.AllocationBatch;
import com.exam.entity.InvigilatorDuty;
import com.exam.repository.AllocationBatchRepository;
import com.exam.service.InvigilatorDutyService;
import com.exam.service.InvigilatorDutyScheduleService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/v1/duties")
public class InvigilatorDutyController {

    private final InvigilatorDutyService dutyService;
    private final InvigilatorDutyScheduleService dutyScheduleService;
    private final AllocationBatchRepository batchRepository;

    public InvigilatorDutyController(InvigilatorDutyService dutyService,
                                      InvigilatorDutyScheduleService dutyScheduleService,
                                      AllocationBatchRepository batchRepository) {
        this.dutyService = dutyService;
        this.dutyScheduleService = dutyScheduleService;
        this.batchRepository = batchRepository;
    }

    @PostMapping("/allocate/{batchId}")
    public ResponseEntity<Map<String, Object>> allocate(
            @PathVariable UUID batchId,
            @RequestBody(required = false) Map<String, List<UUID>> requestBody) {
        List<UUID> facultyIds = requestBody != null ? requestBody.get("facultyIds") : null;
        Map<String, Object> result = dutyService.allocateInvigilators(batchId, facultyIds);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/batch/{batchId}")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getByBatch(@PathVariable UUID batchId) {
        List<InvigilatorDuty> duties = dutyService.getDutiesByBatch(batchId);
        List<Map<String, Object>> result = duties.stream().map(this::toDto).toList();
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/batch/{batchId}")
    public ResponseEntity<Void> clearAllocation(@PathVariable UUID batchId) {
        dutyService.clearDutiesByBatch(batchId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{dutyId}/attendance")
    public ResponseEntity<Void> markAttendance(
            @PathVariable UUID dutyId,
            @RequestBody Map<String, Boolean> body) {
        Boolean present = body.get("present");
        if (present == null) present = true;
        dutyService.markAttendance(dutyId, present);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/batch/{batchId}/excel")
    public void downloadExcel(@PathVariable UUID batchId, HttpServletResponse response) throws Exception {
        String filename = getFormattedFilename(batchId, "xlsx");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        dutyService.generateDutyExcel(batchId, response.getOutputStream());
    }

    @GetMapping("/batch/{batchId}/word")
    public ResponseEntity<byte[]> downloadWord(@PathVariable UUID batchId) throws Exception {
        String filename = getFormattedFilename(batchId, "docx");
        byte[] docBytes = dutyService.generateDutyWord(batchId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(org.springframework.http.MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(docBytes);
    }

    @PutMapping("/{dutyId}/swap/{newFacultyId}")
    public ResponseEntity<Void> swapDuty(@PathVariable UUID dutyId, @PathVariable UUID newFacultyId) {
        dutyService.swapDuty(dutyId, newFacultyId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/batch/{batchId}/duty-schedule-excel")
    public void downloadDutyScheduleExcel(@PathVariable UUID batchId, HttpServletResponse response) throws Exception {
        String filename = getFormattedFilename(batchId, "xlsx").replace("Duty_Chart", "Duty_Schedule");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        dutyScheduleService.generateDutyScheduleExcel(batchId, response.getOutputStream());
    }

    private String getFormattedFilename(UUID batchId, String ext) {
        Optional<AllocationBatch> batchOpt = batchRepository.findByIdWithSession(batchId);
        if (batchOpt.isPresent() && batchOpt.get().getExamSession() != null) {
            var session = batchOpt.get().getExamSession();
            String dateStr = session.getExamDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            String shift = session.getSession();
            String prefix = "INTERNAL".equalsIgnoreCase(session.getExamType()) ? "Internal_Exam" : "Semester_Exam";
            return prefix + "_Duty_Chart_" + dateStr + "_" + shift + "." + ext;
        }
        return "Exam_Duty_Chart_" + batchId + "." + ext;
    }

    private Map<String, Object> toDto(InvigilatorDuty d) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", d.getId());
        dto.put("facultyName", d.getFaculty().getName());
        dto.put("facultyId", d.getFaculty().getId());
        dto.put("employeeId", d.getFaculty().getEmployeeId());
        dto.put("facultyDepartment", d.getFaculty().getDepartment());
        dto.put("hallId", d.getHall().getId());
        dto.put("hallName", d.getHall().getName());
        dto.put("dutyType", d.getDutyType());
        dto.put("shift", d.getShift());
        dto.put("dutyDate", d.getDutyDate().toString());
        dto.put("isPresent", d.getIsPresent());
        return dto;
    }
}

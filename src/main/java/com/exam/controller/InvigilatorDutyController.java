package com.exam.controller;

import com.exam.entity.InvigilatorDuty;
import com.exam.service.InvigilatorDutyService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.util.*;

@RestController
@RequestMapping("/api/v1/duties")
public class InvigilatorDutyController {

    private final InvigilatorDutyService dutyService;

    public InvigilatorDutyController(InvigilatorDutyService dutyService) {
        this.dutyService = dutyService;
    }

    @PostMapping("/allocate/{batchId}")
    public ResponseEntity<Map<String, Object>> allocate(@PathVariable UUID batchId) {
        Map<String, Object> result = dutyService.allocateInvigilators(batchId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/batch/{batchId}")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getByBatch(@PathVariable UUID batchId) {
        List<InvigilatorDuty> duties = dutyService.getDutiesByBatch(batchId);
        List<Map<String, Object>> result = duties.stream().map(this::toDto).toList();
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{dutyId}/attendance")
    public ResponseEntity<Void> markAttendance(@PathVariable UUID dutyId,
                                                @RequestBody Map<String, Boolean> body) {
        dutyService.markAttendance(dutyId, body.getOrDefault("present", false));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/batch/{batchId}/excel")
    public void downloadExcel(@PathVariable UUID batchId, HttpServletResponse response) throws Exception {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=invigilation_duties_" + batchId + ".xlsx");
        dutyService.generateDutyExcel(batchId, response.getOutputStream());
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

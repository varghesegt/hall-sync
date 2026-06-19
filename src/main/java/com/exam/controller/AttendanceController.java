package com.exam.controller;

import com.exam.entity.StudentAttendance;
import com.exam.service.StudentAttendanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/attendance")
public class AttendanceController {

    private final StudentAttendanceService attendanceService;
    private final com.exam.service.AttendanceConsolidationService consolidationService;

    public AttendanceController(StudentAttendanceService attendanceService,
                                com.exam.service.AttendanceConsolidationService consolidationService) {
        this.attendanceService = attendanceService;
        this.consolidationService = consolidationService;
    }

    @GetMapping("/batch/{batchId}/export-absentees")
    public void exportAbsenteeReport(@PathVariable UUID batchId,
                                     jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"Absentee_Report_" + batchId + ".xlsx\"");
        consolidationService.generateAbsenteeReport(batchId, response.getOutputStream());
    }

    @PostMapping("/batch/{batchId}/init")
    public ResponseEntity<Map<String, Object>> initialize(@PathVariable UUID batchId) {
        int count = attendanceService.initializeAttendance(batchId);
        return ResponseEntity.ok(Map.of("initialized", count));
    }

    @PostMapping("/batch/{batchId}/mark")
    public ResponseEntity<Void> markBulk(@PathVariable UUID batchId,
                                          @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        Map<String, Boolean> rawPresence = (Map<String, Boolean>) body.get("attendance");
        UUID markedBy = body.containsKey("markedBy")
                ? UUID.fromString((String) body.get("markedBy")) : null;

        Map<UUID, Boolean> studentPresence = new LinkedHashMap<>();
        for (Map.Entry<String, Boolean> entry : rawPresence.entrySet()) {
            studentPresence.put(UUID.fromString(entry.getKey()), entry.getValue());
        }

        attendanceService.markBulkAttendance(batchId, studentPresence, markedBy);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/batch/{batchId}")
    public ResponseEntity<List<Map<String, Object>>> getByBatch(@PathVariable UUID batchId) {
        List<StudentAttendance> records = attendanceService.getAttendanceByBatch(batchId);
        List<Map<String, Object>> result = records.stream().map(this::toDto).toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/batch/{batchId}/stats")
    public ResponseEntity<Map<String, Object>> getStats(@PathVariable UUID batchId) {
        return ResponseEntity.ok(attendanceService.getAttendanceStats(batchId));
    }

    private Map<String, Object> toDto(StudentAttendance a) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", a.getId());
        dto.put("studentId", a.getStudent().getId());
        dto.put("studentName", a.getStudent().getName());
        dto.put("studentRegNo", a.getStudent().getRegisterNumber());
        dto.put("studentDepartment", a.getStudent().getDepartment());
        dto.put("hallId", a.getHall().getId());
        dto.put("hallName", a.getHall().getName());
        dto.put("isPresent", a.getIsPresent());
        dto.put("markedAt", a.getMarkedAt() != null ? a.getMarkedAt().toString() : null);
        return dto;
    }
}

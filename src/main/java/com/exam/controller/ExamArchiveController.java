package com.exam.controller;

import com.exam.entity.ExamArchive;
import com.exam.service.ExamArchiveService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/v1/archives")
public class ExamArchiveController {

    private final ExamArchiveService archiveService;

    public ExamArchiveController(ExamArchiveService archiveService) {
        this.archiveService = archiveService;
    }

    @PostMapping("/batch/{batchId}")
    public ResponseEntity<Map<String, Object>> archive(@PathVariable UUID batchId,
                                                        @RequestBody(required = false) Map<String, String> body) {
        String archivedBy = body != null ? body.getOrDefault("archivedBy", "system") : "system";
        ExamArchive archive = archiveService.archiveBatch(batchId, archivedBy);
        return ResponseEntity.ok(toDto(archive));
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAll(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        List<ExamArchive> archives;
        if (from != null && to != null) {
            archives = archiveService.getByDateRange(LocalDate.parse(from), LocalDate.parse(to));
        } else {
            archives = archiveService.getAllArchives();
        }
        return ResponseEntity.ok(archives.stream().map(this::toDto).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable UUID id) {
        return archiveService.getById(id)
                .map(a -> ResponseEntity.ok(toDto(a)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/download/{type}")
    public ResponseEntity<byte[]> downloadSnapshot(@PathVariable UUID id, @PathVariable String type) {
        ExamArchive archive = archiveService.getById(id)
                .orElseThrow(() -> new IllegalArgumentException("Archive not found"));

        byte[] data;
        String filename;
        switch (type.toLowerCase()) {
            case "seating":
                data = archive.getSeatingPlanSnapshot();
                filename = "seating_plan_" + archive.getExamDate() + ".pdf";
                break;
            case "attendance":
                data = archive.getAttendanceSnapshot();
                filename = "attendance_" + archive.getExamDate() + ".pdf";
                break;
            case "duty":
                data = archive.getDutySheetSnapshot();
                filename = "duty_sheet_" + archive.getExamDate() + ".pdf";
                break;
            default:
                return ResponseEntity.badRequest().build();
        }

        if (data == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .body(data);
    }

    private Map<String, Object> toDto(ExamArchive a) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", a.getId());
        dto.put("examType", a.getExamType());
        dto.put("examDate", a.getExamDate().toString());
        dto.put("session", a.getSession());
        dto.put("totalStudents", a.getTotalStudents());
        dto.put("totalHalls", a.getTotalHalls());
        dto.put("totalInvigilators", a.getTotalInvigilators());
        dto.put("totalAbsentees", a.getTotalAbsentees());
        dto.put("totalMalpractice", a.getTotalMalpractice());
        dto.put("archivedAt", a.getArchivedAt().toString());
        dto.put("archivedBy", a.getArchivedBy());
        dto.put("hasSeatingPlan", a.getSeatingPlanSnapshot() != null);
        dto.put("hasAttendance", a.getAttendanceSnapshot() != null);
        dto.put("hasDutySheet", a.getDutySheetSnapshot() != null);
        return dto;
    }
}

package com.exam.controller;

import com.exam.entity.*;
import com.exam.repository.*;
import com.exam.service.MalpracticeService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api/v1/malpractice")
public class MalpracticeController {

    private final MalpracticeService malpracticeService;
    private final AllocationBatchRepository batchRepository;
    private final StudentRepository studentRepository;
    private final HallRepository hallRepository;
    private final FacultyRepository facultyRepository;

    public MalpracticeController(MalpracticeService malpracticeService,
                                  AllocationBatchRepository batchRepository,
                                  StudentRepository studentRepository,
                                  HallRepository hallRepository,
                                  FacultyRepository facultyRepository) {
        this.malpracticeService = malpracticeService;
        this.batchRepository = batchRepository;
        this.studentRepository = studentRepository;
        this.hallRepository = hallRepository;
        this.facultyRepository = facultyRepository;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAll() {
        List<MalpracticeCase> cases = malpracticeService.getAllCases();
        return ResponseEntity.ok(cases.stream().map(this::toDto).toList());
    }

    @GetMapping("/batch/{batchId}")
    public ResponseEntity<List<Map<String, Object>>> getByBatch(@PathVariable UUID batchId) {
        List<MalpracticeCase> cases = malpracticeService.getCasesByBatch(batchId);
        return ResponseEntity.ok(cases.stream().map(this::toDto).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable UUID id) {
        return malpracticeService.getCaseById(id)
                .map(c -> ResponseEntity.ok(toDto(c)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, String> body) {
        AllocationBatch batch = body.containsKey("batchId")
                ? batchRepository.findById(UUID.fromString(body.get("batchId"))).orElse(null) : null;
        Student student = body.containsKey("studentId")
                ? studentRepository.findById(UUID.fromString(body.get("studentId"))).orElse(null) : null;
        Hall hall = body.containsKey("hallId")
                ? hallRepository.findById(body.get("hallId")).orElse(null) : null;
        Faculty reporter = body.containsKey("reportedBy")
                ? facultyRepository.findById(UUID.fromString(body.get("reportedBy"))).orElse(null) : null;

        MalpracticeCase mc = new MalpracticeCase(
                UUID.randomUUID(), batch, student, hall, reporter,
                body.getOrDefault("caseType", "OTHER"),
                body.getOrDefault("description", ""),
                body.getOrDefault("severity", "MODERATE"));

        mc = malpracticeService.createCase(mc);
        return ResponseEntity.ok(toDto(mc));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateStatus(@PathVariable UUID id,
                                                             @RequestBody Map<String, String> body) {
        MalpracticeCase mc = malpracticeService.updateStatus(id,
                body.get("status"), body.getOrDefault("actionTaken", null));
        return ResponseEntity.ok(toDto(mc));
    }

    @PostMapping("/{id}/evidence")
    public ResponseEntity<Map<String, Object>> uploadEvidence(@PathVariable UUID id,
                                                               @RequestParam("file") MultipartFile file) {
        MalpracticeCase mc = malpracticeService.attachEvidence(id, file);
        return ResponseEntity.ok(toDto(mc));
    }

    @GetMapping("/{id}/evidence")
    public ResponseEntity<byte[]> downloadEvidence(@PathVariable UUID id) {
        MalpracticeCase mc = malpracticeService.getCaseById(id)
                .orElseThrow(() -> new IllegalArgumentException("Case not found"));
        if (mc.getEvidenceData() == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        mc.getEvidenceContentType() != null ? mc.getEvidenceContentType() : "application/octet-stream"))
                .header("Content-Disposition", "attachment; filename=\"" + mc.getEvidenceFilename() + "\"")
                .body(mc.getEvidenceData());
    }

    @GetMapping("/trends")
    public ResponseEntity<Map<String, Object>> getTrends(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        java.time.LocalDateTime fromDate = from != null
                ? java.time.LocalDate.parse(from).atStartOfDay()
                : java.time.LocalDateTime.now().minusMonths(6);
        java.time.LocalDateTime toDate = to != null
                ? java.time.LocalDate.parse(to).atTime(23, 59, 59)
                : java.time.LocalDateTime.now();
        return ResponseEntity.ok(malpracticeService.getTrends(fromDate, toDate));
    }

    private Map<String, Object> toDto(MalpracticeCase mc) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", mc.getId());
        dto.put("caseType", mc.getCaseType());
        dto.put("description", mc.getDescription());
        dto.put("severity", mc.getSeverity());
        dto.put("status", mc.getStatus());
        dto.put("actionTaken", mc.getActionTaken());
        dto.put("reportedAt", mc.getReportedAt() != null ? mc.getReportedAt().toString() : null);
        dto.put("resolvedAt", mc.getResolvedAt() != null ? mc.getResolvedAt().toString() : null);
        dto.put("hasEvidence", mc.getEvidenceData() != null);
        dto.put("evidenceFilename", mc.getEvidenceFilename());

        if (mc.getStudent() != null) {
            dto.put("studentName", mc.getStudent().getName());
            dto.put("studentRegNo", mc.getStudent().getRegisterNumber());
            dto.put("studentDepartment", mc.getStudent().getDepartment());
        }
        if (mc.getHall() != null) {
            dto.put("hallName", mc.getHall().getName());
        }
        if (mc.getReportedBy() != null) {
            dto.put("reportedByName", mc.getReportedBy().getName());
        }
        return dto;
    }
}

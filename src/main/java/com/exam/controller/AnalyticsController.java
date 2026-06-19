package com.exam.controller;

import com.exam.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.exam.service.RemunerationReportService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final InvigilatorDutyRepository dutyRepository;
    private final MalpracticeCaseRepository malpracticeRepository;
    private final FacultyRepository facultyRepository;
    private final ExamArchiveRepository archiveRepository;
    private final AllocationBatchRepository batchRepository;
    private final QuestionPaperTrackerRepository qpTrackerRepository;
    private final RemunerationReportService remunerationReportService;
    private final com.exam.service.CommandCenterService commandCenterService;

    public AnalyticsController(InvigilatorDutyRepository dutyRepository,
                                MalpracticeCaseRepository malpracticeRepository,
                                FacultyRepository facultyRepository,
                                ExamArchiveRepository archiveRepository,
                                AllocationBatchRepository batchRepository,
                                QuestionPaperTrackerRepository qpTrackerRepository,
                                RemunerationReportService remunerationReportService,
                                com.exam.service.CommandCenterService commandCenterService) {
        this.dutyRepository = dutyRepository;
        this.malpracticeRepository = malpracticeRepository;
        this.facultyRepository = facultyRepository;
        this.archiveRepository = archiveRepository;
        this.batchRepository = batchRepository;
        this.qpTrackerRepository = qpTrackerRepository;
        this.remunerationReportService = remunerationReportService;
        this.commandCenterService = commandCenterService;
    }

    @GetMapping("/command-center")
    public ResponseEntity<Map<String, Object>> getCommandCenterStats(
            @RequestParam(required = false) String date) {
        LocalDate targetDate = date != null ? LocalDate.parse(date) : LocalDate.now();
        return ResponseEntity.ok(commandCenterService.getCommandCenterStats(targetDate));
    }

    @GetMapping("/remuneration/export")
    public void exportRemunerationReport(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {

        LocalDate fromDate = from != null ? LocalDate.parse(from) : LocalDate.now().minusMonths(6);
        LocalDate toDate = to != null ? LocalDate.parse(to) : LocalDate.now();

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"Remuneration_Report_" + fromDate + "_to_" + toDate + ".xlsx\"");
        
        remunerationReportService.generateRemunerationExcel(fromDate, toDate, response.getOutputStream());
    }

    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getOverview(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        LocalDate fromDate = from != null ? LocalDate.parse(from) : LocalDate.now().minusMonths(3);
        LocalDate toDate = to != null ? LocalDate.parse(to) : LocalDate.now();

        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("totalFaculty", facultyRepository.countByIsActiveTrue());
        overview.put("totalArchives", archiveRepository.findByExamDateBetweenOrderByExamDateAsc(fromDate, toDate).size());

        return ResponseEntity.ok(overview);
    }

    @GetMapping("/workload")
    public ResponseEntity<List<Map<String, Object>>> getWorkload(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        LocalDate fromDate = from != null ? LocalDate.parse(from) : LocalDate.now().minusMonths(3);
        LocalDate toDate = to != null ? LocalDate.parse(to) : LocalDate.now();

        List<Object[]> data = dutyRepository.getWorkloadByFacultyBetween(fromDate, toDate);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : data) {
            Map<String, Object> entry = new LinkedHashMap<>();
            UUID facultyId = (UUID) row[0];
            entry.put("facultyId", facultyId);
            entry.put("dutyCount", row[1]);
            facultyRepository.findById(facultyId).ifPresent(f -> {
                entry.put("facultyName", f.getName());
                entry.put("department", f.getDepartment());
            });
            result.add(entry);
        }

        result.sort(Comparator.comparingLong(m -> -((Long) m.get("dutyCount"))));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/department-distribution")
    public ResponseEntity<List<Map<String, Object>>> getDepartmentDistribution(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        LocalDate fromDate = from != null ? LocalDate.parse(from) : LocalDate.now().minusMonths(3);
        LocalDate toDate = to != null ? LocalDate.parse(to) : LocalDate.now();

        List<Object[]> data = dutyRepository.getWorkloadByFacultyBetween(fromDate, toDate);
        Map<String, Long> deptMap = new LinkedHashMap<>();
        Map<String, Long> deptFacultyCount = new LinkedHashMap<>();

        for (Object[] row : data) {
            UUID facultyId = (UUID) row[0];
            long count = (Long) row[1];
            facultyRepository.findById(facultyId).ifPresent(f -> {
                String dept = f.getDepartment();
                deptMap.merge(dept, count, Long::sum);
                deptFacultyCount.merge(dept, 1L, Long::sum);
            });
        }

        List<Map<String, Object>> result = new ArrayList<>();
        deptMap.forEach((dept, totalDuties) -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("department", dept);
            entry.put("totalDuties", totalDuties);
            entry.put("facultyCount", deptFacultyCount.getOrDefault(dept, 0L));
            entry.put("avgDuties", deptFacultyCount.containsKey(dept) ?
                    Math.round((double) totalDuties / deptFacultyCount.get(dept) * 10) / 10.0 : 0);
            result.add(entry);
        });

        result.sort(Comparator.comparingDouble(m -> (Double) m.get("standardDeviation")));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/management-dashboard")
    public ResponseEntity<Map<String, Object>> getManagementDashboard() {
        Map<String, Object> stats = new LinkedHashMap<>();
        
        long totalArchives = archiveRepository.count();
        long totalFaculty = facultyRepository.countByIsActiveTrue();
        long totalActiveDuties = dutyRepository.count(); // Approximation of active workload
        long totalBatches = batchRepository.count();
        
        // Audit Readiness: Percentage of total active batches that are archived
        // Usually archives are less than total batches. 
        double auditReadiness = totalBatches == 0 ? 100.0 : Math.min(100.0, ((double) totalArchives / totalBatches) * 100.0);
        
        // Compliance Score: Base 100, minus deductions for malpractices or unarchived batches
        long totalMalpractices = malpracticeRepository.count();
        double complianceScore = Math.max(0.0, auditReadiness - (totalMalpractices * 2.5)); // Arbitrary formula for demo
        
        // Faculty utilization: % of active faculty who have at least one duty assigned this month
        // For demo, just an approximation
        double facultyUtilization = totalFaculty == 0 ? 0.0 : Math.min(100.0, ((double) totalActiveDuties / (totalFaculty * 3)) * 100.0);

        stats.put("examsConducted", totalArchives);
        stats.put("facultyUtilization", Math.round(facultyUtilization * 100.0) / 100.0);
        stats.put("auditReadiness", Math.round(auditReadiness * 100.0) / 100.0);
        stats.put("complianceScore", Math.round(complianceScore * 100.0) / 100.0);
        
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/lifecycle/{batchId}")
    public ResponseEntity<Map<String, Object>> getExamLifecycle(@PathVariable UUID batchId) {
        Map<String, Object> lifecycle = new LinkedHashMap<>();
        
        boolean batchExists = batchRepository.existsById(batchId);
        if (!batchExists) {
            return ResponseEntity.notFound().build();
        }

        com.exam.entity.AllocationBatch batch = batchRepository.findById(batchId).orElse(null);
        boolean isArchived = archiveRepository.existsByBatchId(batchId);
        
        // Before Exam
        long totalQPs = qpTrackerRepository.countByExamSessionId(batch.getExamSession().getId());
        long receivedQPs = qpTrackerRepository.countByExamSessionIdAndStatus(batch.getExamSession().getId(), "RECEIVED");
        lifecycle.put("qpReady", totalQPs > 0 && receivedQPs == totalQPs);
        
        long dutiesCount = dutyRepository.findByBatchIdOrderByHallIdAsc(batchId).size();
        lifecycle.put("facultyReady", dutiesCount > 0);
        lifecycle.put("hallReady", "ACTIVE".equals(batch.getStatus().name()) || "COMPLETED".equals(batch.getStatus().name()));
        
        // During Exam
        // Assuming attendance is complete if batch is COMPLETED or archived
        lifecycle.put("attendanceComplete", isArchived || "COMPLETED".equals(batch.getStatus().name()));
        long malpractices = malpracticeRepository.count(); // Simplification: count all, or filter by session
        lifecycle.put("malpracticesLogged", malpractices);
        
        // After Exam
        lifecycle.put("isArchived", isArchived);
        lifecycle.put("complianceMet", isArchived && malpractices == 0); // Example compliance logic
        
        return ResponseEntity.ok(lifecycle);
    }

    @GetMapping("/fairness-index")
    public ResponseEntity<Map<String, Object>> getFairnessIndex(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        LocalDate fromDate = from != null ? LocalDate.parse(from) : LocalDate.now().minusMonths(3);
        LocalDate toDate = to != null ? LocalDate.parse(to) : LocalDate.now();

        List<Object[]> data = dutyRepository.getWorkloadByFacultyBetween(fromDate, toDate);
        if (data.isEmpty()) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("fairnessScore", 100.0);
            empty.put("mean", 0);
            empty.put("stdDev", 0);
            empty.put("overloaded", List.of());
            empty.put("underloaded", List.of());
            return ResponseEntity.ok(empty);
        }

        List<Long> counts = new ArrayList<>();
        Map<UUID, Long> facultyCounts = new LinkedHashMap<>();
        for (Object[] row : data) {
            UUID fid = (UUID) row[0];
            long cnt = (Long) row[1];
            counts.add(cnt);
            facultyCounts.put(fid, cnt);
        }

        double mean = counts.stream().mapToLong(l -> l).average().orElse(0);
        double variance = counts.stream().mapToDouble(c -> Math.pow(c - mean, 2)).average().orElse(0);
        double stdDev = Math.sqrt(variance);
        double fairnessScore = mean > 0 ? Math.max(0, 100 - (stdDev / mean * 100)) : 100;

        double overloadThreshold = mean * 1.5;
        double underloadThreshold = mean * 0.5;

        List<Map<String, Object>> overloaded = new ArrayList<>();
        List<Map<String, Object>> underloaded = new ArrayList<>();

        facultyCounts.forEach((fid, count) -> {
            facultyRepository.findById(fid).ifPresent(f -> {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("name", f.getName());
                entry.put("department", f.getDepartment());
                entry.put("dutyCount", count);
                if (count > overloadThreshold) overloaded.add(entry);
                if (count < underloadThreshold) underloaded.add(entry);
            });
        });

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("fairnessScore", Math.round(fairnessScore * 10) / 10.0);
        result.put("mean", Math.round(mean * 10) / 10.0);
        result.put("stdDev", Math.round(stdDev * 10) / 10.0);
        result.put("overloaded", overloaded);
        result.put("underloaded", underloaded);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/malpractice-trends")
    public ResponseEntity<Map<String, Object>> getMalpracticeTrends(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        LocalDateTime fromDate = from != null
                ? LocalDate.parse(from).atStartOfDay()
                : LocalDateTime.now().minusMonths(6);
        LocalDateTime toDate = to != null
                ? LocalDate.parse(to).atTime(23, 59, 59)
                : LocalDateTime.now();

        List<Object[]> byType = malpracticeRepository.getCaseTypeTrends(fromDate, toDate);
        Map<String, Long> typeMap = new LinkedHashMap<>();
        for (Object[] row : byType) typeMap.put((String) row[0], (Long) row[1]);

        List<Object[]> bySeverity = malpracticeRepository.getSeverityDistribution(fromDate, toDate);
        Map<String, Long> severityMap = new LinkedHashMap<>();
        for (Object[] row : bySeverity) severityMap.put((String) row[0], (Long) row[1]);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("byType", typeMap);
        result.put("bySeverity", severityMap);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/exam-history")
    public ResponseEntity<List<Map<String, Object>>> getExamHistory(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        LocalDate fromDate = from != null ? LocalDate.parse(from) : LocalDate.now().minusYears(1);
        LocalDate toDate = to != null ? LocalDate.parse(to) : LocalDate.now();

        var archives = archiveRepository.findByExamDateBetweenOrderByExamDateAsc(fromDate, toDate);
        List<Map<String, Object>> result = new ArrayList<>();
        for (var a : archives) {
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
            result.add(dto);
        }
        return ResponseEntity.ok(result);
    }
}

package com.exam.service;

import com.exam.engine.AllocationEngine;
import com.exam.engine.model.AllocationRequest;
import com.exam.engine.model.AllocationResult;
import com.exam.entity.*;
import com.exam.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AllocationTxService {

    private static final Logger logger = LoggerFactory.getLogger(AllocationTxService.class);

    private final ExamSessionRepository examSessionRepo;
    private final StudentRepository studentRepo;
    private final HallRepository hallRepo;
    private final AllocationBatchRepository batchRepo;
    private final AllocationRepository allocationRepo;
    private final AuditLogRepository auditRepo;

    public AllocationTxService(ExamSessionRepository examSessionRepo, StudentRepository studentRepo, HallRepository hallRepo, AllocationBatchRepository batchRepo, AllocationRepository allocationRepo, AuditLogRepository auditRepo) {
        this.examSessionRepo = examSessionRepo;
        this.studentRepo = studentRepo;
        this.hallRepo = hallRepo;
        this.batchRepo = batchRepo;
        this.allocationRepo = allocationRepo;
        this.auditRepo = auditRepo;
    }

    /**
     * Step 1: Create a RUNNING batch BEFORE allocation starts.
     * Runs in its own REQUIRES_NEW transaction so it is immediately visible
     * to any GET /batches/latest query, even while allocation is still running.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AllocationBatch createRunningBatch(UUID examSessionId, UUID allocationRequestId) {
        // LOCK: Prevent another PC from starting allocation for this session simultaneously
        ExamSession session = examSessionRepo.findByIdWithLock(examSessionId)
                .orElseThrow(() -> new IllegalArgumentException("Exam Session not found: " + examSessionId));

        Integer version = batchRepo.findMaxVersionByExamSessionId(examSessionId) + 1;
        AllocationBatch batch = new AllocationBatch(
                UUID.randomUUID(), session, allocationRequestId, BatchStatus.RUNNING, version);
        batchRepo.save(batch);

        auditRepo.save(new AuditLog(UUID.randomUUID(), session, batch, allocationRequestId,
                AuditEventType.ALLOCATION_STARTED, AuditSeverity.INFO,
                "Allocation started (batch v" + version + ")", null));

        logger.info("[{}] RUNNING batch created: batchId={}, version={}",
                allocationRequestId, batch.getId(), version);
        return batch;
    }

    /**
     * Step 2: Execute the constraint engine and persist results.
     * On success: RUNNING → ACTIVE.
     * Runs in its own REQUIRES_NEW transaction — if this fails, the batch
     * stays RUNNING and will be marked FAILED by the caller.
     *
     * @param selectedRooms list of room IDs selected by the user in the frontend
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AllocationBatch executeAllocation(UUID batchId, UUID examSessionId, String requestedBy, UUID allocationRequestId, List<String> selectedRooms) {
        AllocationBatch batch = batchRepo.findById(batchId)
                .orElseThrow(() -> new IllegalStateException("Running batch not found: " + batchId));

        ExamSession session = examSessionRepo.findById(examSessionId)
                .orElseThrow(() -> new IllegalArgumentException("Exam Session not found: " + examSessionId));

        List<Student> students = studentRepo.findByExamSessionId(examSessionId);

        // *** KEY CHANGE: Only fetch halls that the user selected ***
        List<Hall> halls = hallRepo.findAllById(selectedRooms);

        logger.info("[{}] Allocation inputs: students={}, halls={} (selected from {} requested)",
                allocationRequestId, students.size(), halls.size(), selectedRooms.size());

        // Warn if some selected room IDs don't exist in the DB
        if (halls.size() < selectedRooms.size()) {
            Set<String> foundIds = halls.stream().map(Hall::getId).collect(Collectors.toSet());
            List<String> missing = selectedRooms.stream().filter(id -> !foundIds.contains(id)).toList();
            logger.warn("[{}] {} selected rooms not found in DB: {}", allocationRequestId, missing.size(), missing);
        }

        if (students.isEmpty()) {
            throw new IllegalStateException("No students found for session: " + examSessionId);
        }
        if (halls.isEmpty()) {
            throw new IllegalStateException("No matching halls found for selected rooms: " + selectedRooms + ". Add halls to the database before allocating.");
        }

        List<String> regNumbers = students.stream().map(Student::getRegisterNumber).collect(Collectors.toList());
        long startHistory = System.currentTimeMillis();
        List<Allocation> historyAllocs = allocationRepo.findSeasonHistory(regNumbers, session.getSeasonId(), examSessionId);
        long endHistory = System.currentTimeMillis();
        logger.debug("[{}] History fetch: {} allocations found in {}ms", allocationRequestId, historyAllocs.size(), (endHistory - startHistory));

        Map<String, Set<String>> seasonHistory = new HashMap<>();
        Map<String, Set<String>> positionHistory = new HashMap<>();
        Set<UUID> distinctPreviousSessions = new HashSet<>();
        for (Allocation a : historyAllocs) {
            String regNo = a.getStudent().getRegisterNumber();
            // Hall rotation history (existing)
            seasonHistory.computeIfAbsent(regNo, k -> new HashSet<>())
                    .add(a.getHall().getId());
            // Position rotation history (tracks Hall:Row:Col)
            if (a.getSeatRow() != null && a.getSeatCol() != null) {
                positionHistory.computeIfAbsent(regNo, k -> new HashSet<>())
                        .add(a.getHall().getId() + ":" + a.getSeatRow() + ":" + a.getSeatCol());
            }
            // Count distinct previous exam sessions for row rotation
            if (a.getBatch() != null && a.getBatch().getExamSession() != null) {
                distinctPreviousSessions.add(a.getBatch().getExamSession().getId());
            }
        }
        int seasonSessionIndex = distinctPreviousSessions.size(); // 0 for first exam, 1 for second, etc.
        logger.info("[{}] Season session index: {} (row offset will be {})", allocationRequestId, seasonSessionIndex, (seasonSessionIndex * 2) % 5);

        List<com.exam.engine.model.Student> engineStudents = students.stream()
                .map(s -> new com.exam.engine.model.Student(
                    s.getRegisterNumber(), 
                    s.getDepartment(), 
                    s.getSubjectCode() != null ? s.getSubjectCode().trim().toUpperCase() : "N/A",
                    s.getSemester(),
                    s.getRegulation()
                ))
                .toList();
        
        // Diagnostic: Log first few students to verify subject code normalization
        if (!engineStudents.isEmpty()) {
            logger.info("[{}] DIAGNOSTIC: First student subject code: '{}'", allocationRequestId, engineStudents.get(0).subjectCode());
        }

        List<com.exam.engine.model.Hall> engineHalls = halls.stream()
                .map(h -> new com.exam.engine.model.Hall(h.getId(), 
                    h.getCapacity(),
                    (h.getSemRows() != null ? h.getSemRows() : 5),
                    (h.getSemCols() != null ? h.getSemCols() : 5)
                ))
                .toList();

        logger.info("[{}] Initializing AllocationEngine for {} students and {} halls", allocationRequestId, engineStudents.size(), engineHalls.size());
        AllocationRequest request = new AllocationRequest(engineStudents, engineHalls, seasonHistory, positionHistory, seasonSessionIndex, examSessionId.toString());
        AllocationEngine engine = new AllocationEngine();
        try {
            AllocationResult result = engine.allocate(request);
            logger.info("[{}] Engine completed: {} assignments, {} violations",
                    allocationRequestId, result.assignments().size(), result.violations().size());

            Map<String, Student> studentMap = students.stream().collect(Collectors.toMap(Student::getRegisterNumber, s -> s));
            Map<String, Hall> hallMap = halls.stream().collect(Collectors.toMap(Hall::getId, h -> h));

            List<Allocation> newAllocations = new ArrayList<>();
            for (com.exam.engine.model.SeatAssignment a : result.assignments()) {
                Student s = studentMap.get(a.registerNumber());
                Hall h = hallMap.get(a.hallId());

                if (s != null && h != null) {
                    newAllocations.add(new Allocation(
                        UUID.randomUUID(), 
                        batch, 
                        s, 
                        h, 
                        a.row(), 
                        a.col(),
                        a.allocationRiskScore()
                    ));
                } else {
                    logger.error("[{}] CRITICAL: Could not resolve student {} or hall {} for persistence", 
                            allocationRequestId, a.registerNumber(), a.hallId());
                }
            }

            if (newAllocations.isEmpty()) {
                logger.error("[{}] No allocations generated by engine", allocationRequestId);
                throw new IllegalStateException("No allocations generated — invalid execution");
            }

            // RUNNING → ACTIVE
            logger.info("[{}] PERSISTING {} allocations to database...", allocationRequestId, newAllocations.size());
            long startSave = System.currentTimeMillis();
            
            // 1. Mark status ACTIVE
            batch.setStatus(BatchStatus.ACTIVE);
            batchRepo.save(batch);
            
            // 2. Clear previous active batches
            logger.debug("[{}] Superseding old batches...", allocationRequestId);
            batchRepo.markOthersSuperseded(examSessionId, batch.getId());
            
            // 3. Save all allocations
            logger.info("[{}] Saving {} allocations (batch size=50)...", allocationRequestId, newAllocations.size());
            allocationRepo.saveAll(newAllocations);
            
            long endSave = System.currentTimeMillis();
            logger.info("[{}] PERSISTENCE COMPLETED in {}ms", allocationRequestId, (endSave - startSave));

            List<AuditLog> violationLogs = result.violations().stream()
                    .map(v -> new AuditLog(UUID.randomUUID(), session, batch, allocationRequestId, AuditEventType.valueOf(v.type().name()), AuditSeverity.WARNING, v.message(), null))
                    .toList();
            auditRepo.saveAll(violationLogs);

            auditRepo.save(new AuditLog(UUID.randomUUID(), session, batch, allocationRequestId,
                    AuditEventType.ALLOCATION_COMPLETED, AuditSeverity.INFO,
                    "Allocation completed: " + result.assignments().size() + " seats assigned across " + halls.size() + " rooms", null));

            return batch;
        } catch (Exception e) {
            logger.error("[{}] CRITICAL ERROR in executeAllocation: {}", allocationRequestId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Step 3 (failure path): Mark an existing batch as FAILED and log the reason.
     * Runs in its own REQUIRES_NEW transaction so the failure is persisted
     * even if the outer operation is rolling back.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markBatchFailed(UUID batchId, UUID allocationRequestId, String errorMessage) {
        AllocationBatch batch = batchRepo.findById(batchId).orElse(null);
        if (batch == null) {
            logger.error("[{}] Cannot mark batch FAILED — batch not found: {}", allocationRequestId, batchId);
            return;
        }

        batch.setStatus(BatchStatus.FAILED);
        batchRepo.save(batch);

        // batch.getExamSession() is lazy but we are inside a transaction — safe to access
        ExamSession session = batch.getExamSession();
        auditRepo.save(new AuditLog(UUID.randomUUID(), session, batch, allocationRequestId,
                AuditEventType.PREFLIGHT_FAILED, AuditSeverity.ERROR,
                "Allocation failed: " + errorMessage, null));

        logger.info("[{}] Batch {} marked as FAILED: {}", allocationRequestId, batchId, errorMessage);
    }
}

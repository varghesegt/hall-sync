package com.exam.service.internal;

import com.exam.engine.internal.InternalAllocationEngine;
import com.exam.engine.model.*;
import com.exam.entity.*;
import com.exam.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Transactional service for Internal Exam allocation.
 * Same lifecycle as AllocationTxService but uses InternalAllocationEngine (7×6 grid).
 */
@Service
public class InternalAllocationTxService {

    private static final Logger logger = LoggerFactory.getLogger(InternalAllocationTxService.class);

    private final ExamSessionRepository examSessionRepo;
    private final StudentRepository studentRepo;
    private final HallRepository hallRepo;
    private final AllocationBatchRepository batchRepo;
    private final AllocationRepository allocationRepo;
    private final AuditLogRepository auditRepo;

    public InternalAllocationTxService(ExamSessionRepository examSessionRepo, StudentRepository studentRepo,
                                        HallRepository hallRepo, AllocationBatchRepository batchRepo,
                                        AllocationRepository allocationRepo, AuditLogRepository auditRepo) {
        this.examSessionRepo = examSessionRepo;
        this.studentRepo = studentRepo;
        this.hallRepo = hallRepo;
        this.batchRepo = batchRepo;
        this.allocationRepo = allocationRepo;
        this.auditRepo = auditRepo;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AllocationBatch createRunningBatch(UUID examSessionId, UUID allocationRequestId) {
        ExamSession session = examSessionRepo.findByIdWithLock(examSessionId)
                .orElseThrow(() -> new IllegalArgumentException("Exam Session not found: " + examSessionId));

        Integer version = batchRepo.findMaxVersionByExamSessionId(examSessionId) + 1;
        AllocationBatch batch = new AllocationBatch(UUID.randomUUID(), session, allocationRequestId, BatchStatus.RUNNING, version);
        batchRepo.save(batch);

        auditRepo.save(new AuditLog(UUID.randomUUID(), session, batch, allocationRequestId,
                AuditEventType.ALLOCATION_STARTED, AuditSeverity.INFO,
                "Internal allocation started (batch v" + version + ")", null));

        logger.info("[{}] Internal RUNNING batch created: batchId={}, version={}", allocationRequestId, batch.getId(), version);
        return batch;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AllocationBatch executeAllocation(UUID batchId, UUID examSessionId, String requestedBy,
                                              UUID allocationRequestId, List<String> selectedRooms) {
        AllocationBatch batch = batchRepo.findById(batchId)
                .orElseThrow(() -> new IllegalStateException("Running batch not found: " + batchId));

        ExamSession session = examSessionRepo.findById(examSessionId)
                .orElseThrow(() -> new IllegalArgumentException("Exam Session not found: " + examSessionId));

        List<com.exam.entity.Student> students = studentRepo.findByExamSessionId(examSessionId);
        List<com.exam.entity.Hall> halls = hallRepo.findAllById(selectedRooms);

        logger.info("[{}] Internal allocation inputs: students={}, halls={}", allocationRequestId, students.size(), halls.size());

        if (students.isEmpty()) throw new IllegalStateException("No students found for session: " + examSessionId);
        if (halls.isEmpty()) throw new IllegalStateException("No matching halls found for selected rooms");

        // Build season history
        List<String> regNumbers = students.stream().map(com.exam.entity.Student::getRegisterNumber).collect(Collectors.toList());
        List<Allocation> historyAllocs = allocationRepo.findSeasonHistory(regNumbers, session.getSeasonId(), examSessionId);

        Map<String, Set<String>> seasonHistory = new HashMap<>();
        Map<String, Set<String>> positionHistory = new HashMap<>();
        Set<UUID> distinctPreviousSessions = new HashSet<>();

        for (Allocation a : historyAllocs) {
            String regNo = a.getStudent().getRegisterNumber();
            seasonHistory.computeIfAbsent(regNo, k -> new HashSet<>()).add(a.getHall().getId());
            if (a.getSeatRow() != null && a.getSeatCol() != null) {
                positionHistory.computeIfAbsent(regNo, k -> new HashSet<>())
                        .add(a.getHall().getId() + ":" + a.getSeatRow() + ":" + a.getSeatCol());
            }
            if (a.getBatch() != null && a.getBatch().getExamSession() != null) {
                distinctPreviousSessions.add(a.getBatch().getExamSession().getId());
            }
        }
        int seasonSessionIndex = distinctPreviousSessions.size();

        // Build engine models
        List<com.exam.engine.model.Student> engineStudents = students.stream()
                .map(s -> new com.exam.engine.model.Student(
                        s.getRegisterNumber(), s.getDepartment(),
                        s.getSubjectCode() != null ? s.getSubjectCode().trim().toUpperCase() : "N/A",
                        s.getSemester(), s.getRegulation()))
                .collect(Collectors.toList());

        List<com.exam.engine.model.Hall> engineHalls = halls.stream()
                .map(h -> new com.exam.engine.model.Hall(h.getId(), (h.getInternalCapacity() != null ? h.getInternalCapacity() : 40) + 2)) // Add 2 for the 42 seat grid
                .collect(Collectors.toList());

        AllocationRequest request = new AllocationRequest(engineStudents, engineHalls, seasonHistory, positionHistory, seasonSessionIndex, examSessionId.toString());
        
        // USE INTERNAL ENGINE (7×6 grid)
        InternalAllocationEngine engine = new InternalAllocationEngine();

        try {
            AllocationResult result = engine.allocate(request);
            logger.info("[{}] Internal engine completed: {} assignments, {} violations",
                    allocationRequestId, result.assignments().size(), result.violations().size());

            Map<String, com.exam.entity.Student> studentMap = students.stream()
                    .collect(Collectors.toMap(com.exam.entity.Student::getRegisterNumber, s -> s));
            Map<String, com.exam.entity.Hall> hallMap = halls.stream()
                    .collect(Collectors.toMap(com.exam.entity.Hall::getId, h -> h));

            List<Allocation> newAllocations = new ArrayList<>();
            for (SeatAssignment a : result.assignments()) {
                com.exam.entity.Student s = studentMap.get(a.registerNumber());
                com.exam.entity.Hall h = hallMap.get(a.hallId());
                if (s != null && h != null) {
                    newAllocations.add(new Allocation(UUID.randomUUID(), batch, s, h, a.row(), a.col(), a.allocationRiskScore()));
                }
            }

            if (newAllocations.isEmpty()) throw new IllegalStateException("No allocations generated");

            batch.setStatus(BatchStatus.ACTIVE);
            batchRepo.save(batch);
            batchRepo.markOthersSuperseded(examSessionId, batch.getId());
            allocationRepo.saveAll(newAllocations);

            // Save violations as audit logs
            List<AuditLog> violationLogs = result.violations().stream()
                    .map(v -> new AuditLog(UUID.randomUUID(), session, batch, allocationRequestId,
                            AuditEventType.valueOf(v.type().name()), AuditSeverity.WARNING, v.message(), null))
                    .collect(Collectors.toList());
            auditRepo.saveAll(violationLogs);

            auditRepo.save(new AuditLog(UUID.randomUUID(), session, batch, allocationRequestId,
                    AuditEventType.ALLOCATION_COMPLETED, AuditSeverity.INFO,
                    "Internal allocation completed: " + result.assignments().size() + " seats assigned", null));

            return batch;
        } catch (Exception e) {
            logger.error("[{}] Internal allocation failed: {}", allocationRequestId, e.getMessage(), e);
            throw e;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markBatchFailed(UUID batchId, UUID allocationRequestId, String errorMessage) {
        AllocationBatch batch = batchRepo.findById(batchId).orElse(null);
        if (batch == null) return;

        batch.setStatus(BatchStatus.FAILED);
        batchRepo.save(batch);

        ExamSession session = batch.getExamSession();
        auditRepo.save(new AuditLog(UUID.randomUUID(), session, batch, allocationRequestId,
                AuditEventType.PREFLIGHT_FAILED, AuditSeverity.ERROR,
                "Internal allocation failed: " + errorMessage, null));
    }
}

package com.exam.service.impl;

import com.exam.dto.AllocationTriggerRequest;
import com.exam.dto.AllocationTriggerResponse;
import com.exam.dto.BatchStatusResponse;
import com.exam.entity.AllocationBatch;
import com.exam.entity.BatchStatus;
import com.exam.entity.Student;
import com.exam.exception.PreflightValidationException;
import com.exam.exception.QueueFullException;
import com.exam.exception.ResourceNotFoundException;
import com.exam.mapper.EntityMapper;
import com.exam.repository.AllocationBatchRepository;
import com.exam.repository.ExamSessionRepository;
import com.exam.repository.StudentRepository;
import com.exam.service.AllocationFacade;
import com.exam.service.AllocationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

@Service
public class AllocationFacadeImpl implements AllocationFacade {

    private static final Logger logger = LoggerFactory.getLogger(AllocationFacadeImpl.class);

    private final AllocationService orchestrationLayer;
    private final AllocationBatchRepository batchRepo;
    private final ExamSessionRepository sessionRepo;
    private final StudentRepository studentRepo;
    private final Executor allocationExecutor;

    public AllocationFacadeImpl(AllocationService orchestrationLayer,
                                AllocationBatchRepository batchRepo,
                                ExamSessionRepository sessionRepo,
                                StudentRepository studentRepo,
                                @Qualifier("allocationExecutor") Executor allocationExecutor) {
        this.orchestrationLayer = orchestrationLayer;
        this.batchRepo = batchRepo;
        this.sessionRepo = sessionRepo;
        this.studentRepo = studentRepo;
        this.allocationExecutor = allocationExecutor;
    }

    @Override
    public AllocationTriggerResponse triggerAllocation(UUID examSessionId, AllocationTriggerRequest request) {
        // --- Guard 1: Session must exist ---
        if (!sessionRepo.existsById(examSessionId)) {
            throw new ResourceNotFoundException("Exam session not found: " + examSessionId);
        }

        // --- Guard 2: Log warning if unresolved departments exist ---
        List<Student> unresolved = studentRepo.findByExamSessionId(examSessionId).stream()
                .filter(s -> "UNRESOLVED".equals(s.getDepartment()))
                .toList();
        if (!unresolved.isEmpty()) {
            logger.warn("Allocation proceeding with {} students having unresolved departments", unresolved.size());
        }

        // --- Create allocationRequestId HERE so it can be returned in the response ---
        UUID allocationRequestId = UUID.randomUUID();

        // --- Set MDC in caller thread — TaskDecorator propagates to async thread ---
        MDC.put("allocationRequestId", allocationRequestId.toString());

        // Capture selectedRooms for the async thread
        List<String> selectedRooms = request.selectedRooms();

        logger.info("[{}] Triggering async allocation for session: {}, requestedBy: {}, selectedRooms: {}",
                allocationRequestId, examSessionId, request.requestedBy(), selectedRooms.size());

        // --- Capture tenant context for async thread ---
        String tenantId = com.exam.config.tenant.TenantContext.getCurrentTenant();

        // --- Dispatch async ---
        try {
            CompletableFuture.runAsync(() -> {
                try {
                    if (tenantId != null) {
                        com.exam.config.tenant.TenantContext.setCurrentTenant(tenantId);
                    }
                    orchestrationLayer.runAllocation(examSessionId, request.requestedBy(), allocationRequestId, selectedRooms);
                } catch (Exception e) {
                    // Already logged inside runAllocation — this prevents unhandled exception noise
                    logger.error("[{}] Async allocation completed with error for session {}: {}",
                            allocationRequestId, examSessionId, e.getMessage());
                } finally {
                    com.exam.config.tenant.TenantContext.clear();
                }
            }, allocationExecutor).exceptionally(ex -> {
                // Infrastructure-level failure (e.g., thread interrupted)
                logger.error("[{}] Async execution infrastructure error for session {}",
                        allocationRequestId, examSessionId, ex);
                return null;
            });
        } catch (RejectedExecutionException e) {
            throw new QueueFullException(
                    "Allocation queue is full (capacity=50). Try again later.");
        } finally {
            // Safe: MDC is thread-local. Clearing in HTTP thread does not affect async thread.
            MDC.remove("allocationRequestId");
        }

        return new AllocationTriggerResponse(
                allocationRequestId,   // now returned to client for tracking
                examSessionId,
                null,                  // batchId not yet known (created async)
                BatchStatus.RUNNING.name()
        );
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public BatchStatusResponse getLatestBatchStatus(UUID examSessionId) {
        Optional<AllocationBatch> latest = batchRepo.findFirstByExamSessionIdOrderByCreatedAtDesc(examSessionId);

        if (latest.isEmpty()) {
             return new BatchStatusResponse(examSessionId, null, "NOT_STARTED", null, null);
        }

        return EntityMapper.toBatchStatus(latest.get());
    }
}

package com.exam.service.internal;

import com.exam.dto.AllocationTriggerRequest;
import com.exam.dto.AllocationTriggerResponse;
import com.exam.dto.BatchStatusResponse;
import com.exam.entity.AllocationBatch;
import com.exam.entity.BatchStatus;
import com.exam.exception.QueueFullException;
import com.exam.exception.ResourceNotFoundException;
import com.exam.mapper.EntityMapper;
import com.exam.repository.AllocationBatchRepository;
import com.exam.repository.ExamSessionRepository;
import com.exam.repository.StudentRepository;
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

/**
 * Internal Exam allocation facade.
 * Same async pattern as semester AllocationFacadeImpl but uses InternalAllocationService.
 */
@Service
public class InternalAllocationFacade {

    private static final Logger logger = LoggerFactory.getLogger(InternalAllocationFacade.class);

    private final InternalAllocationService orchestrationLayer;
    private final AllocationBatchRepository batchRepo;
    private final ExamSessionRepository sessionRepo;
    private final Executor allocationExecutor;

    public InternalAllocationFacade(InternalAllocationService orchestrationLayer,
                                     AllocationBatchRepository batchRepo,
                                     ExamSessionRepository sessionRepo,
                                     @Qualifier("allocationExecutor") Executor allocationExecutor) {
        this.orchestrationLayer = orchestrationLayer;
        this.batchRepo = batchRepo;
        this.sessionRepo = sessionRepo;
        this.allocationExecutor = allocationExecutor;
    }

    public AllocationTriggerResponse triggerAllocation(UUID examSessionId, AllocationTriggerRequest request) {
        if (!sessionRepo.existsById(examSessionId)) {
            throw new ResourceNotFoundException("Exam session not found: " + examSessionId);
        }

        UUID allocationRequestId = UUID.randomUUID();
        MDC.put("allocationRequestId", allocationRequestId.toString());

        List<String> selectedRooms = request.selectedRooms();

        logger.info("[{}] Triggering async INTERNAL allocation for session: {}", allocationRequestId, examSessionId);

        try {
            CompletableFuture.runAsync(() -> {
                try {
                    orchestrationLayer.runAllocation(examSessionId, request.requestedBy(), allocationRequestId, selectedRooms);
                } catch (Exception e) {
                    logger.error("[{}] Async internal allocation error: {}", allocationRequestId, e.getMessage());
                }
            }, allocationExecutor).exceptionally(ex -> {
                logger.error("[{}] Internal allocation infrastructure error", allocationRequestId, ex);
                return null;
            });
        } catch (RejectedExecutionException e) {
            throw new QueueFullException("Allocation queue is full. Try again later.");
        } finally {
            MDC.remove("allocationRequestId");
        }

        return new AllocationTriggerResponse(allocationRequestId, examSessionId, null, BatchStatus.RUNNING.name());
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public BatchStatusResponse getLatestBatchStatus(UUID examSessionId) {
        Optional<AllocationBatch> latest = batchRepo.findFirstByExamSessionIdOrderByCreatedAtDesc(examSessionId);
        if (latest.isEmpty()) {
            return new BatchStatusResponse(examSessionId, null, "NOT_STARTED", null, null);
        }
        return EntityMapper.toBatchStatus(latest.get());
    }
}

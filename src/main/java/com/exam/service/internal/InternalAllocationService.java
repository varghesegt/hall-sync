package com.exam.service.internal;

import com.exam.entity.AllocationBatch;
import com.exam.entity.BatchStatus;
import com.exam.exception.AllocationInProgressException;
import com.exam.exception.CooldownActiveException;
import com.exam.repository.AllocationBatchRepository;
import com.exam.repository.AllocationLockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Internal Exam allocation orchestrator.
 * Same lifecycle as AllocationService but delegates to InternalAllocationTxService.
 */
@Service
public class InternalAllocationService {

    private static final Logger logger = LoggerFactory.getLogger(InternalAllocationService.class);

    private final AllocationLockRepository lockRepo;
    private final AllocationBatchRepository batchRepo;
    private final InternalAllocationTxService txService;

    public InternalAllocationService(AllocationLockRepository lockRepo,
                                      AllocationBatchRepository batchRepo,
                                      InternalAllocationTxService txService) {
        this.lockRepo = lockRepo;
        this.batchRepo = batchRepo;
        this.txService = txService;
    }

    public AllocationBatch runAllocation(UUID examSessionId, String requestedBy,
                                          UUID allocationRequestId, List<String> selectedRooms) {
        logger.info("[{}] === Starting INTERNAL allocation for session: {} ===", allocationRequestId, examSessionId);

        // Idempotency guard
        Optional<AllocationBatch> existing = batchRepo.findByExamSessionIdAndStatus(examSessionId, BatchStatus.ACTIVE);
        if (existing.isPresent()) {
            logger.info("[{}] Active batch already exists: {}", allocationRequestId, existing.get().getId());
            return existing.get();
        }

        // Cooldown guard
        Optional<AllocationBatch> latestBatch = batchRepo.findFirstByExamSessionIdOrderByCreatedAtDesc(examSessionId);
        if (latestBatch.isPresent() && latestBatch.get().getStatus() == BatchStatus.FAILED) {
            if (latestBatch.get().getCreatedAt() != null &&
                latestBatch.get().getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(60))) {
                throw new CooldownActiveException("Allocation is in cooldown due to recent failure.", allocationRequestId);
            }
        }

        MDC.put("allocationRequestId", allocationRequestId.toString());

        // Acquire lock
        try {
            lockRepo.attemptLock(examSessionId, requestedBy);
        } catch (DataIntegrityViolationException e) {
            String msg = e.getMostSpecificCause().getMessage();
            if (msg != null && msg.contains("23506")) {
                throw new RuntimeException("Exam session not found: " + examSessionId, e);
            }
            throw new AllocationInProgressException("Allocation already locked for session " + examSessionId, allocationRequestId);
        }

        AllocationBatch batch = null;
        try {
            batch = txService.createRunningBatch(examSessionId, allocationRequestId);
            AllocationBatch result = txService.executeAllocation(batch.getId(), examSessionId, requestedBy, allocationRequestId, selectedRooms);
            logger.info("[{}] Internal allocation completed. Batch {} → ACTIVE", allocationRequestId, result.getId());
            return result;
        } catch (CooldownActiveException | AllocationInProgressException e) {
            throw e;
        } catch (Exception e) {
            logger.error("[{}] Internal allocation failed: {}", allocationRequestId, e.getMessage(), e);
            if (batch != null) {
                try { txService.markBatchFailed(batch.getId(), allocationRequestId, e.getMessage()); } catch (Exception fatal) {
                    logger.error("[{}] CRITICAL: Failed to mark batch FAILED", allocationRequestId, fatal);
                }
            }
            throw new RuntimeException("Internal allocation failed: " + e.getMessage(), e);
        } finally {
            try { lockRepo.releaseLock(examSessionId); } catch (Exception e) {
                logger.warn("[{}] Failed to release lock: {}", allocationRequestId, e.getMessage());
            }
            MDC.remove("allocationRequestId");
        }
    }
}

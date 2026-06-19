package com.exam.service;

import com.exam.entity.AllocationBatch;
import com.exam.entity.BatchStatus;
import com.exam.repository.AllocationBatchRepository;
import com.exam.repository.AllocationLockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.exam.exception.AllocationInProgressException;
import com.exam.exception.CooldownActiveException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AllocationService {

    private static final Logger logger = LoggerFactory.getLogger(AllocationService.class);

    private final AllocationLockRepository lockRepo;
    private final AllocationBatchRepository batchRepo;
    private final AllocationTxService txService;

    public AllocationService(AllocationLockRepository lockRepo, AllocationBatchRepository batchRepo, AllocationTxService txService) {
        this.lockRepo = lockRepo;
        this.batchRepo = batchRepo;
        this.txService = txService;
    }

    /**
     * Orchestrates the full allocation lifecycle:
     *   1. Idempotency guard → return existing ACTIVE batch
     *   2. Cooldown guard → reject if recent FAILED batch
     *   3. Acquire exclusive lock
     *   4. Create RUNNING batch (immediately visible)
     *   5. Execute engine → RUNNING → ACTIVE
     *   6. On failure → RUNNING → FAILED
     *   7. Release lock
     *
     * allocationRequestId is created by the CALLER and passed in,
     * so it can be returned in the HTTP response before async dispatch.
     */
    public AllocationBatch runAllocation(UUID examSessionId, String requestedBy, UUID allocationRequestId, List<String> selectedRooms) {
        logger.info("[{}] === Starting allocation for session: {} with {} selected rooms ===", allocationRequestId, examSessionId, selectedRooms.size());

        // --- Idempotency: return existing ACTIVE batch ---
        Optional<AllocationBatch> existing = batchRepo.findByExamSessionIdAndStatus(examSessionId, BatchStatus.ACTIVE);
        if (existing.isPresent()) {
            logger.info("[{}] Active batch already exists: {}", allocationRequestId, existing.get().getId());
            return existing.get();
        }

        // --- Cooldown guard ---
        Optional<AllocationBatch> latestBatch = batchRepo.findFirstByExamSessionIdOrderByCreatedAtDesc(examSessionId);
        if (latestBatch.isPresent() && latestBatch.get().getStatus() == BatchStatus.FAILED) {
            if (latestBatch.get().getCreatedAt() != null &&
                latestBatch.get().getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(60))) {
                throw new CooldownActiveException(
                        "Allocation is in a strict cooldown state due to recent consecutive failures.",
                        allocationRequestId);
            }
        }

        MDC.put("allocationRequestId", allocationRequestId.toString());

        // --- Acquire exclusive lock ---
        try {
            lockRepo.attemptLock(examSessionId, requestedBy);
            logger.info("[{}] Lock acquired for session: {}", allocationRequestId, examSessionId);
        } catch (DataIntegrityViolationException e) {
            String msg = e.getMostSpecificCause().getMessage();
            logger.error("[{}] Lock attempt failed for session {}: {}", allocationRequestId, examSessionId, msg);
            // Distinguish FK violation (session doesn't exist) from duplicate PK (already locked)
            if (msg != null && msg.contains("23506")) {
                throw new RuntimeException("Exam session not found in database: " + examSessionId, e);
            }
            throw new AllocationInProgressException(
                    "Allocation already locked for session " + examSessionId, allocationRequestId);
        }

        // --- Create RUNNING batch FIRST (its own TX, immediately visible) ---
        AllocationBatch batch = null;
        try {
            batch = txService.createRunningBatch(examSessionId, allocationRequestId);
            logger.info("[{}] RUNNING batch created: {}", allocationRequestId, batch.getId());

            // --- Execute engine (its own TX, transitions RUNNING → ACTIVE) ---
            AllocationBatch result = txService.executeAllocation(
                    batch.getId(), examSessionId, requestedBy, allocationRequestId, selectedRooms);
            logger.info("[{}] Allocation completed. Batch {} → ACTIVE", allocationRequestId, result.getId());
            return result;

        } catch (CooldownActiveException | AllocationInProgressException e) {
            throw e;
        } catch (Exception e) {
            logger.error("[{}] Allocation failed: {}", allocationRequestId, e.getMessage(), e);

            // --- Mark batch as FAILED (its own TX) ---
            if (batch != null) {
                try {
                    txService.markBatchFailed(batch.getId(), allocationRequestId, e.getMessage());
                    logger.info("[{}] Batch {} marked as FAILED", allocationRequestId, batch.getId());
                } catch (Exception fatal) {
                    logger.error("[{}] CRITICAL: Failed to mark batch {} as FAILED: {}",
                            allocationRequestId, batch.getId(), fatal.getMessage(), fatal);
                }
            }
            throw new RuntimeException("Allocation failed: " + e.getMessage(), e);
        } finally {
            try {
                lockRepo.releaseLock(examSessionId);
                logger.info("[{}] Lock released for session: {}", allocationRequestId, examSessionId);
            } catch (Exception e) {
                logger.warn("[{}] Failed to release lock for session {}: {}",
                        allocationRequestId, examSessionId, e.getMessage());
            }
            MDC.remove("allocationRequestId");
        }
    }
}

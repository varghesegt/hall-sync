package com.exam.service;

import com.exam.entity.AllocationBatch;
import com.exam.repository.AllocationBatchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class AllocationWatchdog {

    private static final Logger logger = LoggerFactory.getLogger(AllocationWatchdog.class);
    private final AllocationBatchRepository batchRepo;
    private final AllocationTxService txService;

    // Define timeout threshold (e.g., if a batch has been running for more than 5 minutes, mark as FAILED)
    private static final int TIMEOUT_MINUTES = 5;

    public AllocationWatchdog(AllocationBatchRepository batchRepo, AllocationTxService txService) {
        this.batchRepo = batchRepo;
        this.txService = txService;
    }

    // Runs every 1 minute
    @Scheduled(fixedDelay = 60000)
    public void checkForStaleBatches() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(TIMEOUT_MINUTES);
        List<AllocationBatch> staleBatches = batchRepo.findStaleRunningBatches(threshold);

        for (AllocationBatch batch : staleBatches) {
            logger.warn("Watchdog detected stale RUNNING batch {}. Marking as FAILED.", batch.getId());
            try {
                txService.markBatchFailed(batch.getId(), batch.getAllocationRequestId(), "Batch execution timed out after " + TIMEOUT_MINUTES + " minutes. Infrastructure or thread failure occurred.");
            } catch (Exception e) {
                logger.error("Watchdog failed to mark batch {} as FAILED: {}", batch.getId(), e.getMessage(), e);
            }
        }
    }
}

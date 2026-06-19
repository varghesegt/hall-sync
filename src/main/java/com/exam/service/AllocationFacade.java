package com.exam.service;

import com.exam.dto.AllocationTriggerRequest;
import com.exam.dto.AllocationTriggerResponse;
import com.exam.dto.BatchStatusResponse;
import java.util.UUID;

public interface AllocationFacade {
    AllocationTriggerResponse triggerAllocation(UUID examSessionId, AllocationTriggerRequest request);
    BatchStatusResponse getLatestBatchStatus(UUID examSessionId);
}

package com.exam.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record AllocationTriggerResponse(
        UUID allocationRequestId,
        UUID examSessionId,
        UUID batchId,
        String status
) {}

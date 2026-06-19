package com.exam.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record BatchStatusResponse(
        UUID examSessionId,
        UUID batchId,
        String status,
        UUID allocationRequestId,
        LocalDateTime createdAt
) {}

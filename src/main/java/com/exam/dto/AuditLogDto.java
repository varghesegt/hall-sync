package com.exam.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuditLogDto(
        UUID id,
        UUID allocationRequestId,
        String eventType,
        String severity,
        String message,
        LocalDateTime createdAt
) {}

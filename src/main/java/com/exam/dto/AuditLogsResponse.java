package com.exam.dto;

import java.util.List;
import java.util.UUID;

public record AuditLogsResponse(
        UUID batchId,
        List<AuditLogDto> logs
) {}

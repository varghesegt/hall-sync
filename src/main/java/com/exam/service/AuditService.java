package com.exam.service;

import com.exam.dto.AuditLogsResponse;
import java.util.UUID;

public interface AuditService {
    AuditLogsResponse getAuditLogs(UUID batchId);
}

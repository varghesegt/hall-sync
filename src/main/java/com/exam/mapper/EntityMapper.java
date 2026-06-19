package com.exam.mapper;

import com.exam.dto.BatchStatusResponse;
import com.exam.dto.SessionCreateResponse;
import com.exam.dto.AuditLogDto;
import com.exam.entity.AllocationBatch;
import com.exam.entity.ExamSession;
import com.exam.entity.AuditLog;

public class EntityMapper {

    public static BatchStatusResponse toBatchStatus(AllocationBatch batch) {
        if (batch == null) return null;
        return new BatchStatusResponse(
                batch.getExamSession().getId(),
                batch.getId(),
                batch.getStatus().name(),
                batch.getAllocationRequestId(),
                batch.getCreatedAt()
        );
    }

    public static SessionCreateResponse toSessionCreate(ExamSession session, int mockedStudentCount) {
        if (session == null) return null;
        return new SessionCreateResponse(
                session.getId(),
                "PREFLIGHT_PASSED",
                mockedStudentCount,
                false,
                java.util.Collections.emptyList()
        );
    }

    public static AuditLogDto toAuditLogDto(AuditLog log) {
        if (log == null) return null;
        return new AuditLogDto(
                log.getId(),
                log.getAllocationRequestId(),
                log.getEventType() != null ? log.getEventType().name() : null,
                log.getSeverity() != null ? log.getSeverity().name() : null,
                log.getMessage(),
                log.getCreatedAt()
        );
    }
}

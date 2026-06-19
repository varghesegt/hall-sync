package com.exam.service.impl;

import com.exam.dto.AuditLogDto;
import com.exam.dto.AuditLogsResponse;
import com.exam.entity.AuditLog;
import com.exam.mapper.EntityMapper;
import com.exam.repository.AuditLogRepository;
import com.exam.service.AuditService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuditFacadeImpl implements AuditService {

    private final AuditLogRepository auditRepo;

    public AuditFacadeImpl(AuditLogRepository auditRepo) {
        this.auditRepo = auditRepo;
    }

    @Override
    public AuditLogsResponse getAuditLogs(UUID batchId) {
        List<AuditLog> rawLogs = auditRepo.findByBatchIdOrderByCreatedAtDesc(batchId);
        
        List<AuditLogDto> dtos = rawLogs.stream()
                .map(EntityMapper::toAuditLogDto)
                .collect(Collectors.toList());
                
        return new AuditLogsResponse(batchId, dtos);
    }
}

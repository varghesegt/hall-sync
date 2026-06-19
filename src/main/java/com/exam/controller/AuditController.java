package com.exam.controller;

import com.exam.dto.AuditLogsResponse;
import com.exam.service.AuditService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/allocation-batches/{batchId}/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public ResponseEntity<AuditLogsResponse> getAuditLogs(@PathVariable UUID batchId) {
        AuditLogsResponse response = auditService.getAuditLogs(batchId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}

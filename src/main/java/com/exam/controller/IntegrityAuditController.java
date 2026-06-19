package com.exam.controller;

import com.exam.dto.BatchStatusResponse;
import com.exam.repository.ExamSessionRepository;
import com.exam.service.BatchService;
import com.exam.service.IntegrityAuditService;
import com.exam.service.IntegrityCertificatePdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

/**
 * INTEGRITY AUDIT CONTROLLER — Purely additive.
 * Provides endpoints for the Seating Integrity Certificate.
 */
@RestController
@RequestMapping("/api/v1/allocation-batches/{batchId}")
public class IntegrityAuditController {

    private static final Logger logger = LoggerFactory.getLogger(IntegrityAuditController.class);

    private final IntegrityAuditService auditService;
    private final IntegrityCertificatePdfService certificatePdfService;
    private final BatchService batchService;
    private final ExamSessionRepository examSessionRepository;

    public IntegrityAuditController(IntegrityAuditService auditService,
                                     IntegrityCertificatePdfService certificatePdfService,
                                     BatchService batchService,
                                     ExamSessionRepository examSessionRepository) {
        this.auditService = auditService;
        this.certificatePdfService = certificatePdfService;
        this.batchService = batchService;
        this.examSessionRepository = examSessionRepository;
    }

    /**
     * GET /api/v1/allocation-batches/{batchId}/integrity — JSON summary
     */
    @GetMapping("/integrity")
    public ResponseEntity<Map<String, Object>> getIntegritySummary(@PathVariable UUID batchId) {
        IntegrityAuditService.AuditReport report = auditService.generateAuditReport(batchId);

        Map<String, Object> summary = new java.util.LinkedHashMap<>();
        summary.put("certified", report.certified());
        summary.put("integrityScore", String.format("%.2f", report.integrityScorePercent()));
        summary.put("deptPurityScore", String.format("%.2f", report.deptPurityScorePercent()));
        summary.put("totalStudents", report.totalStudents());
        summary.put("totalHalls", report.totalHalls());
        summary.put("totalAdjacencyChecks", report.totalAdjacencyChecks());
        summary.put("totalViolations", report.totalAdjacencyViolations());

        // Include per-hall violation breakdown when integrity < 100%
        if (!report.certified()) {
            java.util.List<Map<String, Object>> hallViolations = new java.util.ArrayList<>();
            for (IntegrityAuditService.HallAuditResult hall : report.hallResults()) {
                if (hall.adjacencyViolations() > 0) {
                    Map<String, Object> hv = new java.util.LinkedHashMap<>();
                    hv.put("hallName", hall.hallName());
                    hv.put("violations", hall.adjacencyViolations());
                    hv.put("studentCount", hall.studentCount());
                    hv.put("details", hall.violationDetails());
                    hallViolations.add(hv);
                }
            }
            summary.put("hallViolations", hallViolations);
        }

        return ResponseEntity.ok(summary);
    }

    /**
     * GET /api/v1/allocation-batches/{batchId}/integrity-certificate — PDF download
     */
    @GetMapping("/integrity-certificate")
    public ResponseEntity<StreamingResponseBody> downloadIntegrityCertificate(@PathVariable UUID batchId) {
        // Pre-validate and generate the report (within transaction)
        IntegrityAuditService.AuditReport report = auditService.generateAuditReport(batchId);
        String tenantId = com.exam.config.tenant.TenantContext.getCurrentTenant();

        StreamingResponseBody stream = out -> {
            try {
                if (tenantId != null) {
                    com.exam.config.tenant.TenantContext.setCurrentTenant(tenantId);
                }
                certificatePdfService.generateCertificate(report, out);
                out.flush();
            } catch (IOException ioEx) {
                logger.warn("Client aborted Integrity Certificate download for batchId={}", batchId);
            } catch (Exception e) {
                logger.error("Integrity Certificate generation failed for batchId={}", batchId, e);
                throw new IOException("Certificate generation failed", e);
            } finally {
                com.exam.config.tenant.TenantContext.clear();
            }
        };

        String filename = getFormattedFilename(batchId) + " - Integrity Certificate.pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        headers.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION);
        headers.setCacheControl(CacheControl.noCache().getHeaderValue());

        return new ResponseEntity<>(stream, headers, HttpStatus.OK);
    }

    private String getFormattedFilename(UUID batchId) {
        try {
            BatchStatusResponse status = batchService.getBatchStatus(batchId);
            if (status != null && status.examSessionId() != null) {
                var sessionOpt = examSessionRepository.findById(status.examSessionId());
                if (sessionOpt.isPresent()) {
                    var session = sessionOpt.get();
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                    return session.getExamDate().format(formatter) + " " + session.getSession();
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to format filename for batchId {}", batchId, e);
        }
        return "integrity-" + batchId;
    }
}

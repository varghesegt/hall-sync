package com.exam.controller.internal;

import com.exam.dto.BatchStatusResponse;
import com.exam.dto.PdfAllocationView;
import com.exam.repository.ExamSessionRepository;
import com.exam.service.internal.InternalBatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/v1/internal/allocation-batches/{batchId}")
public class InternalBatchController {

    private static final Logger logger = LoggerFactory.getLogger(InternalBatchController.class);
    private final InternalBatchService batchService;
    private final ExamSessionRepository examSessionRepository;

    public InternalBatchController(InternalBatchService batchService, ExamSessionRepository examSessionRepository) {
        this.batchService = batchService;
        this.examSessionRepository = examSessionRepository;
    }

    private String getFormattedBaseName(UUID batchId) {
        try {
            BatchStatusResponse status = batchService.getBatchStatus(batchId);
            if (status != null && status.examSessionId() != null) {
                var sessionOpt = examSessionRepository.findById(status.examSessionId());
                if (sessionOpt.isPresent()) {
                    var session = sessionOpt.get();
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                    return "Internal " + session.getExamDate().format(formatter) + " " + session.getSession();
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to generate formatted filename for batchId {}", batchId, e);
        }
        return "internal-allocation-" + batchId;
    }

    @GetMapping("/status")
    public ResponseEntity<BatchStatusResponse> getStatus(@PathVariable UUID batchId) {
        return new ResponseEntity<>(batchService.getBatchStatus(batchId), HttpStatus.OK);
    }

    @GetMapping("/preview")
    public ResponseEntity<List<PdfAllocationView>> getPreview(@PathVariable UUID batchId) {
        return ResponseEntity.ok(batchService.getPreviewData(batchId));
    }

    @GetMapping(value = "/pdf")
    public ResponseEntity<?> downloadPdf(@PathVariable UUID batchId) {
        BatchStatusResponse status = batchService.getBatchStatus(batchId);
        if (status == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Batch not found: " + batchId));
        }
        if ("RUNNING".equalsIgnoreCase(status.status())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Allocation is still in progress. Please wait for completion."));
        }
        if ("FAILED".equalsIgnoreCase(status.status())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Allocation failed. Please re-run allocation."));
        }
        List<PdfAllocationView> preview = batchService.getPreviewData(batchId);
        if (preview == null || preview.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "No allocations found in batch: " + batchId));
        }

        String mdcToken = MDC.get("allocationRequestId");
        String tenantId = com.exam.config.tenant.TenantContext.getCurrentTenant();
        final String resolvedTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : "krce";

        StreamingResponseBody stream = out -> {
            try {
                com.exam.config.tenant.TenantContext.setCurrentTenant(resolvedTenant);
                if (mdcToken != null) MDC.put("allocationRequestId", mdcToken);
                batchService.generatePdf(batchId, out);
                out.flush();
            } catch (IOException ioEx) {
                if (isClientAbort(ioEx)) {
                    logger.warn("Client aborted Internal PDF download for batchId={}", batchId);
                } else {
                    throw ioEx;
                }
            } catch (Exception e) {
                logger.error("Internal PDF generation failed for batchId={}", batchId, e);
                throw new IOException("PDF generation failed", e);
            } finally {
                MDC.remove("allocationRequestId");
                com.exam.config.tenant.TenantContext.clear();
            }
        };

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", getFormattedBaseName(batchId) + ".pdf");
        headers.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION);
        headers.setCacheControl(CacheControl.noCache().getHeaderValue());
        headers.add(HttpHeaders.PRAGMA, "no-cache");
        headers.add(HttpHeaders.EXPIRES, "0");

        return new ResponseEntity<>(stream, headers, HttpStatus.OK);
    }

    @GetMapping(value = "/excel")
    public ResponseEntity<?> downloadExcel(@PathVariable UUID batchId) {
        BatchStatusResponse status = batchService.getBatchStatus(batchId);
        if (status == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Batch not found: " + batchId));
        }
        if ("RUNNING".equalsIgnoreCase(status.status())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Allocation is still in progress. Please wait for completion."));
        }
        if ("FAILED".equalsIgnoreCase(status.status())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Allocation failed. Please re-run allocation."));
        }
        List<PdfAllocationView> preview = batchService.getPreviewData(batchId);
        if (preview == null || preview.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "No allocations found in batch: " + batchId));
        }

        String mdcToken = MDC.get("allocationRequestId");
        String tenantId = com.exam.config.tenant.TenantContext.getCurrentTenant();
        final String resolvedTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : "krce";

        StreamingResponseBody stream = out -> {
            try {
                com.exam.config.tenant.TenantContext.setCurrentTenant(resolvedTenant);
                if (mdcToken != null) MDC.put("allocationRequestId", mdcToken);
                batchService.generateExcel(batchId, out);
                out.flush();
            } catch (IOException ioEx) {
                if (isClientAbort(ioEx)) {
                    logger.warn("Client aborted Internal Excel download for batchId={}", batchId);
                } else {
                    throw ioEx;
                }
            } catch (Exception e) {
                logger.error("Internal Excel generation failed for batchId={}", batchId, e);
                throw new IOException("Excel generation failed", e);
            } finally {
                MDC.remove("allocationRequestId");
                com.exam.config.tenant.TenantContext.clear();
            }
        };

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", getFormattedBaseName(batchId) + " - Allocation Sheet.xlsx");
        headers.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION);
        headers.setCacheControl(CacheControl.noCache().getHeaderValue());
        headers.add(HttpHeaders.PRAGMA, "no-cache");
        headers.add(HttpHeaders.EXPIRES, "0");

        return new ResponseEntity<>(stream, headers, HttpStatus.OK);
    }

    @GetMapping(value = "/summary-excel")
    public ResponseEntity<?> downloadSummaryExcel(@PathVariable UUID batchId) {
        BatchStatusResponse status = batchService.getBatchStatus(batchId);
        if (status == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Batch not found: " + batchId));
        }
        if ("RUNNING".equalsIgnoreCase(status.status())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Allocation is still in progress. Please wait for completion."));
        }
        if ("FAILED".equalsIgnoreCase(status.status())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Allocation failed. Please re-run allocation."));
        }

        String mdcToken = MDC.get("allocationRequestId");
        String tenantId = com.exam.config.tenant.TenantContext.getCurrentTenant();
        final String resolvedTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : "krce";

        StreamingResponseBody stream = out -> {
            try {
                com.exam.config.tenant.TenantContext.setCurrentTenant(resolvedTenant);
                if (mdcToken != null) MDC.put("allocationRequestId", mdcToken);
                batchService.generateSummaryExcel(batchId, out);
                out.flush();
            } catch (IOException ioEx) {
                if (isClientAbort(ioEx)) {
                    logger.warn("Client aborted Internal Summary download for batchId={}", batchId);
                } else {
                    throw ioEx;
                }
            } catch (Exception e) {
                logger.error("Internal Summary generation failed for batchId={}", batchId, e);
                throw new IOException("Summary Excel generation failed", e);
            } finally {
                MDC.remove("allocationRequestId");
                com.exam.config.tenant.TenantContext.clear();
            }
        };

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", getFormattedBaseName(batchId) + " - Count Opening.xlsx");
        headers.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION);
        headers.setCacheControl(CacheControl.noCache().getHeaderValue());
        headers.add(HttpHeaders.PRAGMA, "no-cache");
        headers.add(HttpHeaders.EXPIRES, "0");

        return new ResponseEntity<>(stream, headers, HttpStatus.OK);
    }

    private boolean isClientAbort(IOException ex) {
        if (ex instanceof org.apache.catalina.connector.ClientAbortException) return true;
        String msg = ex.getMessage();
        return msg != null && (msg.contains("Broken pipe") || msg.contains("Connection reset by peer"));
    }
}

package com.exam.controller;

import com.exam.dto.BatchStatusResponse;
import com.exam.dto.PdfAllocationView;
import com.exam.repository.ExamSessionRepository;
import com.exam.service.BatchService;
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
@RequestMapping("/api/v1/allocation-batches/{batchId}")
public class BatchController {

    private static final Logger logger = LoggerFactory.getLogger(BatchController.class);
    private final BatchService batchService;
    private final ExamSessionRepository examSessionRepository;

    public BatchController(BatchService batchService, ExamSessionRepository examSessionRepository) {
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
                    return session.getExamDate().format(formatter) + " " + session.getSession();
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to generate formatted filename for batchId {}", batchId, e);
        }
        return "allocation-" + batchId;
    }

    @GetMapping("/status")
    public ResponseEntity<BatchStatusResponse> getStatus(@PathVariable UUID batchId) {
        BatchStatusResponse response = batchService.getBatchStatus(batchId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/preview")
    public ResponseEntity<List<PdfAllocationView>> getPreview(@PathVariable UUID batchId) {
        List<PdfAllocationView> data = batchService.getPreviewData(batchId);
        return ResponseEntity.ok(data);
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

        com.exam.dto.GeneratedDocument doc = batchService.generatePdfDocument(batchId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        String safeFilename = doc.filename().replaceAll("[^a-zA-Z0-9._ -]", "_");
        headers.setContentDisposition(org.springframework.http.ContentDisposition.attachment()
                .filename(safeFilename)
                .build());
        headers.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "Content-Disposition, Content-Length");
        headers.setCacheControl(CacheControl.noCache().getHeaderValue());
        headers.add(HttpHeaders.PRAGMA, "no-cache");
        headers.add(HttpHeaders.EXPIRES, "0");

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(doc.bytes().length)
                .body(doc.bytes());
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

        com.exam.dto.GeneratedDocument doc = batchService.generateExcelDocument(batchId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        String safeFilename = doc.filename().replaceAll("[^a-zA-Z0-9._ -]", "_");
        headers.setContentDisposition(org.springframework.http.ContentDisposition.attachment()
                .filename(safeFilename)
                .build());
        headers.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "Content-Disposition, Content-Length");
        headers.setCacheControl(CacheControl.noCache().getHeaderValue());
        headers.add(HttpHeaders.PRAGMA, "no-cache");
        headers.add(HttpHeaders.EXPIRES, "0");

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(doc.bytes().length)
                .body(doc.bytes());
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

        com.exam.dto.GeneratedDocument doc = batchService.generateSummaryExcelDocument(batchId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        String safeFilename = doc.filename().replaceAll("[^a-zA-Z0-9._ -]", "_");
        headers.setContentDisposition(org.springframework.http.ContentDisposition.attachment()
                .filename(safeFilename)
                .build());
        headers.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "Content-Disposition, Content-Length");
        headers.setCacheControl(CacheControl.noCache().getHeaderValue());
        headers.add(HttpHeaders.PRAGMA, "no-cache");
        headers.add(HttpHeaders.EXPIRES, "0");

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(doc.bytes().length)
                .body(doc.bytes());
    }
}

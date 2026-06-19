package com.exam.controller;

import com.exam.service.AccreditationReportService;
import com.exam.service.AccreditationService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
public class AccreditationController {

    private final AccreditationReportService reportService;
    private final AccreditationService accreditationService;

    public AccreditationController(AccreditationReportService reportService, AccreditationService accreditationService) {
        this.reportService = reportService;
        this.accreditationService = accreditationService;
    }

    @GetMapping("/evidence-pack/{archiveId}")
    public ResponseEntity<StreamingResponseBody> generateEvidencePack(@org.springframework.web.bind.annotation.PathVariable UUID archiveId) {
        StreamingResponseBody stream = out -> {
            accreditationService.generateEvidencePack(archiveId, out);
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"NAAC_Evidence_Pack_" + archiveId + ".zip\"")
                .header(HttpHeaders.CONTENT_TYPE, "application/zip")
                .body(stream);
    }

    @GetMapping("/naac")
    public ResponseEntity<byte[]> getNaacReport(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        
        LocalDate fromDate = from != null ? LocalDate.parse(from) : LocalDate.now().minusYears(1);
        LocalDate toDate = to != null ? LocalDate.parse(to) : LocalDate.now();

        byte[] pdfData = reportService.generateNaacReport(fromDate, toDate);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=naac_report.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfData);
    }

    @GetMapping("/nba")
    public ResponseEntity<byte[]> getNbaReport(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        
        LocalDate fromDate = from != null ? LocalDate.parse(from) : LocalDate.now().minusYears(1);
        LocalDate toDate = to != null ? LocalDate.parse(to) : LocalDate.now();

        byte[] pdfData = reportService.generateNbaReport(fromDate, toDate);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=nba_report.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfData);
    }

    @GetMapping("/audit")
    public ResponseEntity<byte[]> getAuditReport(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        
        LocalDate fromDate = from != null ? LocalDate.parse(from) : LocalDate.now().minusMonths(3);
        LocalDate toDate = to != null ? LocalDate.parse(to) : LocalDate.now();

        byte[] pdfData = reportService.generateAuditReport(fromDate, toDate);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=audit_report.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfData);
    }
}

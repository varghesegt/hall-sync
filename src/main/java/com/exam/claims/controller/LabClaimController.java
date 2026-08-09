package com.exam.claims.controller;

import com.exam.claims.entity.LabClaimRecord;
import com.exam.claims.repository.LabClaimRecordRepository;
import com.exam.claims.service.LabClaimCalculationService;
import com.exam.claims.service.LabExcelParserService;
import com.exam.claims.service.LabPdfGeneratorService;
import com.exam.claims.service.LabWordGeneratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.*;

@RestController
@RequestMapping("/api/v1/lab-claims")
@CrossOrigin(origins = "*")
public class LabClaimController {

    private static final Logger log = LoggerFactory.getLogger(LabClaimController.class);

    @Autowired
    private LabClaimRecordRepository repository;

    @Autowired
    private LabExcelParserService excelParserService;

    @Autowired
    private LabClaimCalculationService calculationService;

    @Autowired
    private LabPdfGeneratorService pdfGeneratorService;

    @Autowired
    private LabWordGeneratorService wordGeneratorService;

    @GetMapping
    public ResponseEntity<List<LabClaimRecord>> getAllClaims() {
        List<LabClaimRecord> records = repository.findAll();
        for (LabClaimRecord record : records) {
            if (record.getTotalAmount() == null) {
                calculationService.calculateLabClaim(record);
            }
        }
        return ResponseEntity.ok(records);
    }

    @GetMapping("/batch/{batchId}")
    public ResponseEntity<List<LabClaimRecord>> getClaimsByBatch(@PathVariable("batchId") UUID batchId) {
        List<LabClaimRecord> records = repository.findByBatchId(batchId);
        return ResponseEntity.ok(records);
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "examSeason", required = false) String examSeason,
            @RequestParam(value = "examDate", required = false) String examDateStr,
            @RequestParam(value = "subjectCode", required = false) String subjectCode) {
        log.info("Received lab claims Excel upload: {} (Season: {}, Date: {}, Subject: {})",
                file.getOriginalFilename(), examSeason, examDateStr, subjectCode);
        Map<String, Object> response = new HashMap<>();

        if (examSeason == null || examSeason.isBlank() || examDateStr == null || examDateStr.isBlank() || subjectCode == null || subjectCode.isBlank()) {
            response.put("message", "Exam Season, Exam Date, and Subject Code are required before uploading.");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            LabExcelParserService.ParseResult parseResult = excelParserService.parseExcel(file);
            List<LabClaimRecord> records = parseResult.getRecords();

            if (records.isEmpty()) {
                response.put("message", "No valid lab claim records found in the uploaded file.");
                response.put("warnings", parseResult.getWarnings());
                return ResponseEntity.badRequest().body(response);
            }

            java.time.LocalDate parsedExamDate = null;
            if (examDateStr != null && !examDateStr.trim().isEmpty()) {
                try {
                    parsedExamDate = java.time.LocalDate.parse(examDateStr.trim(), java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                } catch (Exception e) {
                    log.warn("Could not parse input examDate {}", examDateStr);
                }
            }

            // 1. Filter by Subject Code (normalized)
            String normInputCode = subjectCode != null ? subjectCode.toUpperCase().replaceAll("[^A-Z0-9]", "").trim() : "";
            if (!normInputCode.isEmpty()) {
                List<LabClaimRecord> subjectFiltered = new ArrayList<>();
                for (LabClaimRecord record : records) {
                    String recCode = record.getSubjectCode() != null ? record.getSubjectCode().toUpperCase().replaceAll("[^A-Z0-9]", "").trim() : "";
                    if (recCode.equals(normInputCode) || recCode.contains(normInputCode) || normInputCode.contains(recCode)) {
                        subjectFiltered.add(record);
                    }
                }
                if (!subjectFiltered.isEmpty()) {
                    records = subjectFiltered;
                }
            }

            // 2. Filter by Exam Date if matching records exist
            if (parsedExamDate != null) {
                List<LabClaimRecord> dateFiltered = new ArrayList<>();
                for (LabClaimRecord record : records) {
                    if (record.getExamDate() == null || record.getExamDate().equals(parsedExamDate)) {
                        dateFiltered.add(record);
                    }
                }
                if (!dateFiltered.isEmpty()) {
                    records = dateFiltered;
                }
            }

            if (records.isEmpty()) {
                response.put("message", "No lab claim records in the uploaded file matched Exam Date (" + examDateStr + ") and Subject Code (" + subjectCode + ").");
                response.put("warnings", parseResult.getWarnings());
                return ResponseEntity.badRequest().body(response);
            }

            UUID batchId = UUID.randomUUID();
            for (LabClaimRecord record : records) {
                record.setBatchId(batchId);
                if (examSeason != null && !examSeason.isBlank()) {
                    record.setExamSeason(examSeason.trim());
                }
                if (parsedExamDate != null) {
                    record.setExamDate(parsedExamDate);
                }
                if (subjectCode != null && !subjectCode.isBlank()) {
                    record.setSubjectCode(subjectCode.trim().toUpperCase());
                }
            }

            List<LabClaimRecord> saved = repository.saveAll(records);
            log.info("Saved {} strictly matched lab claim records with batchId {}", saved.size(), batchId);

            response.put("batchId", batchId);
            response.put("count", saved.size());
            response.put("message", "Successfully imported " + saved.size() + " lab claim records matching Date: " + examDateStr + " & Subject: " + subjectCode);
            response.put("warnings", parseResult.getWarnings());
            response.put("records", saved);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to parse and import lab claims Excel", e);
            response.put("message", "Failed to parse Excel file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping
    public ResponseEntity<LabClaimRecord> createClaim(@RequestBody LabClaimRecord record) {
        if (record.getBatchId() == null) {
            record.setBatchId(UUID.randomUUID());
        }
        calculationService.calculateLabClaim(record);
        LabClaimRecord saved = repository.save(record);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @DeleteMapping("/all")
    public ResponseEntity<Map<String, String>> deleteAllClaims() {
        long count = repository.count();
        repository.deleteAll();
        log.info("Deleted all {} lab claim records.", count);
        Map<String, String> res = new HashMap<>();
        res.put("message", "Successfully deleted all " + count + " lab claim records.");
        return ResponseEntity.ok(res);
    }

    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<Void> deleteClaim(@PathVariable("id") Long id) {
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sample-template")
    public ResponseEntity<byte[]> downloadSampleTemplate() {
        try {
            File file = new File("C:/Users/Asus/Downloads/Comprehensive_Lab_Claims_Form_Responses.xlsx");
            if (!file.exists()) {
                file = new File("c:/Users/Asus/Downloads/HALLSYNC_KRCE/CLAIMS/Comprehensive_Lab_Claims_Form_Responses.xlsx");
            }
            byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", "Comprehensive_Lab_Claims_Form_Responses.xlsx");
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
            return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Failed to download sample template", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable("id") Long id) {
        Optional<LabClaimRecord> opt = repository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        LabClaimRecord target = opt.get();
        List<LabClaimRecord> sessionRecords = findSessionRecords(target);

        try {
            byte[] pdf = pdfGeneratorService.generateLabClaimPdf(target);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "lab_claim_" + id + ".pdf");
            return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error generating lab claim PDF for id {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}/word")
    public ResponseEntity<byte[]> downloadWord(@PathVariable("id") Long id) {
        Optional<LabClaimRecord> opt = repository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        LabClaimRecord target = opt.get();
        List<LabClaimRecord> sessionRecords = findSessionRecords(target);

        try {
            byte[] word = wordGeneratorService.generateBatchWord(sessionRecords);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
            String fileName = (target.getExamDate() != null ? target.getExamDate().toString() : "lab") + "_" +
                    (target.getSubjectCode() != null ? target.getSubjectCode() : "claim") + "_Claim_Form.docx";
            headers.setContentDispositionFormData("attachment", fileName);
            return new ResponseEntity<>(word, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error generating lab claim Word doc for id {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/download-session-word")
    public ResponseEntity<byte[]> downloadSessionWord(
            @RequestParam("examDate") String examDateStr,
            @RequestParam("subjectCode") String subjectCode) {
        List<LabClaimRecord> all = repository.findAll();
        List<LabClaimRecord> sessionRecords = new ArrayList<>();
        String targetCode = subjectCode != null ? subjectCode.toUpperCase().replaceAll("[^A-Z0-9]", "").trim() : "";

        for (LabClaimRecord r : all) {
            String date = r.getExamDate() != null ? r.getExamDate().toString() : "";
            String code = r.getSubjectCode() != null ? r.getSubjectCode().toUpperCase().replaceAll("[^A-Z0-9]", "").trim() : "";

            if (date.equalsIgnoreCase(examDateStr.trim()) && (targetCode.isEmpty() || code.equals(targetCode) || code.contains(targetCode))) {
                if (r.getTotalAmount() == null) {
                    calculationService.calculateLabClaim(r);
                    repository.save(r);
                }
                sessionRecords.add(r);
            }
        }

        if (sessionRecords.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            byte[] word = wordGeneratorService.generateBatchWord(sessionRecords);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
            String fileName = examDateStr + "_" + subjectCode + "_Lab_Claim_Form.docx";
            headers.setContentDispositionFormData("attachment", fileName);
            return new ResponseEntity<>(word, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error generating session Word doc for date {} code {}", examDateStr, subjectCode, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private List<LabClaimRecord> findSessionRecords(LabClaimRecord target) {
        List<LabClaimRecord> all = repository.findAll();
        List<LabClaimRecord> sessionRecords = new ArrayList<>();
        String targetDate = target.getExamDate() != null ? target.getExamDate().toString() : "";
        String targetCode = target.getSubjectCode() != null ? target.getSubjectCode().trim().toUpperCase() : "";
        String targetDept = target.getDepartment() != null ? target.getDepartment().trim().toUpperCase() : "";

        for (LabClaimRecord r : all) {
            String date = r.getExamDate() != null ? r.getExamDate().toString() : "";
            String code = r.getSubjectCode() != null ? r.getSubjectCode().trim().toUpperCase() : "";
            String dept = r.getDepartment() != null ? r.getDepartment().trim().toUpperCase() : "";

            if (date.equals(targetDate) && code.equals(targetCode) && (targetDept.isEmpty() || dept.equals(targetDept))) {
                if (r.getTotalAmount() == null) {
                    calculationService.calculateLabClaim(r);
                    repository.save(r);
                }
                sessionRecords.add(r);
            }
        }

        if (sessionRecords.isEmpty()) {
            sessionRecords.add(target);
        }
        return sessionRecords;
    }
}

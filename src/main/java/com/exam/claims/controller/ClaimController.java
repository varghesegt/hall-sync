package com.exam.claims.controller;

import com.exam.claims.dto.*;
import com.exam.claims.entity.ClaimRecord;
import com.exam.claims.repository.ClaimRecordRepository;
import com.exam.claims.service.*;
import com.lowagie.text.DocumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/claims")
public class ClaimController {

    private static final Logger log = LoggerFactory.getLogger(ClaimController.class);

    @Autowired
    private ExcelParserService excelParserService;

    @Autowired
    private ClaimCalculationService calculationService;

    @Autowired
    private PdfGeneratorService pdfGeneratorService;

    @Autowired
    private WordGeneratorService wordGeneratorService;

    @Autowired
    private ExcelGeneratorService excelGeneratorService;

    @Autowired
    private ClaimRecordRepository claimRecordRepository;

    /**
     * Upload and process an Excel file.
     * Parses data, calculates claims, saves to DB, and returns summary.
     */
    @PostMapping("/upload")
    @Transactional
    public ResponseEntity<UploadResponseDTO> uploadExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "examSeason", required = false) String examSeason,
            @RequestParam(value = "valuationDate", required = false) String valuationDateStr,
            @RequestParam(value = "governmentHoliday", defaultValue = "false") boolean governmentHoliday) {
        log.info("Received file upload: {} ({} bytes)", file.getOriginalFilename(), file.getSize());

        try {
            // Parse Excel
            ExcelParserService.ParseResult parseResult = excelParserService.parseExcel(file);
            List<ClaimRecord> records = parseResult.getRecords();

            if (records.isEmpty()) {
                UploadResponseDTO response = new UploadResponseDTO();
                response.setMessage("No valid records found in the uploaded file.");
                response.setWarnings(parseResult.getWarnings());
                return ResponseEntity.badRequest().body(response);
            }

            java.time.LocalDate parsedValuationDate = null;
            if (valuationDateStr != null && !valuationDateStr.trim().isEmpty()) {
                try {
                    parsedValuationDate = java.time.LocalDate.parse(valuationDateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                } catch (Exception e) {
                    log.warn("Could not parse valuationDate {}", valuationDateStr);
                }
            }

            // Filter records to keep only the ones matching the selected date (if provided)
            if (parsedValuationDate != null) {
                List<ClaimRecord> filtered = new java.util.ArrayList<>();
                for (ClaimRecord record : records) {
                    if (record.getValuationDate() != null && record.getValuationDate().equals(parsedValuationDate)) {
                        filtered.add(record);
                    }
                }
                records = filtered;
            }

            if (records.isEmpty()) {
                UploadResponseDTO response = new UploadResponseDTO();
                String msg = "No matching records found in the uploaded file";
                if (parsedValuationDate != null) {
                    msg += " for selected valuation date: " + parsedValuationDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) + ".";
                } else {
                    msg += ".";
                }
                response.setMessage(msg);
                response.setWarnings(parseResult.getWarnings());
                return ResponseEntity.badRequest().body(response);
            }

            // Assign batch ID
            UUID batchId = UUID.randomUUID();

            // Calculate and save each record
            int examiners = 0, assistants = 0, chiefs = 0;
            BigDecimal totalAmount = BigDecimal.ZERO;

            for (ClaimRecord record : records) {
                record.setBatchId(batchId);
                record.setExamSeason(examSeason);
                record.setGovernmentHoliday(governmentHoliday);
                if (parsedValuationDate != null) {
                    record.setValuationDate(parsedValuationDate);
                }
                calculationService.calculateClaim(record);

                if (record.isExaminer()) examiners++;
                else if (record.isAssistantExaminer()) assistants++;
                else if (record.isChiefExaminer()) chiefs++;

                totalAmount = totalAmount.add(record.getTotalAmount());
            }

            // Save all records
            claimRecordRepository.saveAll(records);

            // Build response
            UploadResponseDTO response = new UploadResponseDTO();
            response.setBatchId(batchId);
            response.setExamSeason(examSeason);
            response.setTotalRecords(records.size());
            response.setExaminers(examiners);
            response.setAssistantExaminers(assistants);
            response.setChiefExaminers(chiefs);
            response.setTotalAmount(totalAmount);
            response.setWarnings(parseResult.getWarnings());
            response.setMessage("Successfully processed " + records.size() + " claims.");

            log.info("Upload complete: batchId={}, records={}, total=Rs.{}", batchId, records.size(), totalAmount);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing upload", e);
            UploadResponseDTO response = new UploadResponseDTO();
            response.setMessage("Error processing file: " + e.getMessage());
            response.setWarnings(List.of(e.getMessage()));
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Get all claims in a batch.
     */
    @GetMapping("/batch/{batchId}")
    public ResponseEntity<List<ClaimResponseDTO>> getBatchClaims(@PathVariable UUID batchId) {
        List<ClaimRecord> records = claimRecordRepository.findByBatchIdOrderBySerialNumber(batchId);
        List<ClaimResponseDTO> dtos = records.stream().map(this::toDTO).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get a single claim.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ClaimResponseDTO> getClaim(@PathVariable Long id) {
        return claimRecordRepository.findById(id)
            .map(record -> ResponseEntity.ok(toDTO(record)))
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all batch summaries.
     */
    @GetMapping("/batches")
    public ResponseEntity<List<BatchSummaryDTO>> getAllBatches() {
        List<UUID> batchIds = claimRecordRepository.findDistinctBatchIds();
        List<BatchSummaryDTO> summaries = new ArrayList<>();

        for (UUID batchId : batchIds) {
            BatchSummaryDTO summary = new BatchSummaryDTO();
            summary.setBatchId(batchId);

            List<ClaimRecord> records = claimRecordRepository.findByBatchIdOrderBySerialNumber(batchId);
            summary.setTotalRecords(records.size());
            summary.setTotalAmount(claimRecordRepository.sumTotalAmountByBatchId(batchId));

            if (!records.isEmpty()) {
                summary.setCreatedAt(records.get(0).getCreatedAt() != null
                    ? records.get(0).getCreatedAt().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"))
                    : "");
                summary.setExamSeason(records.get(0).getExamSeason());
                summary.setValuationDate(records.get(0).getValuationDate() != null
                    ? records.get(0).getValuationDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) : null);
            }

            long examiners = records.stream().filter(ClaimRecord::isExaminer).count();
            long assistants = records.stream().filter(ClaimRecord::isAssistantExaminer).count();
            long chiefs = records.stream().filter(ClaimRecord::isChiefExaminer).count();
            summary.setExaminers(examiners);
            summary.setAssistantExaminers(assistants);
            summary.setChiefExaminers(chiefs);

            summaries.add(summary);
        }

        return ResponseEntity.ok(summaries);
    }

    /**
     * Download batch as PDF.
     */
    @GetMapping("/batch/{batchId}/download/pdf")
    public ResponseEntity<byte[]> downloadBatchPdf(@PathVariable UUID batchId) {
        try {
            List<ClaimRecord> records = claimRecordRepository.findByBatchIdOrderBySerialNumber(batchId);
            if (records.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            byte[] pdf = pdfGeneratorService.generateBatchPdf(records);

            String datePart = records.get(0).getValuationDate() != null
                ? records.get(0).getValuationDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
                : batchId.toString().substring(0, 8);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.attachment()
                .filename(datePart + " - CLAIMS.pdf").build());
            headers.setContentLength(pdf.length);

            return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
        } catch (DocumentException | IOException e) {
            log.error("Error generating PDF", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Download batch as Word document.
     */
    @GetMapping("/batch/{batchId}/download/word")
    public ResponseEntity<byte[]> downloadBatchWord(@PathVariable UUID batchId) {
        try {
            List<ClaimRecord> records = claimRecordRepository.findByBatchIdOrderBySerialNumber(batchId);
            if (records.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            byte[] word = wordGeneratorService.generateBatchWord(records);

            String datePart = records.get(0).getValuationDate() != null
                ? records.get(0).getValuationDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
                : batchId.toString().substring(0, 8);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
            headers.setContentDisposition(ContentDisposition.attachment()
                .filename(datePart + " - CLAIMS.docx").build());
            headers.setContentLength(word.length);

            return new ResponseEntity<>(word, headers, HttpStatus.OK);
        } catch (IOException e) {
            log.error("Error generating Word", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Download batch as Consolidated Excel sheet (Default to Sheet 1).
     */
    @GetMapping("/batch/{batchId}/download/excel")
    public ResponseEntity<byte[]> downloadBatchExcel(@PathVariable UUID batchId) {
        return downloadBatchExcel1(batchId);
    }

    /**
     * Download batch as Consolidated Excel Sheet 1.
     */
    @GetMapping("/batch/{batchId}/download/excel1")
    public ResponseEntity<byte[]> downloadBatchExcel1(@PathVariable UUID batchId) {
        try {
            List<ClaimRecord> records = claimRecordRepository.findByBatchIdOrderBySerialNumber(batchId);
            if (records.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            byte[] excel = excelGeneratorService.generateBatchExcel1(records);

            String datePart = records.get(0).getValuationDate() != null
                ? records.get(0).getValuationDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
                : batchId.toString().substring(0, 8);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDisposition(ContentDisposition.attachment()
                .filename(datePart + " - CONSOLIDATED SHEET 1.xlsx").build());
            headers.setContentLength(excel.length);

            return new ResponseEntity<>(excel, headers, HttpStatus.OK);
        } catch (IOException e) {
            log.error("Error generating Excel 1", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Download batch as Consolidated Excel Sheet 2.
     */
    @GetMapping("/batch/{batchId}/download/excel2")
    public ResponseEntity<byte[]> downloadBatchExcel2(@PathVariable UUID batchId) {
        try {
            List<ClaimRecord> records = claimRecordRepository.findByBatchIdOrderBySerialNumber(batchId);
            if (records.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            byte[] excel = excelGeneratorService.generateBatchExcel2(records);

            String datePart = records.get(0).getValuationDate() != null
                ? records.get(0).getValuationDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
                : batchId.toString().substring(0, 8);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDisposition(ContentDisposition.attachment()
                .filename(datePart + " - CONSOLIDATED SHEET 2.xlsx").build());
            headers.setContentLength(excel.length);

            return new ResponseEntity<>(excel, headers, HttpStatus.OK);
        } catch (IOException e) {
            log.error("Error generating Excel 2", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Download single claim as PDF.
     */
    @GetMapping("/{id}/download/pdf")
    public ResponseEntity<byte[]> downloadSinglePdf(@PathVariable Long id) {
        try {
            Optional<ClaimRecord> optRecord = claimRecordRepository.findById(id);
            if (optRecord.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            byte[] pdf = pdfGeneratorService.generateSinglePdf(optRecord.get());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.attachment()
                .filename("claim_" + id + ".pdf").build());

            return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
        } catch (DocumentException | IOException e) {
            log.error("Error generating PDF for claim {}", id, e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Download single claim as Word.
     */
    @GetMapping("/{id}/download/word")
    public ResponseEntity<byte[]> downloadSingleWord(@PathVariable Long id) {
        try {
            Optional<ClaimRecord> optRecord = claimRecordRepository.findById(id);
            if (optRecord.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            byte[] word = wordGeneratorService.generateSingleWord(optRecord.get());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
            headers.setContentDisposition(ContentDisposition.attachment()
                .filename("claim_" + id + ".docx").build());

            return new ResponseEntity<>(word, headers, HttpStatus.OK);
        } catch (IOException e) {
            log.error("Error generating Word for claim {}", id, e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Delete a batch.
     */
    @DeleteMapping("/batch/{batchId}")
    @Transactional
    public ResponseEntity<Map<String, String>> deleteBatch(@PathVariable UUID batchId) {
        long count = claimRecordRepository.countByBatchId(batchId);
        if (count == 0) {
            return ResponseEntity.notFound().build();
        }
        claimRecordRepository.deleteByBatchId(batchId);
        return ResponseEntity.ok(Map.of("message", "Deleted " + count + " claims from batch " + batchId));
    }

    // ==================== DTO MAPPING ====================

    private ClaimResponseDTO toDTO(ClaimRecord record) {
        ClaimResponseDTO dto = new ClaimResponseDTO();
        dto.setId(record.getId());
        dto.setBatchId(record.getBatchId());
        dto.setSerialNumber(record.getSerialNumber());
        dto.setExamMonth(record.getExamMonth());
        dto.setExamSeason(record.getExamSeason());
        dto.setValuationDate(record.getValuationDate() != null
            ? record.getValuationDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) : null);
        dto.setGovernmentHoliday(record.isGovernmentHoliday());
        dto.setSessionsAttended(record.getSessionsAttended());
        dto.setBoardName(record.getBoardName());
        dto.setPostHeld(record.getPostHeld());
        dto.setMobileNo(record.getMobileNo());
        dto.setNameTitle(record.getNameTitle());
        dto.setStaffName(record.getStaffName());
        dto.setFacultyType(record.getFacultyType());
        dto.setDesignation(record.getDesignation());
        dto.setInstitutionName(record.getInstitutionName());
        dto.setInstitutionCode(record.getInstitutionCode());
        dto.setIssueRegPageNo(record.getIssueRegPageNo());
        dto.setBankAccountNumber(record.getBankAccountNumber());
        dto.setIfscCode(record.getIfscCode());
        dto.setBankName(record.getBankName());
        dto.setBranch(record.getBranch());
        dto.setDistanceKm(record.getDistanceKm());
        dto.setTotalScripts(record.getTotalScripts());
        dto.setFnScripts(record.getFnScripts());
        dto.setAnScripts(record.getAnScripts());
        dto.setScriptAmount(record.getScriptAmount());
        dto.setTravellingAllowance(record.getTravellingAllowance());
        dto.setDearnessAllowance(record.getDearnessAllowance());
        dto.setTotalAmount(record.getTotalAmount());
        dto.setAmountInWords(record.getAmountInWords());
        dto.setMaxScriptsValued(record.getMaxScriptsValued());
        dto.setMaxScriptsAmount(record.getMaxScriptsAmount());
        dto.setTenPercentAmount(record.getTenPercentAmount());
        dto.setOverallScriptAmount(record.getOverallScriptAmount());
        dto.setCreatedAt(record.getCreatedAt() != null
            ? record.getCreatedAt().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")) : null);

        if (record.getScriptDetails() != null) {
            dto.setScriptDetails(record.getScriptDetails().stream()
                .map(sd -> new ScriptDetailDTO(sd.getSessionType(), sd.getSerialNumber(),
                    sd.getSubjectCode(), sd.getNoOfScripts()))
                .collect(Collectors.toList()));
        }

        return dto;
    }
}

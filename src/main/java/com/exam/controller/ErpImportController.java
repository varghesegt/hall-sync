package com.exam.controller;

import com.exam.dto.ImportResult;
import com.exam.service.ErpImportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/integrations/import")

public class ErpImportController {

    private final ErpImportService importService;

    public ErpImportController(ErpImportService importService) {
        this.importService = importService;
    }

    @PostMapping("/excel")
    public ResponseEntity<ImportResult> importExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam("sessionId") UUID sessionId) {
        return ResponseEntity.ok(importService.importFromExcel(file, sessionId));
    }

    @PostMapping("/camu")
    public ResponseEntity<ImportResult> importCamu(
            @RequestParam("file") MultipartFile file,
            @RequestParam("sessionId") UUID sessionId) {
        return ResponseEntity.ok(importService.importFromCamuCsv(file, sessionId));
    }

    @PostMapping("/icloudems")
    public ResponseEntity<ImportResult> importICloudEms(
            @RequestParam("file") MultipartFile file,
            @RequestParam("sessionId") UUID sessionId) {
        return ResponseEntity.ok(importService.importFromICloudEmsCsv(file, sessionId));
    }

    @GetMapping("/templates/{type}")
    public ResponseEntity<byte[]> downloadTemplate(@PathVariable String type) throws IOException {
        byte[] data = importService.generateTemplate(type);

        String filename;
        String contentType;
        switch (type.toLowerCase()) {
            case "excel":
                filename = "HallSync_Import_Template.xlsx";
                contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                break;
            case "camu":
                filename = "Camu_Import_Template.csv";
                contentType = "text/csv";
                break;
            case "icloudems":
                filename = "iCloudEMS_Import_Template.csv";
                contentType = "text/csv";
                break;
            default:
                return ResponseEntity.badRequest().build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentDispositionFormData("attachment", filename);

        return ResponseEntity.ok().headers(headers).body(data);
    }
}

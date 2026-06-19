package com.exam.controller.internal;

import com.exam.dto.UploadResponse;
import com.exam.service.internal.InternalUploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/uploads")
public class InternalUploadController {

    private static final Logger logger = LoggerFactory.getLogger(InternalUploadController.class);
    private final InternalUploadService uploadService;

    public InternalUploadController(InternalUploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping(value = "/excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadExcel(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    java.util.Map.of("error", "BAD_REQUEST", "message", "File is missing or empty"));
        }

        String filename = file.getOriginalFilename();
        if (filename != null && !filename.toLowerCase().endsWith(".xlsx")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    java.util.Map.of("error", "BAD_REQUEST", "message", "Only .xlsx files are accepted"));
        }

        logger.info("Internal Excel upload: {}, size: {}", file.getOriginalFilename(), file.getSize());

        try {
            return ResponseEntity.ok(uploadService.uploadExcelFile(file));
        } catch (com.exam.exception.PreflightValidationException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to process Excel upload", e);
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(
                    java.util.Map.of("error", "PREFLIGHT_FAILED",
                            "message", "Failed to parse Excel: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"),
                            "details", java.util.Collections.emptyList()));
        }
    }

    @GetMapping("/{fileId}/preview")
    public ResponseEntity<?> getPreview(@PathVariable UUID fileId) {
        return ResponseEntity.ok(uploadService.getPreview(fileId));
    }
}

package com.exam.controller;

import com.exam.dto.UploadResponse;
import com.exam.service.UploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/uploads")
public class UploadController {

    @jakarta.annotation.PostConstruct
    public void init() {
        logger.info("UploadController initialized and mapped at /api/v1/uploads");
    }

    private static final Logger logger = LoggerFactory.getLogger(UploadController.class);
    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping(
        value = "/pdf",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> uploadPdf(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                java.util.Map.of(
                    "error", "BAD_REQUEST",
                    "message", "File is missing or empty"
                )
            );
        }

        logger.info("Received file: {}, size: {}", file.getOriginalFilename(), file.getSize());

        try {
            return ResponseEntity.ok(uploadService.uploadFile(file));
        } catch (com.exam.exception.PreflightValidationException e) {
            throw e; // Handled as 422 by GlobalExceptionHandler
        } catch (Exception e) {
            logger.error("Failed to parse or process upload", e);
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(
                java.util.Map.of(
                    "error", "PREFLIGHT_FAILED",
                    "message", "Failed to parse document: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"),
                    "details", java.util.Collections.emptyList()
                )
            );
        }
    }

    @GetMapping("/{fileId}/preview")
    public ResponseEntity<?> getPreview(@PathVariable java.util.UUID fileId) {
        return ResponseEntity.ok(uploadService.getPreview(fileId));
    }
}

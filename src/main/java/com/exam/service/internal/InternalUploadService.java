package com.exam.service.internal;

import com.exam.dto.UploadResponse;
import com.exam.dto.StudentPreviewDto;
import com.exam.dto.StudentPreviewResponse;
import com.exam.entity.UploadedFile;
import com.exam.exception.DuplicateFileException;
import com.exam.exception.PreflightValidationException;
import com.exam.parser.ExcelParserPipeline;
import com.exam.parser.model.ParseError;
import com.exam.parser.model.ParseResult;
import com.exam.parser.model.ParseStatus;
import com.exam.repository.UploadedFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Upload service for Internal Exam mode.
 * Handles Excel (.xlsx) file uploads using ExcelParserPipeline.
 */
@Service
public class InternalUploadService {

    private static final Logger logger = LoggerFactory.getLogger(InternalUploadService.class);
    private static final long MAX_FILE_SIZE_BYTES = 50L * 1024 * 1024; // 50MB

    private final ExcelParserPipeline excelParser;
    private final UploadedFileRepository fileRepository;

    public InternalUploadService(ExcelParserPipeline excelParser, UploadedFileRepository fileRepository) {
        this.excelParser = excelParser;
        this.fileRepository = fileRepository;
    }

    public UploadResponse uploadExcelFile(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new PreflightValidationException("File exceeds maximum size of 50MB",
                    List.of("File size: " + file.getSize() + " bytes"));
        }

        byte[] excelBytes;
        ParseResult result;
        try {
            excelBytes = file.getBytes();
            result = excelParser.parse(excelBytes);
        } catch (Exception e) {
            logger.error("Excel upload failed", e);
            throw new RuntimeException("Excel processing failed", e);
        }

        // Persist file metadata
        String status = result.status() == ParseStatus.FAILED ? "PARSE_FAILED" : "PARSED";
        UploadedFile uploadedFile = new UploadedFile(
                UUID.randomUUID(),
                file.getOriginalFilename(),
                status,
                result.sourceHash()
        );
        uploadedFile.setExamType("INTERNAL");

        UploadedFile finalFile = uploadedFile;
        try {
            fileRepository.save(uploadedFile);
        } catch (DataIntegrityViolationException e) {
            // Idempotent duplicate handling
            var existing = fileRepository.findBySha256Hash(result.sourceHash());
            if (existing.isPresent()) {
                logger.info("Duplicate Excel upload detected: fileId={}", existing.get().getId());
                finalFile = existing.get();
                status = existing.get().getStatus();
            } else {
                throw new DuplicateFileException("Hash conflict: " + result.sourceHash());
            }
        }

        // Cache parse result
        fileRepository.cacheParseResult(finalFile.getId(), result, "internal-v1");
        fileRepository.storeFileBytes(finalFile.getId(), excelBytes);

        // Check for FATAL errors
        if (result.status() == ParseStatus.FAILED) {
            List<String> failures = result.errors().stream()
                    .filter(e -> "FATAL".equals(e.severity()))
                    .map(ParseError::message)
                    .collect(Collectors.toList());
            throw new PreflightValidationException(
                    "Excel parsing failed with " + failures.size() + " fatal errors", failures);
        }

        logger.info("Excel upload accepted: fileId={}, students={}", finalFile.getId(), result.students().size());

        if (finalFile != uploadedFile) {
            return UploadResponse.duplicate(finalFile.getId(), file.getOriginalFilename(), result.sourceHash());
        }

        return new UploadResponse(finalFile.getId(), file.getOriginalFilename(), result.sourceHash(), status);
    }

    public StudentPreviewResponse getPreview(UUID fileId) {
        var cached = fileRepository.getCachedParseResult(fileId, "internal-v1");

        if (cached == null) {
            var metadata = fileRepository.findById(fileId);
            if (metadata.isPresent()) {
                byte[] bytes = fileRepository.getFileBytes(fileId);
                if (bytes != null) {
                    try {
                        var result = excelParser.parse(bytes);
                        fileRepository.cacheParseResult(fileId, result, "internal-v1");
                        cached = result;
                    } catch (Exception e) {
                        logger.error("Failed to re-parse Excel for preview: {}", fileId, e);
                    }
                }
            }
        }

        if (cached == null) {
            return new StudentPreviewResponse(fileId, 0, Collections.emptyList());
        }

        List<StudentPreviewDto> studentDtos = cached.students().stream()
                .map(s -> StudentPreviewDto.fromParser(
                        s.registerNumber(), s.studentName(),
                        s.department(), s.className(),
                        s.subjectName(), s.subjectCode(),
                        s.semester(), s.regulation()))
                .collect(Collectors.toList());

        return new StudentPreviewResponse(fileId, studentDtos.size(), studentDtos);
    }
}

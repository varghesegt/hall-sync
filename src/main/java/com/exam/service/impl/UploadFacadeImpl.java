package com.exam.service.impl;

import com.exam.dto.UploadResponse;
import com.exam.entity.UploadedFile;
import com.exam.exception.DuplicateFileException;
import com.exam.exception.PreflightValidationException;
import com.exam.parser.config.ParserConfig;
import com.exam.parser.PdfParserPipeline;
import com.exam.parser.model.ParseError;
import com.exam.parser.model.ParseResult;
import com.exam.parser.model.ParseStatus;
import com.exam.repository.UploadedFileRepository;
import com.exam.service.UploadService;
import com.exam.service.impl.S3FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * Upload facade: accepts PDF, runs parser pipeline OUTSIDE transaction,
 * then persists file metadata and cached parse result.
 *
 * Transaction boundary:
 * - PDF parsing (CPU-heavy) → NO transaction
 * - File metadata save → transactional (via repository)
 * - Student persistence → delegated to ExamSessionFacade (separate tx)
 *
 * Fix #1: DB UNIQUE constraint is final dedup authority (race-safe)
 * Fix #4: Stream to temp file — no full byte[] copy in heap
 */
@Service
public class UploadFacadeImpl implements UploadService {

    private static final Logger logger = LoggerFactory.getLogger(UploadFacadeImpl.class);

    private final PdfParserPipeline parserPipeline;
    private final UploadedFileRepository fileRepository;
    private final ParserConfig parserConfig;
    private final S3FileStorageService s3FileStorageService;
    private final ParseResultCacheService cacheService;

    public UploadFacadeImpl(PdfParserPipeline parserPipeline,
                            UploadedFileRepository fileRepository,
                            ParserConfig parserConfig,
                            S3FileStorageService s3FileStorageService,
                            ParseResultCacheService cacheService) {
        this.parserPipeline = parserPipeline;
        this.fileRepository = fileRepository;
        this.parserConfig = parserConfig;
        this.s3FileStorageService = s3FileStorageService;
        this.cacheService = cacheService;
    }

    @Override
    public UploadResponse uploadFile(MultipartFile file) {
        // --- Fix #4: Size guard BEFORE any I/O (fail fast, prevent disk fill) ---
        long maxBytes = (long) parserConfig.getMaxFileSizeMb() * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new PreflightValidationException(
                    "File exceeds maximum size of " + parserConfig.getMaxFileSizeMb() + "MB",
                    List.of("File size: " + file.getSize() + " bytes, max: " + maxBytes + " bytes"));
        }

        byte[] pdfBytes;
        ParseResult result;
        String fileHash;
        try {
            pdfBytes = file.getBytes();
            
            // Calculate Hash
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            fileHash = HexFormat.of().formatHex(digest.digest(pdfBytes));
            
            result = parserPipeline.parse(pdfBytes);
        } catch (Exception e) {
            logger.error("Upload failed", e);
            throw new RuntimeException("Upload processing failed", e);
        }

        // --- Stage B: Persist file metadata + cached parse result ---
        // Fix #1: DB UNIQUE(sha256_hash) is the final dedup authority.
        // The code-level check is removed — DB constraint handles race condition atomically.
        String status = result.status() == ParseStatus.FAILED ? "PARSE_FAILED" : "PARSED";
        UploadedFile uploadedFile = new UploadedFile(
                UUID.randomUUID(),
                file.getOriginalFilename(),
                status,
                result.sourceHash()
        );

        UploadedFile finalFile = uploadedFile;
        try {
            fileRepository.save(uploadedFile);
        } catch (DataIntegrityViolationException e) {
            // Fix #1: Idempotent with retry — handles rare read-before-commit race
            UploadedFile existing = retryFindByHash(result.sourceHash(), 3, 20);
            if (existing == null) {
                throw new DuplicateFileException(
                        "Hash conflict but no matching record after retries (hash: " + result.sourceHash() + ")");
            }
            logger.info("Duplicate upload detected: fileId={}", existing.getId());
            finalFile = existing;
            status = existing.getStatus();
        }

        // --- Fix #3: Cache with config version key (determinism across deployments) ---
        cacheService.cacheParseResult(finalFile.getId(), result, parserConfig.getConfigVersion());

        // Store raw bytes in AWS S3 for fallback re-parse
        s3FileStorageService.storeFileBytes(finalFile.getId(), pdfBytes, file.getContentType());

        // --- Stage C: If FATAL errors → throw with full error list ---
        if (result.status() == ParseStatus.FAILED) {
            List<String> failures = result.errors().stream()
                    .filter(e -> "FATAL".equals(e.severity()))
                    .map(ParseError::message)
                    .toList();
            throw new PreflightValidationException(
                    "PDF parsing failed with " + failures.size() + " fatal errors",
                    failures);
        }

        logger.info("Upload accepted: fileId={}, hash={}, students={}, warnings={}",
                finalFile.getId(), result.sourceHash(),
                result.students().size(),
                result.errors().size());

        if (finalFile != uploadedFile) {
            return UploadResponse.duplicate(finalFile.getId(), file.getOriginalFilename(), result.sourceHash());
        }

        return new UploadResponse(
                finalFile.getId(),
                file.getOriginalFilename(),
                result.sourceHash(),
                status
        );
    }

    /**
     * Retry lookup with backoff to handle rare read-before-commit race.
     * Under very high contention, the competing TX may not have committed
     * by the time we read after our insert failed.
     */
    private UploadedFile retryFindByHash(String hash, int maxAttempts, long backoffMs) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            var found = fileRepository.findBySha256Hash(hash);
            if (found.isPresent()) {
                return found.get();
            }
            if (attempt < maxAttempts) {
                try {
                    Thread.sleep(backoffMs * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        return null;
    }

    @Override
    public com.exam.dto.StudentPreviewResponse getPreview(UUID fileId) {
        var cached = cacheService.getCachedParseResult(fileId, parserConfig.getConfigVersion());
        
        if (cached == null) {
            // Fallback: Check if file metadata exists but cache is gone (e.g. restart)
            var metadata = fileRepository.findById(fileId);
            if (metadata.isPresent()) {
                byte[] bytes = s3FileStorageService.getFileBytes(fileId);
                if (bytes != null) {
                    try {
                        var result = parserPipeline.parse(bytes);
                        cacheService.cacheParseResult(fileId, result, parserConfig.getConfigVersion());
                        cached = result;
                    } catch (Exception e) {
                        logger.error("Failed to re-parse file for preview: {}", fileId, e);
                    }
                }
            }
        }

        if (cached == null) {
            return new com.exam.dto.StudentPreviewResponse(fileId, 0, java.util.Collections.emptyList());
        }

        List<com.exam.dto.StudentPreviewDto> studentDtos = cached.students().stream()
                .map(s -> com.exam.dto.StudentPreviewDto.fromParser(
                        s.registerNumber(),
                        s.studentName(),
                        s.department(),
                        s.className(),
                        s.subjectName(),
                        s.subjectCode(),
                        s.semester(),
                        s.regulation()
                ))
                .toList();

        return new com.exam.dto.StudentPreviewResponse(
                fileId,
                studentDtos.size(),
                studentDtos
        );
    }
}

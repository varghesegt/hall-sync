package com.exam.service.impl;

import com.exam.dto.SessionCreateRequest;
import com.exam.dto.SessionCreateResponse;
import com.exam.entity.ExamSession;
import com.exam.entity.Student;
import com.exam.entity.UploadedFile;
import com.exam.exception.PreflightValidationException;
import com.exam.exception.ResourceNotFoundException;
import com.exam.parser.PdfParserPipeline;
import com.exam.parser.config.ParserConfig;
import com.exam.parser.model.ParseError;
import com.exam.parser.model.ParseResult;
import com.exam.parser.model.ParseStatus;
import com.exam.parser.model.StudentRow;
import com.exam.repository.ExamSessionRepository;
import com.exam.repository.StudentRepository;
import com.exam.repository.UploadedFileRepository;
import com.exam.service.ExamSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * ExamSession facade: links uploaded file → resolves parse result → persists session + students.
 *
 * Architecture:
 * - Parse result resolution → OUTSIDE transaction (uses cache, re-parse as fallback)
 * - Session + student persistence → INSIDE single transaction (atomic)
 * - Unresolved departments → session created but allocation BLOCKED
 *
 * Fix #2: Use cached parse result from upload. Re-parse is fallback only.
 */
@Service
public class ExamSessionFacadeImpl implements ExamSessionService {

    private static final Logger logger = LoggerFactory.getLogger(ExamSessionFacadeImpl.class);

    private final ExamSessionRepository sessionRepository;
    private final StudentRepository studentRepository;
    private final UploadedFileRepository fileRepository;
    private final PdfParserPipeline parserPipeline;
    private final ParserConfig parserConfig;

    public ExamSessionFacadeImpl(ExamSessionRepository sessionRepository,
                                  StudentRepository studentRepository,
                                  UploadedFileRepository fileRepository,
                                  PdfParserPipeline parserPipeline,
                                  ParserConfig parserConfig) {
        this.sessionRepository = sessionRepository;
        this.studentRepository = studentRepository;
        this.fileRepository = fileRepository;
        this.parserPipeline = parserPipeline;
        this.parserConfig = parserConfig;
    }

    @Override
    @Transactional
    public SessionCreateResponse createSession(SessionCreateRequest request) {
        // --- Step 1: Resolve uploaded file ---
        UploadedFile uploadedFile = fileRepository.findById(request.fileId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Uploaded file not found: " + request.fileId()));

        if ("PARSE_FAILED".equals(uploadedFile.getStatus())) {
            throw new PreflightValidationException(
                    "Cannot create session from a failed upload",
                    List.of("File " + request.fileId() + " has status PARSE_FAILED"));
        }

        // --- Step 2: Resolve parse result (cache-first, re-parse fallback) ---
        ParseResult parseResult = resolveParseResult(request.fileId());

        // Double-check: if result has FATAL errors, reject
        if (parseResult.status() == ParseStatus.FAILED) {
            List<String> failures = parseResult.errors().stream()
                    .filter(e -> "FATAL".equals(e.severity()))
                    .map(ParseError::message)
                    .toList();
            throw new PreflightValidationException(
                    "Parse validation failed", failures);
        }

        // --- Step 3: Check for unresolved departments ---
        boolean hasUnresolved = parseResult.students().stream()
                .anyMatch(StudentRow::requiresManualResolution);

        // --- Step 4: Persist session + students atomically ---
        UUID sessionId = persistSessionAndStudents(
                request, parseResult.students());

        // --- Step 5: Update file status ---
        uploadedFile.setStatus("SESSION_CREATED");
        fileRepository.save(uploadedFile);

        // --- Step 6: Extract warnings ---
        List<ParseError> warnings = parseResult.errors().stream()
                .filter(e -> "WARNING".equals(e.severity()))
                .toList();

        String status;
        if (hasUnresolved) {
            status = "CREATED_WITH_UNRESOLVED_DEPARTMENTS";
            logger.warn("Session {} created with {} unresolved departments — allocation BLOCKED",
                    sessionId, parseResult.students().stream()
                            .filter(StudentRow::requiresManualResolution).count());
        } else if (!warnings.isEmpty()) {
            status = "CREATED_WITH_WARNINGS";
        } else {
            status = "CREATED";
        }

        logger.info("Session created: id={}, students={}, status={}",
                sessionId, parseResult.students().size(), status);

        return new SessionCreateResponse(
                sessionId,
                status,
                parseResult.students().size(),
                hasUnresolved,
                warnings
        );
    }

    /**
     * Fix #2: Cache-first parse result resolution.
     * - Try cached result first (avoids CPU-expensive re-parse)
     * - Fall back to re-parse from stored bytes if cache miss
     * - Re-parse guarantees determinism as a safety net
     */
    private ParseResult resolveParseResult(UUID fileId) {
        // Try cache first (versioned — stale entries from old config are ignored)
        String currentVersion = parserConfig.getConfigVersion();
        ParseResult cached = fileRepository.getCachedParseResult(fileId, currentVersion);
        if (cached != null) {
            logger.debug("Using cached parse result for fileId={}, configVersion={}", fileId, currentVersion);
            return cached;
        }

        // Fallback: re-parse from stored bytes
        logger.info("Cache miss for fileId={} (configVersion={}), falling back to re-parse",
                fileId, currentVersion);
        byte[] pdfBytes = fileRepository.getFileBytes(fileId);
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new RuntimeException("File data is missing");
        }
        return parserPipeline.parse(pdfBytes);
    }

    /**
     * Atomic persistence: session + all students in a single transaction.
     * If any student fails → entire transaction rolls back. No partial saves.
     */
    private UUID persistSessionAndStudents(SessionCreateRequest request,
                                             List<StudentRow> students) {
        UUID sessionId = UUID.randomUUID();
        ExamSession session = new ExamSession(
                sessionId,
                request.seasonId(),
                request.examName(),
                request.examDate(),
                request.session()
        );
        sessionRepository.save(session);

        List<Student> entities = students.stream()
                .map(s -> new Student(
                        UUID.randomUUID(),
                        session,
                        s.registerNumber(),
                        s.studentName(),
                        s.department() != null ? s.department() : "UNRESOLVED",
                        s.className(),
                        s.subjectName(),
                        s.subjectCode(),
                        s.semester(),
                        s.regulation()
                ))
                .toList();

        studentRepository.saveAll(entities);

        logger.info("Persisted {} students for session {}", entities.size(), sessionId);
        return sessionId;
    }
}

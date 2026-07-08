package com.exam.service.internal;

import com.exam.dto.SessionCreateRequest;
import com.exam.dto.SessionCreateResponse;
import com.exam.entity.ExamSession;
import com.exam.entity.Student;
import com.exam.entity.UploadedFile;
import com.exam.exception.PreflightValidationException;
import com.exam.exception.ResourceNotFoundException;
import com.exam.parser.ExcelParserPipeline;
import com.exam.parser.model.ParseError;
import com.exam.parser.model.ParseResult;
import com.exam.parser.model.ParseStatus;
import com.exam.parser.model.StudentRow;
import com.exam.repository.ExamSessionRepository;
import com.exam.repository.StudentRepository;
import com.exam.repository.UploadedFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.exam.service.impl.S3FileStorageService;
import com.exam.service.impl.ParseResultCacheService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Internal Exam session creation service.
 * Links uploaded Excel file → resolves parse result → persists session + students.
 * Sets exam_type = INTERNAL on the session.
 */
@Service
public class InternalExamSessionService {

    private static final Logger logger = LoggerFactory.getLogger(InternalExamSessionService.class);

    private final ExamSessionRepository sessionRepository;
    private final StudentRepository studentRepository;
    private final UploadedFileRepository fileRepository;
    private final ExcelParserPipeline excelParser;
    private final S3FileStorageService s3FileStorageService;
    private final ParseResultCacheService cacheService;

    public InternalExamSessionService(ExamSessionRepository sessionRepository,
                                       StudentRepository studentRepository,
                                       UploadedFileRepository fileRepository,
                                       ExcelParserPipeline excelParser,
                                       S3FileStorageService s3FileStorageService,
                                       ParseResultCacheService cacheService) {
        this.sessionRepository = sessionRepository;
        this.studentRepository = studentRepository;
        this.fileRepository = fileRepository;
        this.excelParser = excelParser;
        this.s3FileStorageService = s3FileStorageService;
        this.cacheService = cacheService;
    }

    @Transactional
    public SessionCreateResponse createSession(SessionCreateRequest request) {
        // Step 1: Resolve uploaded file
        UploadedFile uploadedFile = fileRepository.findById(request.fileId())
                .orElseThrow(() -> new ResourceNotFoundException("Uploaded file not found: " + request.fileId()));

        if ("PARSE_FAILED".equals(uploadedFile.getStatus())) {
            throw new PreflightValidationException("Cannot create session from a failed upload",
                    List.of("File " + request.fileId() + " has status PARSE_FAILED"));
        }

        // Step 2: Resolve parse result (cache-first, re-parse fallback)
        ParseResult parseResult = resolveParseResult(request.fileId());

        if (parseResult.status() == ParseStatus.FAILED) {
            List<String> failures = parseResult.errors().stream()
                    .filter(e -> "FATAL".equals(e.severity()))
                    .map(ParseError::message)
                    .collect(Collectors.toList());
            throw new PreflightValidationException("Parse validation failed", failures);
        }

        // Step 3: Check for unresolved departments
        boolean hasUnresolved = parseResult.students().stream().anyMatch(StudentRow::requiresManualResolution);

        // Step 4: Persist session + students atomically (INTERNAL exam type)
        UUID sessionId = persistSessionAndStudents(request, parseResult.students());

        // Step 5: Update file status
        uploadedFile.setStatus("SESSION_CREATED");
        fileRepository.save(uploadedFile);

        // Step 6: Extract warnings
        List<ParseError> warnings = parseResult.errors().stream()
                .filter(e -> "WARNING".equals(e.severity())).collect(Collectors.toList());

        String status;
        if (hasUnresolved) {
            status = "CREATED_WITH_UNRESOLVED_DEPARTMENTS";
        } else if (!warnings.isEmpty()) {
            status = "CREATED_WITH_WARNINGS";
        } else {
            status = "CREATED";
        }

        logger.info("Internal session created: id={}, students={}, status={}", sessionId, parseResult.students().size(), status);

        return new SessionCreateResponse(sessionId, status, parseResult.students().size(), hasUnresolved, warnings);
    }

    private ParseResult resolveParseResult(UUID fileId) {
        // Try cache first
        ParseResult cached = cacheService.getCachedParseResult(fileId, "internal-v1");
        if (cached != null) {
            return cached;
        }

        logger.info("Cache miss for fileId={}, falling back to re-parse", fileId);
        // Fallback: re-parse from stored bytes
        byte[] excelBytes = s3FileStorageService.getFileBytes(fileId);
        if (excelBytes == null || excelBytes.length == 0) {
            throw new RuntimeException("File data is missing from cloud storage");
        }
        return excelParser.parse(excelBytes);
    }

    private UUID persistSessionAndStudents(SessionCreateRequest request, List<StudentRow> students) {
        UUID sessionId = UUID.randomUUID();
        ExamSession session = new ExamSession(
                sessionId, request.seasonId(), request.examName(),
                request.examDate(), request.session(), "INTERNAL"
        );
        sessionRepository.save(session);

        List<Student> entities = students.stream()
                .map(s -> new Student(
                        UUID.randomUUID(), session,
                        s.registerNumber(), s.studentName(),
                        s.department() != null ? s.department() : "UNRESOLVED",
                        s.className(), s.subjectName(), s.subjectCode(),
                        s.semester(), s.regulation()))
                .collect(Collectors.toList());

        studentRepository.saveAll(entities);
        logger.info("Persisted {} internal students for session {}", entities.size(), sessionId);
        return sessionId;
    }
}

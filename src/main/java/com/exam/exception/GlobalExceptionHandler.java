package com.exam.exception;

import com.exam.dto.ErrorResponse;
import com.exam.dto.ValidationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DuplicateFileException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateFile(DuplicateFileException ex) {
        logError("DuplicateFileException", ex);
        ErrorResponse response = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                ErrorCode.DUPLICATE_FILE,
                ex.getMessage(),
                Collections.emptyList(),
                resolveId(ex)
        );
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(PreflightValidationException.class)
    public ResponseEntity<ErrorResponse> handlePreflightValidation(PreflightValidationException ex) {
        logError("PreflightValidationException", ex);
        if (ex.getFailures() != null && !ex.getFailures().isEmpty()) {
            logger.error("Preflight Fatal Errors (first 5): {}", 
                ex.getFailures().stream().limit(5).collect(Collectors.toList()));
        }
        ErrorResponse response = new ErrorResponse(
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                ErrorCode.PREFLIGHT_FAILED,
                ex.getMessage(),
                ex.getFailures() != null ? ex.getFailures() : Collections.emptyList(),
                resolveId(ex)
        );
        return new ResponseEntity<>(response, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(AllocationInProgressException.class)
    public ResponseEntity<ErrorResponse> handleAllocationInProgress(AllocationInProgressException ex) {
        logError("AllocationInProgressException", ex);
        ErrorResponse response = new ErrorResponse(
                HttpStatus.LOCKED.value(),
                ErrorCode.ALLOCATION_LOCKED,
                ex.getMessage(),
                Collections.emptyList(),
                resolveId(ex)
        );
        return new ResponseEntity<>(response, HttpStatus.LOCKED);
    }

    @ExceptionHandler(CooldownActiveException.class)
    public ResponseEntity<ErrorResponse> handleCooldownActive(CooldownActiveException ex) {
        logError("CooldownActiveException", ex);
        ErrorResponse response = new ErrorResponse(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                ErrorCode.COOLDOWN_ACTIVE,
                ex.getMessage(),
                Collections.emptyList(),
                resolveId(ex)
        );
        return new ResponseEntity<>(response, HttpStatus.TOO_MANY_REQUESTS);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        logError("ResourceNotFoundException", ex);
        ErrorResponse response = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                ErrorCode.RESOURCE_NOT_FOUND,
                ex.getMessage(),
                Collections.emptyList(),
                resolveId(ex)
        );
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(QueueFullException.class)
    public ResponseEntity<ErrorResponse> handleQueueFull(QueueFullException ex) {
        logError("QueueFullException", ex);
        ErrorResponse response = new ErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                ErrorCode.QUEUE_FULL,
                ex.getMessage(),
                Collections.emptyList(),
                resolveId(ex)
        );
        return new ResponseEntity<>(response, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler({
            IllegalStateException.class, 
            IllegalArgumentException.class, 
            org.springframework.http.converter.HttpMessageNotReadableException.class,
            org.springframework.web.bind.MissingServletRequestParameterException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception ex) {
        logError(ex.getClass().getSimpleName(), ex);
        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                ErrorCode.BAD_REQUEST,
                ex.getMessage() != null ? ex.getMessage() : "Bad Request provided",
                Collections.emptyList(),
                resolveId(ex) 
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<ValidationError> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ValidationError(
                        error.getField(), 
                        error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value"
                ))
                .collect(Collectors.toList());

        logger.error("Payload Validation Exception [{}] - {}", fetchMdcId(), errors);

        ErrorResponse response = new ErrorResponse(
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                ErrorCode.VALIDATION_FAILED,
                "Request payload validation failed",
                errors,
                resolveId(ex)
        );
        return new ResponseEntity<>(response, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(org.springframework.web.HttpRequestMethodNotSupportedException ex) {
        ErrorResponse response = new ErrorResponse(
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                ErrorCode.METHOD_NOT_ALLOWED,
                "HTTP Method " + ex.getMethod() + " is not supported here.",
                Collections.emptyList(),
                resolveId(ex)
        );
        return new ResponseEntity<>(response, HttpStatus.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(org.springframework.web.context.request.async.AsyncRequestTimeoutException.class)
    public ResponseEntity<ErrorResponse> handleAsyncTimeout(Exception ex) {
        logError("AsyncRequestTimeoutException", ex);
        ErrorResponse response = new ErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                ErrorCode.INTERNAL_SERVER_ERROR,
                "The server took too long to generate your PDF. Please try again.",
                Collections.emptyList(),
                resolveId(ex)
        );
        return new ResponseEntity<>(response, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(
            org.springframework.web.servlet.resource.NoResourceFoundException ex) {
        logger.debug("No resource found: {}", ex.getMessage());
        ErrorResponse response = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                ErrorCode.RESOURCE_NOT_FOUND,
                ex.getMessage(),
                Collections.emptyList(),
                resolveId(ex)
        );
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(org.springframework.security.core.AuthenticationException ex) {
        logger.warn("Authentication failed: {}", ex.getMessage());
        ErrorResponse response = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                ErrorCode.UNAUTHORIZED,
                "Invalid email or password",
                Collections.emptyList(),
                resolveId(ex)
        );
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(org.springframework.mail.MailException.class)
    public ResponseEntity<ErrorResponse> handleMailException(org.springframework.mail.MailException ex) {
        logger.error("Mail dispatch failed: ", ex);
        ErrorResponse response = new ErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                ErrorCode.MAIL_ERROR,
                "SMTP Server configuration is missing or incorrect. Email features are currently unavailable.",
                Collections.emptyList(),
                resolveId(ex)
        );
        return new ResponseEntity<>(response, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(org.springframework.dao.DataIntegrityViolationException ex) {
        logError("DataIntegrityViolationException", ex);
        String detailMessage = "Database constraint violation occurred.";
        if (ex.getMessage() != null && ex.getMessage().contains("uq_student_session")) {
            detailMessage = "Duplicate student detected in the same exam session.";
        }
        ErrorResponse response = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                ErrorCode.DUPLICATE_FILE,
                detailMessage,
                Collections.singletonList(ex.getMessage()),
                resolveId(ex)
        );
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handle(Exception ex) {
        logger.error("ERROR:", ex);
        ErrorResponse response = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ErrorCode.INTERNAL_SERVER_ERROR,
                "Something went wrong",
                Collections.emptyList(),
                resolveId(ex)
        );
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private void logError(String type, Exception ex) {
        UUID reqId = resolveId(ex);
        String traceIdStr = reqId != null ? " [RequestID: " + reqId + "]" : "";
        logger.error("{} caught{}: {}", type, traceIdStr, ex.getMessage());
    }

    private UUID resolveId(Exception ex) {
        UUID explicitId = extractId(ex);
        if (explicitId != null) {
            return explicitId;
        }
        String mdcId = fetchMdcId();
        if (mdcId != null) {
            try {
                return UUID.fromString(mdcId);
            } catch (Exception ignored) {}
        }
        return null;
    }

    private String fetchMdcId() {
        return MDC.get("allocationRequestId");
    }

    private UUID extractId(Exception ex) {
        if (ex instanceof DuplicateFileException) return ((DuplicateFileException) ex).getAllocationRequestId();
        if (ex instanceof PreflightValidationException) return ((PreflightValidationException) ex).getAllocationRequestId();
        if (ex instanceof AllocationInProgressException) return ((AllocationInProgressException) ex).getAllocationRequestId();
        if (ex instanceof CooldownActiveException) return ((CooldownActiveException) ex).getAllocationRequestId();
        if (ex instanceof ResourceNotFoundException) return ((ResourceNotFoundException) ex).getAllocationRequestId();
        return null;
    }
}

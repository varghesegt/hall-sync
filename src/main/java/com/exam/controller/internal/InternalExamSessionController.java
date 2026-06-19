package com.exam.controller.internal;

import com.exam.dto.SessionCreateRequest;
import com.exam.dto.SessionCreateResponse;
import com.exam.service.internal.InternalExamSessionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api/v1/internal/exam-sessions")
public class InternalExamSessionController {

    private final InternalExamSessionService sessionService;

    public InternalExamSessionController(InternalExamSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping(consumes = "application/json", produces = "application/json")
    public ResponseEntity<SessionCreateResponse> createSession(@Valid @RequestBody SessionCreateRequest request) {
        SessionCreateResponse response = sessionService.createSession(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}

package com.exam.controller;

import com.exam.dto.SessionCreateRequest;
import com.exam.dto.SessionCreateResponse;
import com.exam.service.ExamSessionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

@RestController
@Validated
@RequestMapping("/api/v1/exam-sessions")
public class ExamSessionController {

    private final ExamSessionService examSessionService;

    public ExamSessionController(ExamSessionService examSessionService) {
        this.examSessionService = examSessionService;
    }

    @PostMapping(consumes = "application/json", produces = "application/json")
    public ResponseEntity<SessionCreateResponse> createSession(@Valid @RequestBody SessionCreateRequest request) {
        SessionCreateResponse response = examSessionService.createSession(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}

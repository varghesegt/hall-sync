package com.exam.service;

import com.exam.dto.SessionCreateRequest;
import com.exam.dto.SessionCreateResponse;

public interface ExamSessionService {
    SessionCreateResponse createSession(SessionCreateRequest request);
}

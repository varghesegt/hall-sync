package com.exam.dto;

import com.exam.parser.model.ParseError;

import java.util.List;
import java.util.UUID;

/**
 * Response returned after successful session creation.
 * Includes warnings if parse had non-fatal issues.
 */
public record SessionCreateResponse(
        UUID examSessionId,
        String status,
        int totalStudents,
        boolean hasUnresolvedDepartments,
        List<ParseError> warnings
) {}

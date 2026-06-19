package com.exam.parser.model;

import java.util.List;

public record ParseResult(
        ParseStatus      status,
        List<StudentRow>  students,
        List<ParseError>  errors,
        String           sourceHash,
        int              totalPages,
        int              totalLinesScanned
) {}

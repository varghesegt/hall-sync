package com.exam.parser.model;

public record ParseError(
        int           pageNumber,
        int           lineNumber,
        ParseErrorCode errorCode,
        String        severity,
        String        message,
        String        rawContent
) {}

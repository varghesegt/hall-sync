package com.exam.dto;

import com.exam.exception.ErrorCode;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

public class ErrorResponse {
    private final String timestamp;
    private final int status;
    private final ErrorCode error;
    private final String message;
    private final List<?> details;
    private final UUID allocationRequestId;

    public ErrorResponse(int status, ErrorCode error, String message, List<?> details, UUID allocationRequestId) {
        this.timestamp = OffsetDateTime.now(ZoneOffset.UTC).toString();
        this.status = status;
        this.error = error;
        this.message = message;
        this.details = details;
        this.allocationRequestId = allocationRequestId;
    }

    public String getTimestamp() { return timestamp; }
    public int getStatus() { return status; }
    public ErrorCode getError() { return error; }
    public String getMessage() { return message; }
    public List<?> getDetails() { return details; }
    public UUID getAllocationRequestId() { return allocationRequestId; }
}

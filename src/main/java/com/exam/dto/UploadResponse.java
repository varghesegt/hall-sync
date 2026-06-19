package com.exam.dto;

import java.util.UUID;

/**
 * Upload response DTO. Includes idempotent duplicate handling.
 */
public record UploadResponse(
        UUID fileId,
        String fileName,
        String hash,
        String status
) {
    /**
     * Factory for idempotent duplicate response.
     * Returns the existing fileId so clients can reuse without re-uploading.
     */
    public static UploadResponse duplicate(UUID existingFileId, String fileName, String hash) {
        return new UploadResponse(existingFileId, fileName, hash, "DUPLICATE");
    }
}

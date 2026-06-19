package com.exam.service;

import com.exam.dto.UploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UploadService {
    UploadResponse uploadFile(MultipartFile file);
    com.exam.dto.StudentPreviewResponse getPreview(java.util.UUID fileId);
}

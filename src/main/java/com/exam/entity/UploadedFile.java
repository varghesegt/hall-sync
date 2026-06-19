package com.exam.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "uploaded_files")
public class UploadedFile {

    @Id
    private UUID id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(nullable = false, length = 50)
    private String status;

    @Column(name = "sha256_hash", nullable = false, unique = true, length = 64)
    private String sha256Hash;

    @Column(name = "exam_type", nullable = false, length = 20)
    private String examType = "SEMESTER";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected UploadedFile() {} // JPA requirement

    public UploadedFile(UUID id, String fileName, String status, String sha256Hash) {
        this.id = id;
        this.fileName = fileName;
        this.status = status;
        this.sha256Hash = sha256Hash;
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public String getFileName() { return fileName; }
    public String getStatus() { return status; }
    public String getSha256Hash() { return sha256Hash; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setStatus(String status) { this.status = status; }
    public String getExamType() { return examType; }
    public void setExamType(String examType) { this.examType = examType; }
}

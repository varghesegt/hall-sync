package com.exam.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "allocation_batches")
public class AllocationBatch {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_session_id", nullable = false)
    private ExamSession examSession;

    @Column(name = "allocation_request_id", nullable = false)
    private UUID allocationRequestId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private BatchStatus status;

    @Column(nullable = false)
    private Integer version;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected AllocationBatch() {}

    public AllocationBatch(UUID id, ExamSession examSession, UUID allocationRequestId, BatchStatus status, Integer version) {
        this.id = id;
        this.examSession = examSession;
        this.allocationRequestId = allocationRequestId;
        this.status = status;
        this.version = version;
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public ExamSession getExamSession() { return examSession; }
    public UUID getAllocationRequestId() { return allocationRequestId; }
    public BatchStatus getStatus() { return status; }
    public Integer getVersion() { return version; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setStatus(BatchStatus status) { this.status = status; }
}

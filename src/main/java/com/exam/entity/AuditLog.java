package com.exam.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_session_id", nullable = false)
    private ExamSession examSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private AllocationBatch batch;

    @Column(name = "allocation_request_id", nullable = false)
    private UUID allocationRequestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 100)
    private AuditEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AuditSeverity severity;

    @Column(nullable = false, length = 4000)
    private String message;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    private String metadata;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected AuditLog() {}

    public AuditLog(UUID id, ExamSession examSession, AllocationBatch batch, UUID allocationRequestId, AuditEventType eventType, AuditSeverity severity, String message, String metadata) {
        this.id = id;
        this.examSession = examSession;
        this.batch = batch;
        this.allocationRequestId = allocationRequestId;
        this.eventType = eventType;
        this.severity = severity;
        this.message = message;
        this.metadata = metadata;
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public ExamSession getExamSession() { return examSession; }
    public AllocationBatch getBatch() { return batch; }
    public UUID getAllocationRequestId() { return allocationRequestId; }
    public AuditEventType getEventType() { return eventType; }
    public AuditSeverity getSeverity() { return severity; }
    public String getMessage() { return message; }
    public String getMetadata() { return metadata; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

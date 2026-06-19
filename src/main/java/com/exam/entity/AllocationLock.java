package com.exam.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "allocation_locks")
public class AllocationLock {

    // Using exam_session_id as both Primary Key and Foreign Key
    @Id
    @Column(name = "exam_session_id")
    private UUID examSessionId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "exam_session_id")
    private ExamSession examSession;

    @Column(name = "locked_at", nullable = false)
    private LocalDateTime lockedAt;

    @Column(name = "locked_by", nullable = false, length = 255)
    private String lockedBy;

    protected AllocationLock() {}

    public AllocationLock(ExamSession examSession, String lockedBy) {
        this.examSession = examSession;
        this.lockedBy = lockedBy;
        this.lockedAt = LocalDateTime.now();
    }

    public UUID getExamSessionId() { return examSessionId; }
    public ExamSession getExamSession() { return examSession; }
    public LocalDateTime getLockedAt() { return lockedAt; }
    public String getLockedBy() { return lockedBy; }
}

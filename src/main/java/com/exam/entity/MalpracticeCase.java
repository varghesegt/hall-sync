package com.exam.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "malpractice_cases")
public class MalpracticeCase {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private AllocationBatch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hall_id")
    private Hall hall;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by")
    private Faculty reportedBy;

    @Column(name = "case_type", nullable = false, length = 50)
    private String caseType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "evidence_data")
    private byte[] evidenceData;

    @Column(name = "evidence_filename", length = 255)
    private String evidenceFilename;

    @Column(name = "evidence_content_type", length = 100)
    private String evidenceContentType;

    @Column(nullable = false, length = 20)
    private String severity = "MODERATE";

    @Column(name = "action_taken", length = 500)
    private String actionTaken;

    @Column(nullable = false, length = 30)
    private String status = "REPORTED";

    @Column(name = "reported_at", nullable = false)
    private LocalDateTime reportedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    protected MalpracticeCase() {}

    public MalpracticeCase(UUID id, AllocationBatch batch, Student student, Hall hall,
                           Faculty reportedBy, String caseType, String description,
                           String severity) {
        this.id = id;
        this.batch = batch;
        this.student = student;
        this.hall = hall;
        this.reportedBy = reportedBy;
        this.caseType = caseType;
        this.description = description;
        this.severity = severity != null ? severity : "MODERATE";
        this.status = "REPORTED";
        this.reportedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public AllocationBatch getBatch() { return batch; }
    public Student getStudent() { return student; }
    public Hall getHall() { return hall; }
    public Faculty getReportedBy() { return reportedBy; }
    public String getCaseType() { return caseType; }
    public String getDescription() { return description; }
    public byte[] getEvidenceData() { return evidenceData; }
    public String getEvidenceFilename() { return evidenceFilename; }
    public String getEvidenceContentType() { return evidenceContentType; }
    public String getSeverity() { return severity; }
    public String getActionTaken() { return actionTaken; }
    public String getStatus() { return status; }
    public LocalDateTime getReportedAt() { return reportedAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }

    public void setStatus(String status) { this.status = status; }
    public void setActionTaken(String actionTaken) { this.actionTaken = actionTaken; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
    public void setEvidenceData(byte[] evidenceData) { this.evidenceData = evidenceData; }
    public void setEvidenceFilename(String evidenceFilename) { this.evidenceFilename = evidenceFilename; }
    public void setEvidenceContentType(String evidenceContentType) { this.evidenceContentType = evidenceContentType; }
    public void setDescription(String description) { this.description = description; }
    public void setSeverity(String severity) { this.severity = severity; }
}

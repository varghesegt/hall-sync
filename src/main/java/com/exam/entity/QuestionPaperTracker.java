package com.exam.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "question_paper_trackers")
public class QuestionPaperTracker {

    @Id
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_session_id", nullable = false)
    private ExamSession examSession;

    @Column(name = "subject_code", nullable = false)
    private String subjectCode;

    @Column(name = "total_received", nullable = false)
    private Integer totalReceived;

    @Column(name = "stored_location")
    private String storedLocation;

    @Column(name = "distributed_time")
    private LocalDateTime distributedTime;

    @Column(name = "returned_time")
    private LocalDateTime returnedTime;

    // RECEIVED, DISTRIBUTED, RETURNED, COMPROMISED
    @Column(name = "status", nullable = false)
    private String status = "RECEIVED";

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public QuestionPaperTracker() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public ExamSession getExamSession() { return examSession; }
    public void setExamSession(ExamSession examSession) { this.examSession = examSession; }
    
    public String getSubjectCode() { return subjectCode; }
    public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }
    
    public Integer getTotalReceived() { return totalReceived; }
    public void setTotalReceived(Integer totalReceived) { this.totalReceived = totalReceived; }
    
    public String getStoredLocation() { return storedLocation; }
    public void setStoredLocation(String storedLocation) { this.storedLocation = storedLocation; }
    
    public LocalDateTime getDistributedTime() { return distributedTime; }
    public void setDistributedTime(LocalDateTime distributedTime) { this.distributedTime = distributedTime; }
    
    public LocalDateTime getReturnedTime() { return returnedTime; }
    public void setReturnedTime(LocalDateTime returnedTime) { this.returnedTime = returnedTime; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

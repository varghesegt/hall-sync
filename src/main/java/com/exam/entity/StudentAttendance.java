package com.exam.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "student_attendance", uniqueConstraints = {
    @UniqueConstraint(name = "uq_attendance_batch_student", columnNames = {"batch_id", "student_id"})
})
public class StudentAttendance {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private AllocationBatch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hall_id", nullable = false)
    private Hall hall;

    @Column(name = "is_present")
    private Boolean isPresent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marked_by")
    private Faculty markedBy;

    @Column(name = "marked_at")
    private LocalDateTime markedAt;

    protected StudentAttendance() {}

    public StudentAttendance(UUID id, AllocationBatch batch, Student student, Hall hall) {
        this.id = id;
        this.batch = batch;
        this.student = student;
        this.hall = hall;
    }

    public UUID getId() { return id; }
    public AllocationBatch getBatch() { return batch; }
    public Student getStudent() { return student; }
    public Hall getHall() { return hall; }
    public Boolean getIsPresent() { return isPresent; }
    public Faculty getMarkedBy() { return markedBy; }
    public LocalDateTime getMarkedAt() { return markedAt; }

    public void setIsPresent(Boolean isPresent) { this.isPresent = isPresent; }
    public void setMarkedBy(Faculty markedBy) { this.markedBy = markedBy; }
    public void setMarkedAt(LocalDateTime markedAt) { this.markedAt = markedAt; }
}

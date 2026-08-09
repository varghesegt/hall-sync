package com.exam.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "invigilator_duties", uniqueConstraints = {
    @UniqueConstraint(name = "uq_duty_batch_faculty_hall", columnNames = {"batch_id", "faculty_id", "hall_id"})
})
public class InvigilatorDuty {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private AllocationBatch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faculty_id", nullable = false)
    private Faculty faculty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hall_id", nullable = false)
    private Hall hall;

    @Column(name = "duty_type", nullable = false, length = 30)
    private String dutyType = "INVIGILATOR";

    @Column(nullable = false, length = 10)
    private String shift;

    @Column(name = "duty_date", nullable = false)
    private LocalDate dutyDate;

    @Column(name = "is_present")
    private Boolean isPresent;

    @Column(name = "marked_at")
    private LocalDateTime markedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected InvigilatorDuty() {}

    public InvigilatorDuty(UUID id, AllocationBatch batch, Faculty faculty, Hall hall,
                           String dutyType, String shift, LocalDate dutyDate) {
        this.id = id;
        this.batch = batch;
        this.faculty = faculty;
        this.hall = hall;
        this.dutyType = dutyType;
        this.shift = shift;
        this.dutyDate = dutyDate;
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public AllocationBatch getBatch() { return batch; }
    public Faculty getFaculty() { return faculty; }
    public Hall getHall() { return hall; }
    public String getDutyType() { return dutyType; }
    public String getShift() { return shift; }
    public LocalDate getDutyDate() { return dutyDate; }
    public Boolean getIsPresent() { return isPresent; }
    public LocalDateTime getMarkedAt() { return markedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setIsPresent(Boolean isPresent) { this.isPresent = isPresent; }
    public void setMarkedAt(LocalDateTime markedAt) { this.markedAt = markedAt; }
    public void setDutyType(String dutyType) { this.dutyType = dutyType; }
    public void setFaculty(Faculty faculty) { this.faculty = faculty; }
}

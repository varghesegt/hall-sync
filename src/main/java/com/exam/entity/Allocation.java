package com.exam.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "allocations", uniqueConstraints = {
    @UniqueConstraint(name = "uq_hall_seat_batch", columnNames = {"hall_id", "seat_row", "seat_col", "batch_id"}),
    @UniqueConstraint(name = "uq_student_per_batch", columnNames = {"student_id", "batch_id"})
})
public class Allocation {

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

    @Column(name = "seat_row", nullable = false)
    private Integer seatRow;

    @Column(name = "seat_col", nullable = false, length = 5)
    private String seatCol;

    @Column(name = "risk_score")
    private Integer riskScore;

    protected Allocation() {}

    public Allocation(UUID id, AllocationBatch batch, Student student, Hall hall, Integer seatRow, String seatCol, Integer riskScore) {
        this.id = id;
        this.batch = batch;
        this.student = student;
        this.hall = hall;
        this.seatRow = seatRow;
        this.seatCol = seatCol;
        this.riskScore = riskScore;
    }

    public UUID getId() { return id; }
    public AllocationBatch getBatch() { return batch; }
    public Student getStudent() { return student; }
    public Hall getHall() { return hall; }
    public Integer getSeatRow() { return seatRow; }
    public String getSeatCol() { return seatCol; }
    public Integer getRiskScore() { return riskScore; }

    // Setters for visual override (seat swap / move)
    public void setSeatRow(Integer seatRow) { this.seatRow = seatRow; }
    public void setSeatCol(String seatCol) { this.seatCol = seatCol; }
    public void setHall(Hall hall) { this.hall = hall; }
    public void setStudent(Student student) { this.student = student; }
    public void setRiskScore(Integer riskScore) { this.riskScore = riskScore; }
}

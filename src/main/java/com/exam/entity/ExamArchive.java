package com.exam.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "exam_archives")
public class ExamArchive {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_session_id", nullable = false)
    private ExamSession examSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private AllocationBatch batch;

    @Column(name = "exam_type", nullable = false, length = 20)
    private String examType;

    @Column(name = "exam_date", nullable = false)
    private LocalDate examDate;

    @Column(nullable = false, length = 10)
    private String session;

    @Column(name = "total_students", nullable = false)
    private Integer totalStudents;

    @Column(name = "total_halls", nullable = false)
    private Integer totalHalls;

    @Column(name = "total_invigilators", nullable = false)
    private Integer totalInvigilators = 0;

    @Column(name = "total_absentees", nullable = false)
    private Integer totalAbsentees = 0;

    @Column(name = "total_malpractice", nullable = false)
    private Integer totalMalpractice = 0;

    @Column(name = "seating_plan_snapshot")
    private byte[] seatingPlanSnapshot;

    @Column(name = "attendance_snapshot")
    private byte[] attendanceSnapshot;

    @Column(name = "duty_sheet_snapshot")
    private byte[] dutySheetSnapshot;

    @Column(name = "archived_at", nullable = false)
    private LocalDateTime archivedAt;

    @Column(name = "archived_by", length = 150)
    private String archivedBy;

    protected ExamArchive() {}

    public ExamArchive(UUID id, ExamSession examSession, AllocationBatch batch,
                       String examType, LocalDate examDate, String session,
                       int totalStudents, int totalHalls, String archivedBy) {
        this.id = id;
        this.examSession = examSession;
        this.batch = batch;
        this.examType = examType;
        this.examDate = examDate;
        this.session = session;
        this.totalStudents = totalStudents;
        this.totalHalls = totalHalls;
        this.archivedBy = archivedBy;
        this.archivedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public ExamSession getExamSession() { return examSession; }
    public AllocationBatch getBatch() { return batch; }
    public String getExamType() { return examType; }
    public LocalDate getExamDate() { return examDate; }
    public String getSession() { return session; }
    public Integer getTotalStudents() { return totalStudents; }
    public Integer getTotalHalls() { return totalHalls; }
    public Integer getTotalInvigilators() { return totalInvigilators; }
    public Integer getTotalAbsentees() { return totalAbsentees; }
    public Integer getTotalMalpractice() { return totalMalpractice; }
    public byte[] getSeatingPlanSnapshot() { return seatingPlanSnapshot; }
    public byte[] getAttendanceSnapshot() { return attendanceSnapshot; }
    public byte[] getDutySheetSnapshot() { return dutySheetSnapshot; }
    public LocalDateTime getArchivedAt() { return archivedAt; }
    public String getArchivedBy() { return archivedBy; }

    public void setTotalInvigilators(Integer totalInvigilators) { this.totalInvigilators = totalInvigilators; }
    public void setTotalAbsentees(Integer totalAbsentees) { this.totalAbsentees = totalAbsentees; }
    public void setTotalMalpractice(Integer totalMalpractice) { this.totalMalpractice = totalMalpractice; }
    public void setSeatingPlanSnapshot(byte[] seatingPlanSnapshot) { this.seatingPlanSnapshot = seatingPlanSnapshot; }
    public void setAttendanceSnapshot(byte[] attendanceSnapshot) { this.attendanceSnapshot = attendanceSnapshot; }
    public void setDutySheetSnapshot(byte[] dutySheetSnapshot) { this.dutySheetSnapshot = dutySheetSnapshot; }
}

package com.exam.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "exam_sessions")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ExamSession {

    @Id
    private UUID id;

    @Column(name = "season_id", nullable = false, length = 100)
    private String seasonId;

    @Column(nullable = false)
    private String name;

    @Column(name = "exam_date", nullable = false)
    private LocalDate examDate;

    @Column(nullable = false, length = 10)
    private String session;

    @Column(name = "exam_type", nullable = false, length = 20)
    private String examType = "SEMESTER";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected ExamSession() {}

    public ExamSession(UUID id, String seasonId, String name, LocalDate examDate, String session) {
        this.id = id;
        this.seasonId = seasonId;
        this.name = name;
        this.examDate = examDate;
        this.session = session;
        this.examType = "SEMESTER";
        this.createdAt = LocalDateTime.now();
    }

    public ExamSession(UUID id, String seasonId, String name, LocalDate examDate, String session, String examType) {
        this.id = id;
        this.seasonId = seasonId;
        this.name = name;
        this.examDate = examDate;
        this.session = session;
        this.examType = examType != null ? examType : "SEMESTER";
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public String getSeasonId() { return seasonId; }
    public String getName() { return name; }
    public LocalDate getExamDate() { return examDate; }
    public String getSession() { return session; }
    public String getExamType() { return examType; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

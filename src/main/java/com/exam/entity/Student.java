package com.exam.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "students", uniqueConstraints = {
    @UniqueConstraint(name = "uq_student_session", columnNames = {"register_number", "exam_session_id"})
})
public class Student {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_session_id", nullable = false)
    private ExamSession examSession;

    @Column(name = "register_number", nullable = false, length = 100)
    private String registerNumber;

    @Column(nullable = true, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String department;

    @Column(name = "class_name", length = 100)
    private String className;

    @Column(name = "subject_name", length = 255)
    private String subjectName;

    @Column(name = "subject_code", length = 100)
    private String subjectCode;

    @Column(length = 50)
    private String semester;

    @Column(length = 50)
    private String regulation;

    protected Student() {}

    public Student(UUID id, ExamSession examSession, String registerNumber, String name, String department, 
                   String className, String subjectName, String subjectCode, String semester, String regulation) {
        this.id = id;
        this.examSession = examSession;
        this.registerNumber = registerNumber;
        this.name = name;
        this.department = department;
        this.className = className;
        this.subjectName = cleanSubjectName(subjectName);
        this.subjectCode = subjectCode;
        this.semester = semester;
        this.regulation = regulation;
    }

    private String cleanSubjectName(String name) {
        if (name == null) return null;
        // Case-insensitive regex to handle variations in spacing and parens
        return name.replaceAll("(?i)\\s*\\(\\s*CUM\\s+PRACTICAL\\s*\\)\\s*", " ").trim();
    }

    public UUID getId() { return id; }
    public ExamSession getExamSession() { return examSession; }
    public String getRegisterNumber() { return registerNumber; }
    public String getName() { return name; }
    public String getDepartment() { return department; }
    public String getClassName() { return className; }
    public String getSubjectName() { return subjectName; }
    public String getSubjectCode() { return subjectCode; }
    public String getSemester() { return semester; }
    public String getRegulation() { return regulation; }

    // Setter for visual override (register number edit)
    public void setRegisterNumber(String registerNumber) { this.registerNumber = registerNumber; }
}

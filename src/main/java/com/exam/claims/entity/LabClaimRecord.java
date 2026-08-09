package com.exam.claims.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "lab_claim_records")
public class LabClaimRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id")
    private UUID batchId;

    @Column(name = "exam_date")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate examDate;

    @Column(name = "exam_season")
    private String examSeason;

    @Column(name = "session")
    private String session; // FN / AN

    @Column(name = "department")
    private String department;

    @Column(name = "semester")
    private String semester;

    @Column(name = "subject_code")
    private String subjectCode;

    @Column(name = "subject_name")
    private String subjectName;

    @Column(name = "batch_number")
    private String batchNumber;

    @Column(name = "registered_count")
    private Integer registeredCount = 0;

    @Column(name = "present_count")
    private Integer presentCount = 0;

    @NotBlank(message = "Staff name is required")
    @Column(name = "staff_name", nullable = false)
    private String staffName;

    @Column(name = "name_title")
    private String nameTitle;

    @NotBlank(message = "Staff role is required")
    @Column(name = "staff_role", nullable = false)
    private String staffRole; // INTERNAL_EXAMINER, EXTERNAL_EXAMINER, SKILLED_ASSISTANT, LAB_ATTENDER

    @Column(name = "designation")
    private String designation;

    @Column(name = "institution_name")
    private String institutionName;

    @Column(name = "institution_code")
    private String institutionCode;

    @Column(name = "mobile_no")
    private String mobileNo;

    @Column(name = "bank_account_number")
    private String bankAccountNumber;

    @Column(name = "ifsc_code")
    private String ifscCode;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "branch")
    private String branch;

    // ========== Calculated Amounts ==========

    @Column(name = "distance_km", precision = 10, scale = 2)
    private BigDecimal distanceKm = BigDecimal.ZERO;

    @Column(name = "remuneration_amount", precision = 12, scale = 2)
    private BigDecimal remunerationAmount = BigDecimal.ZERO;

    @Column(name = "ta_amount", precision = 12, scale = 2)
    private BigDecimal taAmount = BigDecimal.ZERO;

    @Column(name = "da_amount", precision = 12, scale = 2)
    private BigDecimal daAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "status")
    private String status = "SUBMITTED"; // SUBMITTED, APPROVED, PAID

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public LabClaimRecord() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UUID getBatchId() { return batchId; }
    public void setBatchId(UUID batchId) { this.batchId = batchId; }

    public LocalDate getExamDate() { return examDate; }
    public void setExamDate(LocalDate examDate) { this.examDate = examDate; }

    public String getExamSeason() { return examSeason; }
    public void setExamSeason(String examSeason) { this.examSeason = examSeason; }

    public String getSession() { return session; }
    public void setSession(String session) { this.session = session; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }

    public String getSubjectCode() { return subjectCode; }
    public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }

    public Integer getRegisteredCount() { return registeredCount; }
    public void setRegisteredCount(Integer registeredCount) { this.registeredCount = registeredCount; }

    public Integer getPresentCount() { return presentCount; }
    public void setPresentCount(Integer presentCount) { this.presentCount = presentCount; }

    public String getStaffName() { return staffName; }
    public void setStaffName(String staffName) { this.staffName = staffName; }

    public String getNameTitle() { return nameTitle; }
    public void setNameTitle(String nameTitle) { this.nameTitle = nameTitle; }

    public String getStaffRole() { return staffRole; }
    public void setStaffRole(String staffRole) { this.staffRole = staffRole; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getInstitutionName() { return institutionName; }
    public void setInstitutionName(String institutionName) { this.institutionName = institutionName; }

    public String getInstitutionCode() { return institutionCode; }
    public void setInstitutionCode(String institutionCode) { this.institutionCode = institutionCode; }

    public String getMobileNo() { return mobileNo; }
    public void setMobileNo(String mobileNo) { this.mobileNo = mobileNo; }

    public String getBankAccountNumber() { return bankAccountNumber; }
    public void setBankAccountNumber(String bankAccountNumber) { this.bankAccountNumber = bankAccountNumber; }

    public String getIfscCode() { return ifscCode; }
    public void setIfscCode(String ifscCode) { this.ifscCode = ifscCode; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public BigDecimal getDistanceKm() { return distanceKm; }
    public void setDistanceKm(BigDecimal distanceKm) { this.distanceKm = distanceKm; }

    public BigDecimal getRemunerationAmount() { return remunerationAmount; }
    public void setRemunerationAmount(BigDecimal remunerationAmount) { this.remunerationAmount = remunerationAmount; }

    public BigDecimal getTaAmount() { return taAmount; }
    public void setTaAmount(BigDecimal taAmount) { this.taAmount = taAmount; }

    public BigDecimal getDaAmount() { return daAmount; }
    public void setDaAmount(BigDecimal daAmount) { this.daAmount = daAmount; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

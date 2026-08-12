package com.exam.claims.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "claim_records")
public class ClaimRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id", nullable = false)
    private UUID batchId;

    @Column(name = "serial_number")
    private Integer serialNumber;

    @Column(name = "exam_month")
    private String examMonth;

    @Column(name = "valuation_date")
    private LocalDate valuationDate;

    @Column(name = "exam_season")
    private String examSeason;

    @Column(name = "is_government_holiday")
    private Boolean governmentHoliday = false;

    @Column(name = "is_revaluation")
    private Boolean revaluation = false;

    @Column(name = "sessions_attended")
    private String sessionsAttended;

    @Column(name = "board_name")
    private String boardName;

    @NotBlank(message = "Post held is required")
    @Column(name = "post_held", nullable = false)
    private String postHeld;

    @Column(name = "mobile_no")
    private String mobileNo;

    @Column(name = "name_title")
    private String nameTitle;

    @NotBlank(message = "Staff name is required")
    @Column(name = "staff_name", nullable = false)
    private String staffName;

    @Column(name = "faculty_type")
    private String facultyType;

    @Column(name = "designation")
    private String designation;

    @Column(name = "institution_name")
    private String institutionName;

    @Column(name = "institution_code")
    private String institutionCode;

    @Column(name = "issue_reg_page_no")
    private String issueRegPageNo;

    @Column(name = "bank_account_number")
    private String bankAccountNumber;

    @Column(name = "ifsc_code")
    private String ifscCode;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "branch")
    private String branch;

    // ========== Calculated Fields ==========

    @Column(name = "distance_km", precision = 10, scale = 2)
    private BigDecimal distanceKm;

    @Column(name = "total_scripts")
    private Integer totalScripts = 0;

    @Column(name = "fn_scripts")
    private Integer fnScripts = 0;

    @Column(name = "an_scripts")
    private Integer anScripts = 0;

    @Column(name = "script_amount", precision = 12, scale = 2)
    private BigDecimal scriptAmount = BigDecimal.ZERO;

    @Column(name = "travelling_allowance", precision = 12, scale = 2)
    private BigDecimal travellingAllowance = BigDecimal.ZERO;

    @Column(name = "dearness_allowance", precision = 12, scale = 2)
    private BigDecimal dearnessAllowance = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "amount_in_words", length = 500)
    private String amountInWords;

    // ========== Chief Examiner Specific ==========

    @Column(name = "max_scripts_valued")
    private Integer maxScriptsValued;

    @Column(name = "max_scripts_amount", precision = 12, scale = 2)
    private BigDecimal maxScriptsAmount;

    @Column(name = "ten_percent_amount", precision = 12, scale = 2)
    private BigDecimal tenPercentAmount;

    @Column(name = "overall_script_amount", precision = 12, scale = 2)
    private BigDecimal overallScriptAmount;

    // ========== Script Details (FN/AN breakdown) ==========

    @OneToMany(mappedBy = "claimRecord", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("sessionType ASC, serialNumber ASC")
    private List<ScriptDetail> scriptDetails = new ArrayList<>();

    // ========== Timestamps ==========

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ========== Helper Methods ==========

    public void addScriptDetail(ScriptDetail detail) {
        scriptDetails.add(detail);
        detail.setClaimRecord(this);
    }

    public String getFullName() {
        return (nameTitle != null ? nameTitle : "") + staffName;
    }

    public boolean isChiefExaminer() {
        return "CHIEF EXAMINER".equalsIgnoreCase(postHeld);
    }

    public boolean isAssistantExaminer() {
        return "ASSISTANT EXAMINER".equalsIgnoreCase(postHeld);
    }

    public boolean isExaminer() {
        return "EXAMINER".equalsIgnoreCase(postHeld);
    }

    public boolean isInternal() {
        return facultyType != null && facultyType.toUpperCase().contains("INT");
    }

    public boolean hasBothSessions() {
        if (sessionsAttended == null) return false;
        String s = sessionsAttended.toUpperCase();
        return s.contains("FN") && s.contains("AN");
    }

    public int getSessionCount() {
        if (sessionsAttended == null) return 0;
        String s = sessionsAttended.toUpperCase();
        boolean hasFN = s.contains("FN");
        boolean hasAN = s.contains("AN");
        if (hasFN && hasAN) return 2;
        if (hasFN || hasAN) return 1;
        return 0;
    }

    // ========== Getters and Setters ==========

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UUID getBatchId() { return batchId; }
    public void setBatchId(UUID batchId) { this.batchId = batchId; }

    public Integer getSerialNumber() { return serialNumber; }
    public void setSerialNumber(Integer serialNumber) { this.serialNumber = serialNumber; }

    public String getExamMonth() { return examMonth; }
    public void setExamMonth(String examMonth) { this.examMonth = examMonth; }

    public LocalDate getValuationDate() { return valuationDate; }
    public void setValuationDate(LocalDate valuationDate) { this.valuationDate = valuationDate; }

    public String getExamSeason() { return examSeason; }
    public void setExamSeason(String examSeason) { this.examSeason = examSeason; }

    public Boolean isGovernmentHoliday() { return governmentHoliday != null && governmentHoliday; }
    public void setGovernmentHoliday(Boolean governmentHoliday) { this.governmentHoliday = governmentHoliday != null ? governmentHoliday : false; }

    public Boolean isRevaluation() { return revaluation != null && revaluation; }
    public void setRevaluation(Boolean revaluation) { this.revaluation = revaluation; }

    public String getSessionsAttended() { return sessionsAttended; }
    public void setSessionsAttended(String sessionsAttended) { this.sessionsAttended = sessionsAttended; }

    public String getBoardName() { return boardName; }
    public void setBoardName(String boardName) { this.boardName = boardName; }

    public String getPostHeld() { return postHeld; }
    public void setPostHeld(String postHeld) { this.postHeld = postHeld; }

    public String getMobileNo() { return mobileNo; }
    public void setMobileNo(String mobileNo) { this.mobileNo = mobileNo; }

    public String getNameTitle() { return nameTitle; }
    public void setNameTitle(String nameTitle) { this.nameTitle = nameTitle; }

    public String getStaffName() { return staffName; }
    public void setStaffName(String staffName) { this.staffName = staffName; }

    public String getFacultyType() { return facultyType; }
    public void setFacultyType(String facultyType) { this.facultyType = facultyType; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getInstitutionName() { return institutionName; }
    public void setInstitutionName(String institutionName) { this.institutionName = institutionName; }

    public String getInstitutionCode() { return institutionCode; }
    public void setInstitutionCode(String institutionCode) { this.institutionCode = institutionCode; }

    public String getIssueRegPageNo() { return issueRegPageNo; }
    public void setIssueRegPageNo(String issueRegPageNo) { this.issueRegPageNo = issueRegPageNo; }

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

    public Integer getTotalScripts() { return totalScripts; }
    public void setTotalScripts(Integer totalScripts) { this.totalScripts = totalScripts; }

    public Integer getFnScripts() { return fnScripts; }
    public void setFnScripts(Integer fnScripts) { this.fnScripts = fnScripts; }

    public Integer getAnScripts() { return anScripts; }
    public void setAnScripts(Integer anScripts) { this.anScripts = anScripts; }

    public BigDecimal getScriptAmount() { return scriptAmount; }
    public void setScriptAmount(BigDecimal scriptAmount) { this.scriptAmount = scriptAmount; }

    public BigDecimal getTravellingAllowance() { return travellingAllowance; }
    public void setTravellingAllowance(BigDecimal travellingAllowance) { this.travellingAllowance = travellingAllowance; }

    public BigDecimal getDearnessAllowance() { return dearnessAllowance; }
    public void setDearnessAllowance(BigDecimal dearnessAllowance) { this.dearnessAllowance = dearnessAllowance; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getAmountInWords() { return amountInWords; }
    public void setAmountInWords(String amountInWords) { this.amountInWords = amountInWords; }

    public Integer getMaxScriptsValued() { return maxScriptsValued; }
    public void setMaxScriptsValued(Integer maxScriptsValued) { this.maxScriptsValued = maxScriptsValued; }

    public BigDecimal getMaxScriptsAmount() { return maxScriptsAmount; }
    public void setMaxScriptsAmount(BigDecimal maxScriptsAmount) { this.maxScriptsAmount = maxScriptsAmount; }

    public BigDecimal getTenPercentAmount() { return tenPercentAmount; }
    public void setTenPercentAmount(BigDecimal tenPercentAmount) { this.tenPercentAmount = tenPercentAmount; }

    public BigDecimal getOverallScriptAmount() { return overallScriptAmount; }
    public void setOverallScriptAmount(BigDecimal overallScriptAmount) { this.overallScriptAmount = overallScriptAmount; }

    public List<ScriptDetail> getScriptDetails() { return scriptDetails; }
    public void setScriptDetails(List<ScriptDetail> scriptDetails) { this.scriptDetails = scriptDetails; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}

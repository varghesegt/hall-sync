package com.exam.claims.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class ClaimResponseDTO {

    private Long id;
    private UUID batchId;
    private Integer serialNumber;
    private String examMonth;
    private String examSeason;
    private String valuationDate;
    private boolean governmentHoliday;
    private String sessionsAttended;
    private String boardName;
    private String postHeld;
    private String mobileNo;
    private String nameTitle;
    private String staffName;
    private String facultyType;
    private String designation;
    private String institutionName;
    private String institutionCode;
    private String issueRegPageNo;
    private String bankAccountNumber;
    private String ifscCode;
    private String bankName;
    private String branch;

    // Calculated
    private BigDecimal distanceKm;
    private Integer totalScripts;
    private Integer fnScripts;
    private Integer anScripts;
    private BigDecimal scriptAmount;
    private BigDecimal travellingAllowance;
    private BigDecimal dearnessAllowance;
    private BigDecimal totalAmount;
    private String amountInWords;

    // Chief Examiner
    private Integer maxScriptsValued;
    private BigDecimal maxScriptsAmount;
    private BigDecimal tenPercentAmount;
    private BigDecimal overallScriptAmount;

    // Script Details
    private List<ScriptDetailDTO> scriptDetails;

    private String createdAt;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UUID getBatchId() { return batchId; }
    public void setBatchId(UUID batchId) { this.batchId = batchId; }

    public Integer getSerialNumber() { return serialNumber; }
    public void setSerialNumber(Integer serialNumber) { this.serialNumber = serialNumber; }

    public String getExamMonth() { return examMonth; }
    public void setExamMonth(String examMonth) { this.examMonth = examMonth; }

    public String getExamSeason() { return examSeason; }
    public void setExamSeason(String examSeason) { this.examSeason = examSeason; }

    public String getValuationDate() { return valuationDate; }
    public void setValuationDate(String valuationDate) { this.valuationDate = valuationDate; }

    public boolean isGovernmentHoliday() { return governmentHoliday; }
    public void setGovernmentHoliday(boolean governmentHoliday) { this.governmentHoliday = governmentHoliday; }

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

    public List<ScriptDetailDTO> getScriptDetails() { return scriptDetails; }
    public void setScriptDetails(List<ScriptDetailDTO> scriptDetails) { this.scriptDetails = scriptDetails; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}

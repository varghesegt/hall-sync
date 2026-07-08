package com.exam.claims.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;

public class ClaimSubmissionRequest {

    private String valuationDate;
    private String examSeason;

    @NotBlank(message = "Staff Name is required")
    private String staffName;

    @NotBlank(message = "Post Held is required")
    private String postHeld;

    @NotBlank(message = "Mobile No is required")
    @Pattern(regexp = "^\\d{10}$", message = "Mobile number must be 10 digits")
    private String mobileNo;

    @NotBlank(message = "Designation is required")
    private String designation;

    @NotBlank(message = "Institution Name is required")
    private String institutionName;

    @NotBlank(message = "Bank Account Number is required")
    private String bankAccountNumber;

    @NotBlank(message = "IFSC Code is required")
    @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "Invalid IFSC Code format")
    private String ifscCode;

    @NotBlank(message = "Bank Name is required")
    private String bankName;

    @NotBlank(message = "Branch is required")
    private String branch;

    @NotBlank(message = "Faculty Type is required")
    private String facultyType;

    private Boolean isGovernmentHoliday;

    @NotBlank(message = "Sessions Attended is required")
    private String sessionsAttended;

    private List<SubjectDetail> subjects;

    public static class SubjectDetail {
        @NotBlank(message = "Subject code is required")
        private String code;
        
        private Integer fnScripts;
        private Integer anScripts;

        // Getters and Setters
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        
        public Integer getFnScripts() { return fnScripts; }
        public void setFnScripts(Integer fnScripts) { this.fnScripts = fnScripts; }
        
        public Integer getAnScripts() { return anScripts; }
        public void setAnScripts(Integer anScripts) { this.anScripts = anScripts; }
    }

    // Getters and Setters
    public String getValuationDate() { return valuationDate; }
    public void setValuationDate(String valuationDate) { this.valuationDate = valuationDate; }

    public String getExamSeason() { return examSeason; }
    public void setExamSeason(String examSeason) { this.examSeason = examSeason; }

    public String getStaffName() { return staffName; }
    public void setStaffName(String staffName) { this.staffName = staffName; }

    public String getPostHeld() { return postHeld; }
    public void setPostHeld(String postHeld) { this.postHeld = postHeld; }

    public String getMobileNo() { return mobileNo; }
    public void setMobileNo(String mobileNo) { this.mobileNo = mobileNo; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getInstitutionName() { return institutionName; }
    public void setInstitutionName(String institutionName) { this.institutionName = institutionName; }

    public String getBankAccountNumber() { return bankAccountNumber; }
    public void setBankAccountNumber(String bankAccountNumber) { this.bankAccountNumber = bankAccountNumber; }

    public String getIfscCode() { return ifscCode; }
    public void setIfscCode(String ifscCode) { this.ifscCode = ifscCode; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public String getFacultyType() { return facultyType; }
    public void setFacultyType(String facultyType) { this.facultyType = facultyType; }

    public Boolean getIsGovernmentHoliday() { return isGovernmentHoliday; }
    public void setIsGovernmentHoliday(Boolean isGovernmentHoliday) { this.isGovernmentHoliday = isGovernmentHoliday; }

    public String getSessionsAttended() { return sessionsAttended; }
    public void setSessionsAttended(String sessionsAttended) { this.sessionsAttended = sessionsAttended; }

    public List<SubjectDetail> getSubjects() { return subjects; }
    public void setSubjects(List<SubjectDetail> subjects) { this.subjects = subjects; }
}

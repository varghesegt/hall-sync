package com.exam.claims.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "script_details")
public class ScriptDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_record_id", nullable = false)
    @JsonIgnore
    private ClaimRecord claimRecord;

    /** 'FN' or 'AN' */
    @Column(name = "session_type", nullable = false, length = 50)
    private String sessionType;

    /** Serial number within the session (1-15) */
    @Column(name = "serial_number")
    private Integer serialNumber;

    @Column(name = "subject_code", length = 255)
    private String subjectCode;

    @Column(name = "no_of_scripts")
    private Integer noOfScripts = 0;

    // ========== Constructors ==========

    public ScriptDetail() {}

    public ScriptDetail(String sessionType, Integer serialNumber, String subjectCode, Integer noOfScripts) {
        this.sessionType = sessionType;
        this.serialNumber = serialNumber;
        this.subjectCode = subjectCode;
        this.noOfScripts = noOfScripts;
    }

    // ========== Getters and Setters ==========

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ClaimRecord getClaimRecord() { return claimRecord; }
    public void setClaimRecord(ClaimRecord claimRecord) { this.claimRecord = claimRecord; }

    public String getSessionType() { return sessionType; }
    public void setSessionType(String sessionType) { this.sessionType = sessionType; }

    public Integer getSerialNumber() { return serialNumber; }
    public void setSerialNumber(Integer serialNumber) { this.serialNumber = serialNumber; }

    public String getSubjectCode() { return subjectCode; }
    public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }

    public Integer getNoOfScripts() { return noOfScripts; }
    public void setNoOfScripts(Integer noOfScripts) { this.noOfScripts = noOfScripts; }
}

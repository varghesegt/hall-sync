package com.exam.claims.dto;

public class ScriptDetailDTO {

    private String sessionType;
    private Integer serialNumber;
    private String subjectCode;
    private Integer noOfScripts;

    public ScriptDetailDTO() {}

    public ScriptDetailDTO(String sessionType, Integer serialNumber, String subjectCode, Integer noOfScripts) {
        this.sessionType = sessionType;
        this.serialNumber = serialNumber;
        this.subjectCode = subjectCode;
        this.noOfScripts = noOfScripts;
    }

    public String getSessionType() { return sessionType; }
    public void setSessionType(String sessionType) { this.sessionType = sessionType; }

    public Integer getSerialNumber() { return serialNumber; }
    public void setSerialNumber(Integer serialNumber) { this.serialNumber = serialNumber; }

    public String getSubjectCode() { return subjectCode; }
    public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }

    public Integer getNoOfScripts() { return noOfScripts; }
    public void setNoOfScripts(Integer noOfScripts) { this.noOfScripts = noOfScripts; }
}

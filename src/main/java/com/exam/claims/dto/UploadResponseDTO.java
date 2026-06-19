package com.exam.claims.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class UploadResponseDTO {

    private UUID batchId;
    private String examSeason;
    private int totalRecords;
    private int examiners;
    private int assistantExaminers;
    private int chiefExaminers;
    private BigDecimal totalAmount;
    private List<String> warnings;
    private String message;

    public UUID getBatchId() { return batchId; }
    public void setBatchId(UUID batchId) { this.batchId = batchId; }

    public String getExamSeason() { return examSeason; }
    public void setExamSeason(String examSeason) { this.examSeason = examSeason; }

    public int getTotalRecords() { return totalRecords; }
    public void setTotalRecords(int totalRecords) { this.totalRecords = totalRecords; }

    public int getExaminers() { return examiners; }
    public void setExaminers(int examiners) { this.examiners = examiners; }

    public int getAssistantExaminers() { return assistantExaminers; }
    public void setAssistantExaminers(int assistantExaminers) { this.assistantExaminers = assistantExaminers; }

    public int getChiefExaminers() { return chiefExaminers; }
    public void setChiefExaminers(int chiefExaminers) { this.chiefExaminers = chiefExaminers; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}

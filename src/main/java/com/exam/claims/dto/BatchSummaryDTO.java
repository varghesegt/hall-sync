package com.exam.claims.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class BatchSummaryDTO {

    private UUID batchId;
    private long totalRecords;
    private BigDecimal totalAmount;
    private String examSeason;
    private String valuationDate;
    private String createdAt;
    private long examiners;
    private long assistantExaminers;
    private long chiefExaminers;

    public UUID getBatchId() { return batchId; }
    public void setBatchId(UUID batchId) { this.batchId = batchId; }

    public long getTotalRecords() { return totalRecords; }
    public void setTotalRecords(long totalRecords) { this.totalRecords = totalRecords; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getExamSeason() { return examSeason; }
    public void setExamSeason(String examSeason) { this.examSeason = examSeason; }

    public String getValuationDate() { return valuationDate; }
    public void setValuationDate(String valuationDate) { this.valuationDate = valuationDate; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public long getExaminers() { return examiners; }
    public void setExaminers(long examiners) { this.examiners = examiners; }

    public long getAssistantExaminers() { return assistantExaminers; }
    public void setAssistantExaminers(long assistantExaminers) { this.assistantExaminers = assistantExaminers; }

    public long getChiefExaminers() { return chiefExaminers; }
    public void setChiefExaminers(long chiefExaminers) { this.chiefExaminers = chiefExaminers; }
}

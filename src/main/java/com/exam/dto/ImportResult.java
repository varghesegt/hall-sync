package com.exam.dto;

import java.util.ArrayList;
import java.util.List;

public class ImportResult {
    private int totalRows;
    private int successCount;
    private int failedCount;
    private int skippedDuplicates;
    private List<String> errors;

    public ImportResult() {
        this.errors = new ArrayList<>();
    }

    public int getTotalRows() { return totalRows; }
    public void setTotalRows(int totalRows) { this.totalRows = totalRows; }
    public int getSuccessCount() { return successCount; }
    public void setSuccessCount(int successCount) { this.successCount = successCount; }
    public int getFailedCount() { return failedCount; }
    public void setFailedCount(int failedCount) { this.failedCount = failedCount; }
    public int getSkippedDuplicates() { return skippedDuplicates; }
    public void setSkippedDuplicates(int skippedDuplicates) { this.skippedDuplicates = skippedDuplicates; }
    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }

    public void addError(String error) {
        if (this.errors.size() < 50) {
            this.errors.add(error);
        }
    }

    public void incrementSuccess() { this.successCount++; }
    public void incrementFailed() { this.failedCount++; }
    public void incrementSkipped() { this.skippedDuplicates++; }
}

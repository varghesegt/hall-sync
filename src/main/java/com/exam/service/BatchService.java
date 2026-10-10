package com.exam.service;

import com.exam.dto.BatchStatusResponse;
import com.exam.dto.PdfAllocationView;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;

public interface BatchService {
    BatchStatusResponse getBatchStatus(UUID batchId);
    List<PdfAllocationView> getPreviewData(UUID batchId);
    void generatePdf(UUID batchId, OutputStream out);
    void generatePdfWithAllocations(UUID batchId, List<PdfAllocationView> allocations, OutputStream out);
    void generateExcel(UUID batchId, OutputStream out);
    void generateExcelWithAllocations(UUID batchId, List<PdfAllocationView> allocations, OutputStream out);
    void generateSummaryExcel(UUID batchId, OutputStream out);
}

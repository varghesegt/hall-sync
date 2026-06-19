package com.exam.service;

import com.exam.dto.PdfAllocationView;
import com.exam.dto.SummaryAllocationView;
import com.exam.entity.AllocationBatch;
import java.io.OutputStream;
import java.util.List;

public interface ExcelService {
    void generateBatchExcel(AllocationBatch batch, List<PdfAllocationView> allocations, OutputStream out);
    void generateSummaryExcel(AllocationBatch batch, List<SummaryAllocationView> summaryData, OutputStream out);
}

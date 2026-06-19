package com.exam.service;

import com.exam.dto.PdfAllocationView;
import com.exam.entity.AllocationBatch;
import java.io.OutputStream;
import java.util.List;

public interface PdfService {
    void generateHallPdf(AllocationBatch batch, List<PdfAllocationView> allocations, OutputStream out);
}

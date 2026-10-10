package com.exam.service.internal;

import com.exam.dto.BatchStatusResponse;
import com.exam.dto.PdfAllocationView;
import com.exam.dto.SummaryAllocationView;
import com.exam.entity.AllocationBatch;
import com.exam.exception.ResourceNotFoundException;
import com.exam.mapper.EntityMapper;
import com.exam.repository.AllocationBatchRepository;
import com.exam.repository.AllocationRepository;
import com.exam.service.PdfService;
import com.exam.service.ExcelService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.util.List;
import java.util.UUID;

/**
 * Internal Exam batch service.
 * Reuses the same PDF/Excel generators — they read from the same allocations table.
 * The 7×6 grid layout in PDFs/Excels is handled by the allocation data itself
 * (rows 1-7, columns I-VI are stored in the allocations table).
 */
@Service
public class InternalBatchService {

    private final AllocationBatchRepository batchRepo;
    private final AllocationRepository allocationRepo;
    private final PdfService pdfService;
    private final ExcelService excelService;

    public InternalBatchService(AllocationBatchRepository batchRepo, AllocationRepository allocationRepo,
                                 PdfService pdfService, ExcelService excelService) {
        this.batchRepo = batchRepo;
        this.allocationRepo = allocationRepo;
        this.pdfService = pdfService;
        this.excelService = excelService;
    }

    @Transactional(readOnly = true)
    public BatchStatusResponse getBatchStatus(UUID batchId) {
        AllocationBatch batch = batchRepo.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + batchId));
        return EntityMapper.toBatchStatus(batch);
    }

    @Transactional(readOnly = true)
    public List<PdfAllocationView> getPreviewData(UUID batchId) {
        batchRepo.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + batchId));
        return allocationRepo.findPdfViewsByBatchId(batchId);
    }

    @Transactional(readOnly = true)
    public void generatePdf(UUID batchId, OutputStream out) {
        AllocationBatch batch = batchRepo.findByIdWithSession(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + batchId));
        List<PdfAllocationView> allocations = allocationRepo.findPdfViewsByBatchId(batchId);
        if (allocations.isEmpty()) throw new ResourceNotFoundException("No allocations in batch: " + batchId);
        pdfService.generateHallPdf(batch, allocations, out);
    }

    @Transactional(readOnly = true)
    public void generatePdfWithAllocations(UUID batchId, List<PdfAllocationView> allocations, OutputStream out) {
        AllocationBatch batch = batchRepo.findByIdWithSession(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + batchId));
        pdfService.generateHallPdf(batch, allocations, out);
    }

    @Transactional(readOnly = true)
    public void generateExcel(UUID batchId, OutputStream out) {
        AllocationBatch batch = batchRepo.findByIdWithSession(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + batchId));
        List<PdfAllocationView> allocations = allocationRepo.findPdfViewsByBatchId(batchId);
        if (allocations.isEmpty()) throw new ResourceNotFoundException("No allocations in batch: " + batchId);
        excelService.generateBatchExcel(batch, allocations, out);
    }

    @Transactional(readOnly = true)
    public void generateExcelWithAllocations(UUID batchId, List<PdfAllocationView> allocations, OutputStream out) {
        AllocationBatch batch = batchRepo.findByIdWithSession(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + batchId));
        excelService.generateBatchExcel(batch, allocations, out);
    }

    @Transactional(readOnly = true)
    public void generateSummaryExcel(UUID batchId, OutputStream out) {
        AllocationBatch batch = batchRepo.findByIdWithSession(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + batchId));
        List<SummaryAllocationView> summaryData = allocationRepo.findSummaryViewsByBatchId(batchId);
        if (summaryData.isEmpty()) throw new ResourceNotFoundException("No allocations in batch: " + batchId);
        excelService.generateSummaryExcel(batch, summaryData, out);
    }
}

package com.exam.service.impl;

import com.exam.dto.BatchStatusResponse;
import com.exam.entity.AllocationBatch;
import com.exam.exception.ResourceNotFoundException;
import com.exam.mapper.EntityMapper;
import com.exam.repository.AllocationBatchRepository;
import com.exam.repository.AllocationRepository;
import com.exam.service.BatchService;
import com.exam.service.PdfService;
import com.exam.service.ExcelService;
import com.exam.dto.PdfAllocationView;
import com.exam.dto.SummaryAllocationView;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class BatchFacadeImpl implements BatchService {

    private final AllocationBatchRepository batchRepo;
    private final AllocationRepository allocationRepo;
    private final PdfService pdfService;
    private final ExcelService excelService;

    public BatchFacadeImpl(AllocationBatchRepository batchRepo, AllocationRepository allocationRepo, PdfService pdfService, ExcelService excelService) {
        this.batchRepo = batchRepo;
        this.allocationRepo = allocationRepo;
        this.pdfService = pdfService;
        this.excelService = excelService;
    }

    @Override
    @Transactional(readOnly = true)
    public BatchStatusResponse getBatchStatus(UUID batchId) {
        AllocationBatch batch = batchRepo.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found for ID: " + batchId));
        return EntityMapper.toBatchStatus(batch);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PdfAllocationView> getPreviewData(UUID batchId) {
        batchRepo.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found for ID: " + batchId));
        return allocationRepo.findPdfViewsByBatchId(batchId);
    }

    @Override
    @Transactional(readOnly = true)
    public void generatePdf(UUID batchId, java.io.OutputStream out) {
        // Hydrate all data in the front-end thread (with transaction) 
        // before handing off to the async stream.
        AllocationBatch batch = batchRepo.findByIdWithSession(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found for PDF: " + batchId));
        
        List<PdfAllocationView> allocations = allocationRepo.findPdfViewsByBatchId(batchId);
        
        if (allocations.isEmpty()) {
            throw new ResourceNotFoundException("No active allocations found in batch: " + batchId);
        }

        pdfService.generateHallPdf(batch, allocations, out);
    }

    @Override
    @Transactional(readOnly = true)
    public void generatePdfWithAllocations(UUID batchId, List<PdfAllocationView> allocations, java.io.OutputStream out) {
        AllocationBatch batch = batchRepo.findByIdWithSession(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found for PDF: " + batchId));
        pdfService.generateHallPdf(batch, allocations, out);
    }

    @Override
    @Transactional(readOnly = true)
    public void generateExcel(UUID batchId, java.io.OutputStream out) {
        AllocationBatch batch = batchRepo.findByIdWithSession(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found for Excel: " + batchId));
        
        List<PdfAllocationView> allocations = allocationRepo.findPdfViewsByBatchId(batchId);
        
        if (allocations.isEmpty()) {
            throw new ResourceNotFoundException("No active allocations found in batch: " + batchId);
        }

        excelService.generateBatchExcel(batch, allocations, out);
    }

    @Override
    @Transactional(readOnly = true)
    public void generateExcelWithAllocations(UUID batchId, List<PdfAllocationView> allocations, java.io.OutputStream out) {
        AllocationBatch batch = batchRepo.findByIdWithSession(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found for Excel: " + batchId));
        excelService.generateBatchExcel(batch, allocations, out);
    }

    @Override
    @Transactional(readOnly = true)
    public void generateSummaryExcel(UUID batchId, java.io.OutputStream out) {
        AllocationBatch batch = batchRepo.findByIdWithSession(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found for Summary: " + batchId));
        
        List<SummaryAllocationView> summaryData = allocationRepo.findSummaryViewsByBatchId(batchId);
        
        if (summaryData.isEmpty()) {
            throw new ResourceNotFoundException("No active allocations found in batch: " + batchId);
        }

        excelService.generateSummaryExcel(batch, summaryData, out);
    }
}

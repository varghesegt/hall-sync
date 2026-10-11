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

    @Override
    @Transactional(readOnly = true)
    public com.exam.dto.GeneratedDocument generatePdfDocument(UUID batchId) {
        AllocationBatch batch = batchRepo.findByIdWithSession(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found for PDF: " + batchId));
        List<PdfAllocationView> allocations = allocationRepo.findPdfViewsByBatchId(batchId);
        if (allocations.isEmpty()) {
            throw new ResourceNotFoundException("No active allocations found in batch: " + batchId);
        }
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream(64 * 1024);
        pdfService.generateHallPdf(batch, allocations, baos);
        String filename = formatBaseName(batch.getExamSession(), batchId) + ".pdf";
        return new com.exam.dto.GeneratedDocument(baos.toByteArray(), filename);
    }

    @Override
    @Transactional(readOnly = true)
    public com.exam.dto.GeneratedDocument generateExcelDocument(UUID batchId) {
        AllocationBatch batch = batchRepo.findByIdWithSession(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found for Excel: " + batchId));
        List<PdfAllocationView> allocations = allocationRepo.findPdfViewsByBatchId(batchId);
        if (allocations.isEmpty()) {
            throw new ResourceNotFoundException("No active allocations found in batch: " + batchId);
        }
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream(64 * 1024);
        excelService.generateBatchExcel(batch, allocations, baos);
        String filename = formatBaseName(batch.getExamSession(), batchId) + " - Allocation Sheet.xlsx";
        return new com.exam.dto.GeneratedDocument(baos.toByteArray(), filename);
    }

    @Override
    @Transactional(readOnly = true)
    public com.exam.dto.GeneratedDocument generateSummaryExcelDocument(UUID batchId) {
        AllocationBatch batch = batchRepo.findByIdWithSession(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found for Summary: " + batchId));
        List<SummaryAllocationView> summaryData = allocationRepo.findSummaryViewsByBatchId(batchId);
        if (summaryData.isEmpty()) {
            throw new ResourceNotFoundException("No active allocations found in batch: " + batchId);
        }
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream(64 * 1024);
        excelService.generateSummaryExcel(batch, summaryData, baos);
        String filename = formatBaseName(batch.getExamSession(), batchId) + " - Count Opening.xlsx";
        return new com.exam.dto.GeneratedDocument(baos.toByteArray(), filename);
    }

    private String formatBaseName(com.exam.entity.ExamSession session, UUID batchId) {
        if (session != null && session.getExamDate() != null) {
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy");
            String sess = session.getSession() != null ? session.getSession() : "FN";
            return session.getExamDate().format(formatter) + " " + sess;
        }
        return "allocation-" + batchId;
    }
}

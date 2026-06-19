package com.exam.service;

import com.exam.entity.*;
import com.exam.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@Transactional(readOnly = true)
public class EvidencePackService {

    private final AllocationBatchRepository batchRepository;
    private final AllocationRepository allocationRepository;
    private final InvigilatorDutyRepository dutyRepository;

    public EvidencePackService(AllocationBatchRepository batchRepository,
                               AllocationRepository allocationRepository,
                               InvigilatorDutyRepository dutyRepository) {
        this.batchRepository = batchRepository;
        this.allocationRepository = allocationRepository;
        this.dutyRepository = dutyRepository;
    }

    public byte[] generateNaacEvidencePack(UUID batchId) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            
            AllocationBatch batch = batchRepository.findById(batchId).orElseThrow();
            ExamSession session = batch.getExamSession();
            
            StringBuilder seatingCsv = new StringBuilder();
            seatingCsv.append("Session Date,Session Name,Hall Name,Seat Number,Student Register No,Student Department\n");
            
            StringBuilder dutyCsv = new StringBuilder();
            dutyCsv.append("Duty Date,Shift,Faculty Name,Faculty Dept,Hall Name\n");
            
            List<Allocation> allocations = allocationRepository.findByBatchId(batchId);
            for (Allocation alloc : allocations) {
                seatingCsv.append(String.format("%s,%s,%s,%s,%s,%s\n",
                        session.getExamDate(), session.getName(),
                        alloc.getHall().getName(), alloc.getSeatCol() + alloc.getSeatRow(),
                        alloc.getStudent().getRegisterNumber(),
                        alloc.getStudent().getDepartment()));
            }

            // Get Duties for the whole batch
            List<InvigilatorDuty> duties = dutyRepository.findByBatchIdOrderByHallIdAsc(batchId);
            for (InvigilatorDuty duty : duties) {
                dutyCsv.append(String.format("%s,%s,%s,%s,%s\n",
                        duty.getDutyDate(), duty.getShift(),
                        duty.getFaculty().getName(),
                        duty.getFaculty().getDepartment(),
                        duty.getHall().getName()));
            }
            
            // Add Seating Plan CSV
            addEntryToZip(zos, "seating_plans.csv", seatingCsv.toString());
            
            // Add Invigilator Duty CSV
            addEntryToZip(zos, "invigilator_reports.csv", dutyCsv.toString());
            
            // Add Summary Report
            String summary = "NAAC Evidence Pack Summary\n" +
                    "Batch ID: " + batchId + "\n" +
                    "Generated at: " + java.time.LocalDateTime.now() + "\n";
            addEntryToZip(zos, "summary_report.txt", summary);
            
        }
        return baos.toByteArray();
    }
            
    private void addEntryToZip(ZipOutputStream zos, String filename, String content) throws IOException {
        ZipEntry entry = new ZipEntry(filename);
        zos.putNextEntry(entry);
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }
}

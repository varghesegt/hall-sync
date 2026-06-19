package com.exam.controller;

import com.exam.entity.Allocation;
import com.exam.entity.AllocationBatch;
import com.exam.entity.ExamSession;
import com.exam.repository.AllocationRepository;
import com.exam.repository.AllocationBatchRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/integrations/erp")

@Transactional(readOnly = true)
public class ErpIntegrationController {

    private final AllocationRepository allocationRepository;
    private final AllocationBatchRepository batchRepository;

    public ErpIntegrationController(AllocationRepository allocationRepository, AllocationBatchRepository batchRepository) {
        this.allocationRepository = allocationRepository;
        this.batchRepository = batchRepository;
    }

    @GetMapping("/export/camu/{batchId}")
    public ResponseEntity<byte[]> exportToCamu(@PathVariable UUID batchId) {
        return generateExport(batchId, "Camu_Export", "Camu ID,Student Name,Register No,Department,Hall,Seat No,Exam Date,Session Name\n", true);
    }

    @GetMapping("/export/icloudems/{batchId}")
    public ResponseEntity<byte[]> exportToICloudEms(@PathVariable UUID batchId) {
        return generateExport(batchId, "iCloudEMS_Export", "Enrollment No,Program,Batch,Hall No,Seat No,Date,Shift\n", false);
    }

    private ResponseEntity<byte[]> generateExport(UUID batchId, String prefix, String header, boolean isCamu) {
        AllocationBatch batch = batchRepository.findById(batchId).orElseThrow();
        ExamSession session = batch.getExamSession();
        StringBuilder csv = new StringBuilder(header);

        List<Allocation> allocations = allocationRepository.findByBatchId(batchId);
        for (Allocation alloc : allocations) {
            if (isCamu) {
                // Camu ID,Student Name,Register No,Department,Hall,Seat No,Exam Date,Session Name
                csv.append(String.format("%s,%s,%s,%s,%s,%s,%s,%s\n",
                        "", // Camu ID blank
                        alloc.getStudent().getName() != null ? alloc.getStudent().getName() : "",
                        alloc.getStudent().getRegisterNumber(),
                        alloc.getStudent().getDepartment(),
                        alloc.getHall().getName(),
                        alloc.getSeatCol() + alloc.getSeatRow(),
                        session.getExamDate(),
                        session.getName()
                ));
            } else {
                // Enrollment No,Program,Batch,Hall No,Seat No,Date,Shift
                csv.append(String.format("%s,%s,%s,%s,%s,%s,%s\n",
                        alloc.getStudent().getRegisterNumber(),
                        alloc.getStudent().getDepartment(),
                        alloc.getStudent().getSemester() != null ? alloc.getStudent().getSemester() : "",
                        alloc.getHall().getName(),
                        alloc.getSeatCol() + alloc.getSeatRow(),
                        session.getExamDate(),
                        session.getSession()
                ));
            }
        }

        byte[] data = csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("text/csv"));
        headers.setContentDispositionFormData("attachment", prefix + "_" + batchId + ".csv");

        return ResponseEntity.ok()
                .headers(headers)
                .body(data);
    }
}

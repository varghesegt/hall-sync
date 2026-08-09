package com.exam.controller;

import com.exam.entity.AllocationBatch;
import com.exam.repository.AllocationBatchRepository;
import com.exam.repository.ExamArchiveRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/v1/batches")
public class GlobalBatchController {

    private final AllocationBatchRepository allocationBatchRepository;
    private final ExamArchiveRepository examArchiveRepository;

    public GlobalBatchController(AllocationBatchRepository allocationBatchRepository, ExamArchiveRepository examArchiveRepository) {
        this.allocationBatchRepository = allocationBatchRepository;
        this.examArchiveRepository = examArchiveRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getAllBatches() {
        List<AllocationBatch> batches = allocationBatchRepository.findAll();
        
        List<Map<String, Object>> response = batches.stream().map(batch -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", batch.getId());
            
            if (examArchiveRepository.existsByBatchId(batch.getId())) {
                map.put("status", "ARCHIVED");
            } else {
                map.put("status", batch.getStatus());
            }
            
            map.put("createdAt", batch.getCreatedAt());
            
            if (batch.getExamSession() != null) {
                Map<String, Object> sessionMap = new HashMap<>();
                sessionMap.put("id", batch.getExamSession().getId());
                sessionMap.put("seasonId", batch.getExamSession().getSeasonId());
                sessionMap.put("date", batch.getExamSession().getExamDate() != null ? batch.getExamSession().getExamDate().toString() : null);
                sessionMap.put("session", batch.getExamSession().getSession());
                sessionMap.put("examType", batch.getExamSession().getExamType());
                sessionMap.put("name", batch.getExamSession().getExamDate().toString() + " " + batch.getExamSession().getSession());
                map.put("examSession", sessionMap);
            }
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}

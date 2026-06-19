package com.exam.service;

import com.exam.entity.MalpracticeCase;
import com.exam.repository.MalpracticeCaseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class MalpracticeService {

    private static final Logger logger = LoggerFactory.getLogger(MalpracticeService.class);

    private final MalpracticeCaseRepository caseRepository;

    public MalpracticeService(MalpracticeCaseRepository caseRepository) {
        this.caseRepository = caseRepository;
    }

    public List<MalpracticeCase> getAllCases() {
        return caseRepository.findAllByOrderByReportedAtDesc();
    }

    public List<MalpracticeCase> getCasesByBatch(UUID batchId) {
        return caseRepository.findByBatchIdOrderByReportedAtDesc(batchId);
    }

    public List<MalpracticeCase> getCasesByStatus(String status) {
        return caseRepository.findByStatusOrderByReportedAtDesc(status);
    }

    public Optional<MalpracticeCase> getCaseById(UUID id) {
        return caseRepository.findById(id);
    }

    @Transactional
    public MalpracticeCase createCase(MalpracticeCase malpracticeCase) {
        return caseRepository.save(malpracticeCase);
    }

    @Transactional
    public MalpracticeCase updateStatus(UUID caseId, String newStatus, String actionTaken) {
        MalpracticeCase mc = caseRepository.findById(caseId)
                .orElseThrow(() -> new IllegalArgumentException("Case not found: " + caseId));

        mc.setStatus(newStatus);
        if (actionTaken != null && !actionTaken.isBlank()) {
            mc.setActionTaken(actionTaken);
        }
        if ("CLOSED".equals(newStatus) || "ACTION_TAKEN".equals(newStatus)) {
            mc.setResolvedAt(LocalDateTime.now());
        }
        return caseRepository.save(mc);
    }

    @Transactional
    public MalpracticeCase attachEvidence(UUID caseId, MultipartFile file) {
        MalpracticeCase mc = caseRepository.findById(caseId)
                .orElseThrow(() -> new IllegalArgumentException("Case not found: " + caseId));

        try {
            mc.setEvidenceData(file.getBytes());
            mc.setEvidenceFilename(file.getOriginalFilename());
            mc.setEvidenceContentType(file.getContentType());
        } catch (Exception e) {
            throw new RuntimeException("Failed to read evidence file", e);
        }

        return caseRepository.save(mc);
    }

    public Map<String, Object> getTrends(LocalDateTime from, LocalDateTime to) {
        Map<String, Object> trends = new LinkedHashMap<>();

        List<Object[]> byType = caseRepository.getCaseTypeTrends(from, to);
        Map<String, Long> typeMap = new LinkedHashMap<>();
        for (Object[] row : byType) {
            typeMap.put((String) row[0], (Long) row[1]);
        }
        trends.put("byType", typeMap);

        List<Object[]> bySeverity = caseRepository.getSeverityDistribution(from, to);
        Map<String, Long> severityMap = new LinkedHashMap<>();
        for (Object[] row : bySeverity) {
            severityMap.put((String) row[0], (Long) row[1]);
        }
        trends.put("bySeverity", severityMap);

        return trends;
    }
}

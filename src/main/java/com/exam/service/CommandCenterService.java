package com.exam.service;

import com.exam.entity.*;
import com.exam.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class CommandCenterService {

    private final ExamSessionRepository sessionRepository;
    private final HallRepository hallRepository;
    private final InvigilatorDutyRepository dutyRepository;
    private final MalpracticeCaseRepository malpracticeRepository;
    private final AllocationRepository allocationRepository;
    private final AllocationBatchRepository batchRepository;

    public CommandCenterService(ExamSessionRepository sessionRepository,
                                HallRepository hallRepository,
                                InvigilatorDutyRepository dutyRepository,
                                MalpracticeCaseRepository malpracticeRepository,
                                AllocationRepository allocationRepository,
                                AllocationBatchRepository batchRepository) {
        this.sessionRepository = sessionRepository;
        this.hallRepository = hallRepository;
        this.dutyRepository = dutyRepository;
        this.malpracticeRepository = malpracticeRepository;
        this.allocationRepository = allocationRepository;
        this.batchRepository = batchRepository;
    }

    public Map<String, Object> getCommandCenterStats(LocalDate date) {
        List<ExamSession> sessions = sessionRepository.findByExamDate(date);
        
        long totalExpectedStudents = 0;
        long totalPresent = 0;
        long totalFacultyAllocated = 0;
        Set<String> activeHalls = new HashSet<>();
        
        for (ExamSession session : sessions) {
            Optional<AllocationBatch> batchOpt = batchRepository.findByExamSessionIdAndStatus(session.getId(), BatchStatus.ACTIVE);
            if (batchOpt.isPresent()) {
                AllocationBatch batch = batchOpt.get();
                List<Allocation> allocations = allocationRepository.findByBatchId(batch.getId());
                totalExpectedStudents += allocations.size();
                for (Allocation alloc : allocations) {
                    activeHalls.add(alloc.getHall().getId());
                }
                
                List<InvigilatorDuty> duties = dutyRepository.findByBatchIdOrderByHallIdAsc(batch.getId());
                long count = duties.stream().filter(d -> d.getDutyDate().equals(date)).count();
                totalFacultyAllocated += count;
            }
        }

        // Get malpractices for the date
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);
        List<Object[]> malpractices = malpracticeRepository.getCaseTypeTrends(startOfDay, endOfDay);
        long pendingMalpractices = malpractices.stream().mapToLong(row -> (Long) row[1]).sum();

        Map<String, Object> stats = new HashMap<>();
        stats.put("activeHalls", activeHalls.size());
        stats.put("totalExpectedStudents", totalExpectedStudents);
        stats.put("totalFacultyAllocated", totalFacultyAllocated);
        stats.put("totalSessionsToday", sessions.size());
        stats.put("pendingMalpractices", pendingMalpractices);
        
        return stats;
    }
}

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
        
        List<Map<String, Object>> activeBatchesData = new ArrayList<>();
        
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
                
                Map<String, Object> batchData = new HashMap<>();
                batchData.put("batchId", batch.getId());
                batchData.put("examType", session.getExamType());
                batchData.put("session", session.getSession());
                batchData.put("name", session.getName());
                batchData.put("hallCount", allocations.stream().map(a -> a.getHall().getId()).distinct().count());
                batchData.put("studentCount", allocations.size());
                
                // Mock time ranges based on typical FN/AN schedules
                String sStr = session.getSession().toUpperCase();
                if (sStr.contains("FN")) {
                    batchData.put("startTime", date.atTime(9, 30));
                    batchData.put("endTime", date.atTime(12, 30));
                } else if (sStr.contains("AN")) {
                    batchData.put("startTime", date.atTime(13, 30));
                    batchData.put("endTime", date.atTime(16, 30));
                } else {
                    batchData.put("startTime", date.atTime(10, 0));
                    batchData.put("endTime", date.atTime(13, 0));
                }
                activeBatchesData.add(batchData);
            }
        }

        // Get malpractices for the date
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);
        List<Object[]> malpractices = malpracticeRepository.getCaseTypeTrends(startOfDay, endOfDay);
        long pendingMalpractices = malpractices.stream().mapToLong(row -> (Long) row[1]).sum();
        
        // Mock live events
        List<Map<String, Object>> liveEvents = new ArrayList<>();
        liveEvents.add(Map.of("id", UUID.randomUUID(), "time", LocalDateTime.now().minusMinutes(5), "type", "INFO", "message", "System polling cycle completed successfully."));
        if (!activeBatchesData.isEmpty()) {
            liveEvents.add(Map.of("id", UUID.randomUUID(), "time", LocalDateTime.now().minusMinutes(12), "type", "SUCCESS", "message", "Faculty check-in verified for " + activeBatchesData.size() + " active sessions."));
        }
        if (pendingMalpractices > 0) {
            liveEvents.add(Map.of("id", UUID.randomUUID(), "time", LocalDateTime.now().minusMinutes(2), "type", "WARNING", "message", pendingMalpractices + " malpractices reported today awaiting review."));
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("activeHalls", activeHalls.size());
        stats.put("totalExpectedStudents", totalExpectedStudents);
        stats.put("totalFacultyAllocated", totalFacultyAllocated);
        stats.put("totalSessionsToday", sessions.size());
        stats.put("pendingMalpractices", pendingMalpractices);
        stats.put("activeBatches", activeBatchesData);
        stats.put("liveEvents", liveEvents);
        
        return stats;
    }
}

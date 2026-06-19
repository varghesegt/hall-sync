package com.exam.service;

import com.exam.entity.*;
import com.exam.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class StudentAttendanceService {

    private static final Logger logger = LoggerFactory.getLogger(StudentAttendanceService.class);

    private final StudentAttendanceRepository attendanceRepository;
    private final AllocationRepository allocationRepository;
    private final AllocationBatchRepository batchRepository;

    public StudentAttendanceService(StudentAttendanceRepository attendanceRepository,
                                     AllocationRepository allocationRepository,
                                     AllocationBatchRepository batchRepository) {
        this.attendanceRepository = attendanceRepository;
        this.allocationRepository = allocationRepository;
        this.batchRepository = batchRepository;
    }

    @Transactional
    public int initializeAttendance(UUID batchId) {
        if (attendanceRepository.existsByBatchId(batchId)) {
            logger.info("Attendance already initialized for batch {}", batchId);
            return 0;
        }

        AllocationBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + batchId));

        List<Allocation> allocations = allocationRepository.findByBatchIdWithDetails(batchId);
        List<StudentAttendance> records = new ArrayList<>();

        for (Allocation alloc : allocations) {
            StudentAttendance attendance = new StudentAttendance(
                    UUID.randomUUID(), batch, alloc.getStudent(), alloc.getHall());
            records.add(attendance);
        }

        attendanceRepository.saveAll(records);
        logger.info("Initialized {} attendance records for batch {}", records.size(), batchId);
        return records.size();
    }

    @Transactional
    public void markBulkAttendance(UUID batchId, Map<UUID, Boolean> studentPresence, UUID markedByFacultyId) {
        List<StudentAttendance> records = attendanceRepository.findByBatchId(batchId);

        for (StudentAttendance record : records) {
            UUID studentId = record.getStudent().getId();
            if (studentPresence.containsKey(studentId)) {
                record.setIsPresent(studentPresence.get(studentId));
                record.setMarkedAt(LocalDateTime.now());
            }
        }

        attendanceRepository.saveAll(records);
        logger.info("Marked attendance for {} students in batch {}", studentPresence.size(), batchId);
    }

    public List<StudentAttendance> getAttendanceByBatch(UUID batchId) {
        return attendanceRepository.findByBatchId(batchId);
    }

    public Map<String, Object> getAttendanceStats(UUID batchId) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("present", attendanceRepository.countByBatchIdAndIsPresentTrue(batchId));
        stats.put("absent", attendanceRepository.countByBatchIdAndIsPresentFalse(batchId));
        stats.put("unmarked", attendanceRepository.countByBatchIdAndIsPresentIsNull(batchId));

        List<Object[]> byHall = attendanceRepository.getAttendanceSummaryByHall(batchId);
        List<Map<String, Object>> hallStats = new ArrayList<>();
        for (Object[] row : byHall) {
            Map<String, Object> hs = new LinkedHashMap<>();
            hs.put("hallId", row[0]);
            hs.put("present", row[1]);
            hs.put("absent", row[2]);
            hs.put("unmarked", row[3]);
            hallStats.add(hs);
        }
        stats.put("byHall", hallStats);

        return stats;
    }
}

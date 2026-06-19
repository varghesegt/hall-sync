package com.exam.service;

import com.exam.entity.*;
import com.exam.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ExamArchiveService {

    private static final Logger logger = LoggerFactory.getLogger(ExamArchiveService.class);

    private final ExamArchiveRepository archiveRepository;
    private final AllocationBatchRepository batchRepository;
    private final AllocationRepository allocationRepository;
    private final InvigilatorDutyRepository dutyRepository;
    private final StudentAttendanceRepository attendanceRepository;
    private final MalpracticeCaseRepository malpracticeRepository;

    public ExamArchiveService(ExamArchiveRepository archiveRepository,
                              AllocationBatchRepository batchRepository,
                              AllocationRepository allocationRepository,
                              InvigilatorDutyRepository dutyRepository,
                              StudentAttendanceRepository attendanceRepository,
                              MalpracticeCaseRepository malpracticeRepository) {
        this.archiveRepository = archiveRepository;
        this.batchRepository = batchRepository;
        this.allocationRepository = allocationRepository;
        this.dutyRepository = dutyRepository;
        this.attendanceRepository = attendanceRepository;
        this.malpracticeRepository = malpracticeRepository;
    }

    @Transactional
    public ExamArchive archiveBatch(UUID batchId, String archivedBy) {
        if (archiveRepository.existsByBatchId(batchId)) {
            throw new IllegalStateException("Batch " + batchId + " is already archived.");
        }

        AllocationBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + batchId));

        ExamSession session = batch.getExamSession();

        // Compute stats
        List<Allocation> allocations = allocationRepository.findByBatchIdWithDetails(batchId);
        int totalStudents = allocations.size();
        long totalHalls = allocations.stream().map(a -> a.getHall().getId()).distinct().count();
        long totalInvigilators = dutyRepository.findByBatchIdOrderByHallIdAsc(batchId).size();
        long totalAbsentees = attendanceRepository.countByBatchIdAndIsPresentFalse(batchId);
        long totalMalpractice = malpracticeRepository.countByBatchId(batchId);

        ExamArchive archive = new ExamArchive(
                UUID.randomUUID(), session, batch,
                session.getExamType(), session.getExamDate(), session.getSession(),
                totalStudents, (int) totalHalls, archivedBy);

        archive.setTotalInvigilators((int) totalInvigilators);
        archive.setTotalAbsentees((int) totalAbsentees);
        archive.setTotalMalpractice((int) totalMalpractice);

        archiveRepository.save(archive);
        logger.info("Archived batch {} — {} students, {} halls, {} invigilators",
                batchId, totalStudents, totalHalls, totalInvigilators);

        return archive;
    }

    public List<ExamArchive> getAllArchives() {
        return archiveRepository.findAllByOrderByArchivedAtDesc();
    }

    public Optional<ExamArchive> getById(UUID id) {
        return archiveRepository.findById(id);
    }

    public List<ExamArchive> getByDateRange(java.time.LocalDate from, java.time.LocalDate to) {
        return archiveRepository.findByExamDateBetweenOrderByExamDateAsc(from, to);
    }
}

package com.exam.repository;

import com.exam.entity.ExamArchive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExamArchiveRepository extends JpaRepository<ExamArchive, UUID> {

    List<ExamArchive> findAllByOrderByArchivedAtDesc();

    List<ExamArchive> findByExamDateBetweenOrderByExamDateAsc(LocalDate from, LocalDate to);

    Optional<ExamArchive> findByBatchId(UUID batchId);

    boolean existsByBatchId(UUID batchId);
}

package com.exam.repository;

import com.exam.entity.MalpracticeCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface MalpracticeCaseRepository extends JpaRepository<MalpracticeCase, UUID> {

    List<MalpracticeCase> findByBatchIdOrderByReportedAtDesc(UUID batchId);

    List<MalpracticeCase> findByStatusOrderByReportedAtDesc(String status);

    List<MalpracticeCase> findAllByOrderByReportedAtDesc();

    long countByBatchId(UUID batchId);

    @Query("SELECT m.caseType, COUNT(m) FROM MalpracticeCase m " +
           "WHERE m.reportedAt BETWEEN :from AND :to GROUP BY m.caseType")
    List<Object[]> getCaseTypeTrends(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT m.severity, COUNT(m) FROM MalpracticeCase m " +
           "WHERE m.reportedAt BETWEEN :from AND :to GROUP BY m.severity")
    List<Object[]> getSeverityDistribution(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}

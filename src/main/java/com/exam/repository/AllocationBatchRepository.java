package com.exam.repository;

import com.exam.entity.AllocationBatch;
import com.exam.entity.BatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AllocationBatchRepository extends JpaRepository<AllocationBatch, UUID> {
    @Query("SELECT b FROM AllocationBatch b JOIN FETCH b.examSession WHERE b.id = :id")
    Optional<AllocationBatch> findByIdWithSession(@Param("id") UUID id);

    Optional<AllocationBatch> findByExamSessionIdAndStatus(UUID examSessionId, BatchStatus status);
    
    Optional<AllocationBatch> findFirstByExamSessionIdOrderByCreatedAtDesc(UUID examSessionId);
    
    @Query("SELECT COALESCE(MAX(b.version), 0) FROM AllocationBatch b WHERE b.examSession.id = :examSessionId")
    int findMaxVersionByExamSessionId(@Param("examSessionId") UUID examSessionId);

    @Modifying
    @Query("UPDATE AllocationBatch b SET b.status = 'SUPERSEDED' WHERE b.examSession.id = :examSessionId AND b.status = 'ACTIVE' AND b.id != :newBatchId")
    void markOthersSuperseded(@Param("examSessionId") UUID examSessionId, @Param("newBatchId") UUID newBatchId);

    @Query("SELECT b FROM AllocationBatch b WHERE b.status = 'RUNNING' AND b.createdAt < :threshold")
    java.util.List<AllocationBatch> findStaleRunningBatches(@Param("threshold") java.time.LocalDateTime threshold);
}

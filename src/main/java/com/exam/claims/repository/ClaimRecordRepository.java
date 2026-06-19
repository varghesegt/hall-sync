package com.exam.claims.repository;

import com.exam.claims.entity.ClaimRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClaimRecordRepository extends JpaRepository<ClaimRecord, Long> {

    List<ClaimRecord> findByBatchIdOrderBySerialNumber(UUID batchId);

    @Query("SELECT c.batchId FROM ClaimRecord c GROUP BY c.batchId ORDER BY MAX(c.createdAt) DESC")
    List<UUID> findDistinctBatchIds();

    long countByBatchId(UUID batchId);

    void deleteByBatchId(UUID batchId);

    @Query("SELECT COALESCE(SUM(c.totalAmount), 0) FROM ClaimRecord c WHERE c.batchId = :batchId")
    java.math.BigDecimal sumTotalAmountByBatchId(UUID batchId);
}

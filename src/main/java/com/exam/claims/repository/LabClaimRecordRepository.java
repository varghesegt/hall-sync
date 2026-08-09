package com.exam.claims.repository;

import com.exam.claims.entity.LabClaimRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface LabClaimRecordRepository extends JpaRepository<LabClaimRecord, Long> {

    List<LabClaimRecord> findByBatchId(UUID batchId);

    List<LabClaimRecord> findByExamDate(LocalDate examDate);

    List<LabClaimRecord> findByDepartment(String department);

    List<LabClaimRecord> findByStaffRole(String staffRole);
}

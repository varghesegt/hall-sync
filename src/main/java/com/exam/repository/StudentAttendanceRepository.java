package com.exam.repository;

import com.exam.entity.StudentAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StudentAttendanceRepository extends JpaRepository<StudentAttendance, UUID> {

    List<StudentAttendance> findByBatchId(UUID batchId);

    @Query("SELECT a FROM StudentAttendance a JOIN FETCH a.student JOIN FETCH a.hall WHERE a.batch.id = :batchId AND a.isPresent = false ORDER BY a.hall.name ASC, a.student.registerNumber ASC")
    List<StudentAttendance> findAbsenteesByBatch(@Param("batchId") UUID batchId);

    long countByBatchIdAndIsPresentFalse(UUID batchId);

    long countByBatchIdAndIsPresentTrue(UUID batchId);

    long countByBatchIdAndIsPresentIsNull(UUID batchId);

    @Query("SELECT a.hall.id, " +
           "SUM(CASE WHEN a.isPresent = true THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN a.isPresent = false THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN a.isPresent IS NULL THEN 1 ELSE 0 END) " +
           "FROM StudentAttendance a WHERE a.batch.id = :batchId GROUP BY a.hall.id")
    List<Object[]> getAttendanceSummaryByHall(@Param("batchId") UUID batchId);

    boolean existsByBatchId(UUID batchId);
}

package com.exam.repository;

import com.exam.entity.Allocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AllocationRepository extends JpaRepository<Allocation, UUID> {

    List<Allocation> findByBatchId(UUID batchId);

    @Query("SELECT a FROM Allocation a " +
           "JOIN FETCH a.student " +
           "JOIN FETCH a.hall " +
           "WHERE a.batch.id = :batchId")
    List<Allocation> findByBatchIdWithDetails(@Param("batchId") UUID batchId);

    @Query("SELECT a FROM Allocation a " +
           "WHERE a.student.registerNumber IN :registerNumbers " +
           "AND a.batch.examSession.seasonId = :seasonId " +
           "AND a.batch.status = 'ACTIVE' " +
           "AND a.batch.examSession.id != :currentSessionId")
    List<Allocation> findSeasonHistory(
            @Param("registerNumbers") List<String> registerNumbers,
            @Param("seasonId") String seasonId,
            @Param("currentSessionId") UUID currentSessionId);

    @Query("SELECT new com.exam.dto.PdfAllocationView(a.hall.id, a.hall.name, a.seatRow, a.seatCol, a.student.registerNumber, a.student.name, a.student.department, a.student.subjectCode, a.student.semester, a.student.regulation, a.riskScore) " +
           "FROM Allocation a WHERE a.batch.id = :batchId " +
           "ORDER BY a.hall.name ASC, a.seatCol ASC, a.seatRow ASC")
    List<com.exam.dto.PdfAllocationView> findPdfViewsByBatchId(@Param("batchId") UUID batchId);

    @Query("SELECT new com.exam.dto.SummaryAllocationView(a.hall.id, a.hall.name, a.student.department, a.student.subjectName, a.student.subjectCode, COUNT(a)) " +
           "FROM Allocation a " +
           "WHERE a.batch.id = :batchId " +
           "GROUP BY a.hall.id, a.hall.name, a.student.department, a.student.subjectName, a.student.subjectCode " +
           "ORDER BY a.hall.name ASC, a.student.department ASC")
    List<com.exam.dto.SummaryAllocationView> findSummaryViewsByBatchId(@Param("batchId") UUID batchId);

    // ==================== VISUAL OVERRIDE QUERIES (NEW) ====================

    @Query("SELECT a FROM Allocation a " +
           "JOIN FETCH a.student " +
           "JOIN FETCH a.hall " +
           "WHERE a.batch.id = :batchId " +
           "AND a.hall.id = :hallId " +
           "AND a.seatRow = :seatRow " +
           "AND a.seatCol = :seatCol")
    java.util.Optional<Allocation> findBySeatPosition(
            @Param("batchId") UUID batchId,
            @Param("hallId") String hallId,
            @Param("seatRow") Integer seatRow,
            @Param("seatCol") String seatCol);
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Allocation a SET a.seatRow = -1 * a.seatRow - 1000 WHERE a.id = :id")
    void moveToTempState(@Param("id") UUID id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Allocation a SET a.hall.id = :hallId, a.seatRow = :seatRow, a.seatCol = :seatCol WHERE a.id = :id")
    void moveToFinalState(@Param("id") UUID id, @Param("hallId") String hallId, @Param("seatRow") Integer seatRow, @Param("seatCol") String seatCol);
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Student s SET s.registerNumber = :regNo WHERE s.id = (SELECT a.student.id FROM Allocation a WHERE a.id = :allocId)")
    void updateRegisterNumber(@Param("allocId") UUID allocId, @Param("regNo") String regNo);
}

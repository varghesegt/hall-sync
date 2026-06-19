package com.exam.repository;

import com.exam.entity.InvigilatorDuty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface InvigilatorDutyRepository extends JpaRepository<InvigilatorDuty, UUID> {

    @Query("SELECT d FROM InvigilatorDuty d JOIN FETCH d.faculty JOIN FETCH d.hall WHERE d.batch.id = :batchId ORDER BY d.hall.id ASC")
    List<InvigilatorDuty> findByBatchIdOrderByHallIdAsc(@Param("batchId") UUID batchId);

    List<InvigilatorDuty> findByFacultyId(UUID facultyId);

    long countByFacultyIdAndDutyDateBetween(UUID facultyId, LocalDate from, LocalDate to);

    @Modifying
    @Query("DELETE FROM InvigilatorDuty d WHERE d.batch.id = :batchId")
    void deleteByBatchId(@Param("batchId") UUID batchId);

    @Query("SELECT d.faculty.id, COUNT(d) FROM InvigilatorDuty d " +
           "WHERE d.dutyDate BETWEEN :from AND :to GROUP BY d.faculty.id")
    List<Object[]> getWorkloadByFacultyBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT d.faculty.department, COUNT(d) FROM InvigilatorDuty d " +
           "WHERE d.dutyDate BETWEEN :from AND :to GROUP BY d.faculty.department")
    List<Object[]> getWorkloadByDepartmentBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT d.faculty, COUNT(d) FROM InvigilatorDuty d " +
           "WHERE d.dutyDate BETWEEN :from AND :to GROUP BY d.faculty ORDER BY d.faculty.department ASC, d.faculty.name ASC")
    List<Object[]> getRemunerationDataBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}

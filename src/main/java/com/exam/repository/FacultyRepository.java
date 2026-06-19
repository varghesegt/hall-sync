package com.exam.repository;

import com.exam.entity.Faculty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FacultyRepository extends JpaRepository<Faculty, UUID> {

    List<Faculty> findByIsActiveTrueOrderByDepartmentAscNameAsc();

    List<Faculty> findByIsActiveTrueAndIsAvailableTrueOrderByDepartmentAscNameAsc();

    List<Faculty> findByDepartmentAndIsActiveTrue(String department);

    Optional<Faculty> findByEmployeeId(String employeeId);

    boolean existsByEmployeeId(String employeeId);

    long countByIsActiveTrue();
}

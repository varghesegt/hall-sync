package com.exam.claims.repository;

import com.exam.claims.entity.CollegeDistance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CollegeDistanceRepository extends JpaRepository<CollegeDistance, Long> {

    Optional<CollegeDistance> findFirstByInstitutionCode(String institutionCode);

    default Optional<CollegeDistance> findByInstitutionCode(String institutionCode) {
        return findFirstByInstitutionCode(institutionCode);
    }

    @Query("SELECT c FROM CollegeDistance c WHERE LOWER(c.institutionName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<CollegeDistance> searchByName(String name);

    @Query("SELECT c FROM CollegeDistance c WHERE c.institutionName LIKE CONCAT('%', :code, '%') OR c.institutionCode = :code")
    List<CollegeDistance> findByCodeInName(String code);
}

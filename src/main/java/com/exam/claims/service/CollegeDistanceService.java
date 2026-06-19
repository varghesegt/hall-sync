package com.exam.claims.service;

import com.exam.claims.entity.CollegeDistance;
import com.exam.claims.repository.CollegeDistanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class CollegeDistanceService {

    private static final Logger log = LoggerFactory.getLogger(CollegeDistanceService.class);

    @Autowired
    private CollegeDistanceRepository repository;

    public List<CollegeDistance> getAllColleges() {
        return repository.findAll();
    }

    public List<CollegeDistance> searchByName(String name) {
        return repository.searchByName(name);
    }

    public Optional<CollegeDistance> findByCode(String code) {
        return repository.findByInstitutionCode(code);
    }

    public CollegeDistance save(CollegeDistance college) {
        return repository.save(college);
    }

    public BigDecimal getDistance(String institutionCode, String institutionName) {
        // Try by code first
        if (institutionCode != null && !institutionCode.isEmpty()) {
            Optional<CollegeDistance> byCode = repository.findByInstitutionCode(institutionCode);
            if (byCode.isPresent()) {
                return byCode.get().getDistanceKm();
            }
        }

        // Try by name
        if (institutionName != null && !institutionName.isEmpty()) {
            String searchName = institutionName.replaceAll("\\(.*\\)", "").trim();
            List<CollegeDistance> results = repository.searchByName(searchName);
            if (!results.isEmpty()) {
                return results.get(0).getDistanceKm();
            }
        }

        log.warn("No distance found for institution: {} ({})", institutionName, institutionCode);
        return BigDecimal.ZERO;
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}

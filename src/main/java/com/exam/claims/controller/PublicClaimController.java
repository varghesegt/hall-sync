package com.exam.claims.controller;

import com.exam.claims.entity.ClaimRecord;
import com.exam.claims.entity.ScriptDetail;
import com.exam.claims.repository.ClaimRecordRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.transaction.Transactional;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import com.exam.claims.dto.ClaimSubmissionRequest;

@RestController
@RequestMapping("/api/v1/public/claims")
@CrossOrigin(origins = "*") // Allows the public frontend route to hit this if needed
public class PublicClaimController {

    private final ClaimRecordRepository claimRepository;

    public PublicClaimController(ClaimRecordRepository claimRepository) {
        this.claimRepository = claimRepository;
    }

    @PostMapping("/{batchId}")
    @Transactional
    public ResponseEntity<?> submitPublicClaim(@PathVariable UUID batchId, @Valid @RequestBody ClaimSubmissionRequest payload) {
        ClaimRecord record = new ClaimRecord();
        record.setBatchId(batchId);
        
        if (payload.getValuationDate() != null && !payload.getValuationDate().isEmpty()) {
            record.setValuationDate(LocalDate.parse(payload.getValuationDate()));
        } else {
            record.setValuationDate(LocalDate.now());
        }

        if (payload.getExamSeason() != null && !payload.getExamSeason().isEmpty()) {
            record.setExamSeason(payload.getExamSeason());
        }
        
        // Map basic fields
        record.setStaffName(payload.getStaffName());
        record.setPostHeld(payload.getPostHeld());
        record.setMobileNo(payload.getMobileNo());
        record.setDesignation(payload.getDesignation());
        record.setInstitutionName(payload.getInstitutionName());
        record.setBankAccountNumber(payload.getBankAccountNumber());
        record.setIfscCode(payload.getIfscCode());
        record.setBankName(payload.getBankName());
        record.setBranch(payload.getBranch());
        
        record.setFacultyType(payload.getFacultyType()); 
        record.setGovernmentHoliday(Boolean.TRUE.equals(payload.getIsGovernmentHoliday()));
        record.setSessionsAttended(payload.getSessionsAttended());

        // Calculate FN and AN scripts total
        int totalFn = 0;
        int totalAn = 0;

        if (payload.getSubjects() != null) {
            int serial = 1;
            for (ClaimSubmissionRequest.SubjectDetail sub : payload.getSubjects()) {
                String code = sub.getCode();
                int fnCount = sub.getFnScripts() != null ? sub.getFnScripts() : 0;
                int anCount = sub.getAnScripts() != null ? sub.getAnScripts() : 0;

                if (fnCount > 0) {
                    record.addScriptDetail(new ScriptDetail("FN", serial, code, fnCount));
                    totalFn += fnCount;
                }
                if (anCount > 0) {
                    record.addScriptDetail(new ScriptDetail("AN", serial, code, anCount));
                    totalAn += anCount;
                }
                serial++;
            }
        }

        record.setFnScripts(totalFn);
        record.setAnScripts(totalAn);
        record.setTotalScripts(totalFn + totalAn);

        claimRepository.save(record);
        return ResponseEntity.ok(Map.of("message", "Claim submitted successfully", "id", record.getId()));
    }
}

package com.exam.claims.service;

import com.exam.claims.entity.LabClaimRecord;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

public class LabClaimCalculationServiceTest {

    private final LabClaimCalculationService service = new LabClaimCalculationService();

    @Test
    public void testAllFourRolesPracticalUGCalculation() {
        // 1. External Examiner: 20 candidates appeared, <= 35km distance, Both sessions
        LabClaimRecord ext = new LabClaimRecord();
        ext.setStaffRole("EXTERNAL_EXAMINER");
        ext.setSubjectName("CS3461 Operating Systems Lab");
        ext.setSubjectCode("CS3461");
        ext.setSemester("IV");
        ext.setRegisteredCount(20);
        ext.setPresentCount(20);
        ext.setDistanceKm(new BigDecimal("25.0"));
        ext.setSession("Both FN and AN");
        service.calculateLabClaim(ext);

        // Remuneration: max(20 * 25, 150) = max(500, 150) = 500
        // TA: 150.00 (<= 35km)
        // DA: 300.00 (Both sessions)
        // Total: 500 + 150 + 300 = 950.00
        assertEquals(new BigDecimal("500.00"), ext.getRemunerationAmount());
        assertEquals(new BigDecimal("150.00"), ext.getTaAmount());
        assertEquals(new BigDecimal("300.00"), ext.getDaAmount());
        assertEquals(new BigDecimal("950.00"), ext.getTotalAmount());

        // 2. Internal Examiner: 20 candidates appeared
        LabClaimRecord intRec = new LabClaimRecord();
        intRec.setStaffRole("INTERNAL_EXAMINER");
        intRec.setSubjectName("CS3461 Operating Systems Lab");
        intRec.setSubjectCode("CS3461");
        intRec.setSemester("IV");
        intRec.setRegisteredCount(20);
        intRec.setPresentCount(20);
        service.calculateLabClaim(intRec);

        // Remuneration: max(20 * 25, 150) = 500.00
        assertEquals(new BigDecimal("500.00"), intRec.getRemunerationAmount());
        assertEquals(new BigDecimal("0.00"), intRec.getTaAmount());
        assertEquals(new BigDecimal("0.00"), intRec.getDaAmount());
        assertEquals(new BigDecimal("500.00"), intRec.getTotalAmount());

        // 3. Skilled Assistant: 20 candidates registered
        LabClaimRecord skilled = new LabClaimRecord();
        skilled.setStaffRole("SKILLED_ASSISTANT");
        skilled.setSubjectName("CS3461 Operating Systems Lab");
        skilled.setSubjectCode("CS3461");
        skilled.setSemester("IV");
        skilled.setRegisteredCount(20);
        skilled.setPresentCount(20);
        service.calculateLabClaim(skilled);

        // Remuneration: max(20 * 12, 100) = max(240, 100) = 240.00
        assertEquals(new BigDecimal("240.00"), skilled.getRemunerationAmount());
        assertEquals(new BigDecimal("240.00"), skilled.getTotalAmount());

        // 4. Lab Technician / Attender: 20 candidates registered
        LabClaimRecord tech = new LabClaimRecord();
        tech.setStaffRole("LAB_ATTENDER");
        tech.setSubjectName("CS3461 Operating Systems Lab");
        tech.setSubjectCode("CS3461");
        tech.setSemester("IV");
        tech.setRegisteredCount(20);
        tech.setPresentCount(20);
        service.calculateLabClaim(tech);

        // Remuneration: max(20 * 6, 50) = max(120, 50) = 120.00
        assertEquals(new BigDecimal("120.00"), tech.getRemunerationAmount());
        assertEquals(new BigDecimal("120.00"), tech.getTotalAmount());
    }

    @Test
    public void testMinimumGuaranteedRates() {
        // Skilled Assistant with 5 candidates: 5 * 12 = 60 < 100 -> min Rs 100
        LabClaimRecord skilledLow = new LabClaimRecord();
        skilledLow.setStaffRole("SKILLED_ASSISTANT");
        skilledLow.setSubjectName("CS3461 Lab");
        skilledLow.setRegisteredCount(5);
        skilledLow.setPresentCount(5);
        service.calculateLabClaim(skilledLow);

        assertEquals(new BigDecimal("100.00"), skilledLow.getRemunerationAmount());

        // Lab Technician with 5 candidates: 5 * 6 = 30 < 50 -> min Rs 50
        LabClaimRecord techLow = new LabClaimRecord();
        techLow.setStaffRole("LAB_ATTENDER");
        techLow.setSubjectName("CS3461 Lab");
        techLow.setRegisteredCount(5);
        techLow.setPresentCount(5);
        service.calculateLabClaim(techLow);

        assertEquals(new BigDecimal("50.00"), techLow.getRemunerationAmount());
    }
}

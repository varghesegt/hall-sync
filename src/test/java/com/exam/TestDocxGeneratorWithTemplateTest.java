package com.exam;

import com.exam.claims.entity.LabClaimRecord;
import com.exam.claims.service.LabClaimCalculationService;
import com.exam.claims.service.LabWordGeneratorService;
import org.junit.jupiter.api.Test;

import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TestDocxGeneratorWithTemplateTest {

    @Test
    public void generateExactDocxFromTemplate() throws Exception {
        LabClaimCalculationService calculationService = new LabClaimCalculationService();
        LabWordGeneratorService wordGeneratorService = new LabWordGeneratorService();

        List<LabClaimRecord> records = new ArrayList<>();

        // External Examiner
        LabClaimRecord ext = new LabClaimRecord();
        ext.setExamSeason("April./May. 2026");
        ext.setExamDate(LocalDate.of(2026, 6, 6));
        ext.setSession("Only FN");
        ext.setDepartment("ECE");
        ext.setSemester("I");
        ext.setSubjectCode("GEA1107");
        ext.setSubjectName("C PROGRAMMING");
        ext.setRegisteredCount(23);
        ext.setPresentCount(22);
        ext.setNameTitle("Mrs.");
        ext.setStaffName("Mrs. RANJANI U");
        ext.setStaffRole("EXTERNAL_EXAMINER");
        ext.setDesignation("Assistant Professor");
        ext.setDepartment("AIDS");
        ext.setInstitutionName("MAM SCHOOL OF ENGINEERING (8121)");
        ext.setDistanceKm(new BigDecimal("30"));
        ext.setMobileNo("8270981412");
        ext.setBankAccountNumber("6658000400036837");
        ext.setBankName("PUNJAB NATIONAL BANK");
        ext.setIfscCode("PUNB0665800");
        ext.setBranch("SIRUGANUR");
        calculationService.calculateLabClaim(ext);
        records.add(ext);

        // Internal Examiner
        LabClaimRecord intRec = new LabClaimRecord();
        intRec.setExamSeason("April./May. 2026");
        intRec.setExamDate(LocalDate.of(2026, 6, 6));
        intRec.setSession("Only FN");
        intRec.setDepartment("ECE");
        intRec.setSemester("I");
        intRec.setSubjectCode("GEA1107");
        intRec.setSubjectName("C PROGRAMMING");
        intRec.setRegisteredCount(23);
        intRec.setPresentCount(22);
        intRec.setNameTitle("Mr.");
        intRec.setStaffName("Mr. KASTHURI RENGAN P");
        intRec.setStaffRole("INTERNAL_EXAMINER");
        intRec.setDesignation("Assistant Professor");
        intRec.setDepartment("CSE");
        intRec.setInstitutionName("KRCE");
        intRec.setMobileNo("9842612131");
        intRec.setBankAccountNumber("913010015846824");
        intRec.setBankName("AXIS BANK");
        intRec.setIfscCode("UTIB0001746");
        intRec.setBranch("KOTTUR");
        calculationService.calculateLabClaim(intRec);
        records.add(intRec);

        // Skilled Assistant
        LabClaimRecord skilled = new LabClaimRecord();
        skilled.setExamSeason("April./May. 2026");
        skilled.setExamDate(LocalDate.of(2026, 6, 6));
        skilled.setSession("Only FN");
        skilled.setDepartment("ECE");
        skilled.setSemester("I");
        skilled.setSubjectCode("GEA1107");
        skilled.setSubjectName("C PROGRAMMING");
        skilled.setRegisteredCount(23);
        skilled.setPresentCount(22);
        skilled.setNameTitle("Mrs.");
        skilled.setStaffName("Mrs. SURYA G");
        skilled.setStaffRole("SKILLED_ASSISTANT");
        skilled.setDesignation("Assistant Professor");
        skilled.setDepartment("CSE");
        skilled.setInstitutionName("KRCE");
        skilled.setMobileNo("8220915957");
        skilled.setBankAccountNumber("609301501667");
        skilled.setBankName("ICICI BANK");
        skilled.setIfscCode("ICIC0006093");
        skilled.setBranch("JAWAHAR STREET NACHANDUPATTI");
        calculationService.calculateLabClaim(skilled);
        records.add(skilled);

        byte[] docxBytes = wordGeneratorService.generateBatchWord(records);

        String outputPath = "c:/Users/Asus/Downloads/Exact_Generated_Lab_Claim_Form.docx";
        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            fos.write(docxBytes);
        }

        System.out.println("SUCCESSFULLY GENERATED EXACT DOCX FROM TEMPLATE AT: " + outputPath);
    }
}

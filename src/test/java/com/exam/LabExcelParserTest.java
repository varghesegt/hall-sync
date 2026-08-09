package com.exam;

import com.exam.claims.entity.LabClaimRecord;
import com.exam.claims.repository.CollegeDistanceRepository;
import com.exam.claims.service.LabClaimCalculationService;
import com.exam.claims.service.LabExcelParserService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LabExcelParserTest {

    @InjectMocks
    private LabExcelParserService parserService;

    @Mock
    private CollegeDistanceRepository collegeDistanceRepository;

    @InjectMocks
    private LabClaimCalculationService calculationService = new LabClaimCalculationService();

    @Test
    public void testParse49ColumnComprehensiveExcelSheet() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Inject calculation service manually
        org.springframework.test.util.ReflectionTestUtils.setField(parserService, "calculationService", calculationService);

        File file = new File("C:/Users/Asus/Downloads/Comprehensive_Lab_Claims_Form_Responses.xlsx");
        assertTrue(file.exists(), "Comprehensive excel file must exist in Downloads");

        FileInputStream fis = new FileInputStream(file);
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "Comprehensive_Lab_Claims_Form_Responses.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                fis
        );

        LabExcelParserService.ParseResult result = parserService.parseExcel(multipartFile);

        assertNotNull(result, "ParseResult should not be null");
        List<LabClaimRecord> records = result.getRecords();

        assertNotNull(records);
        assertEquals(420, records.size(), "105 lab sessions with 4 roles each should yield 420 records");

        // Verify Roles count
        long extCount = records.stream().filter(r -> "EXTERNAL_EXAMINER".equals(r.getStaffRole())).count();
        long intCount = records.stream().filter(r -> "INTERNAL_EXAMINER".equals(r.getStaffRole())).count();
        long skilledCount = records.stream().filter(r -> "SKILLED_ASSISTANT".equals(r.getStaffRole())).count();
        long techCount = records.stream().filter(r -> "LAB_ATTENDER".equals(r.getStaffRole())).count();

        assertEquals(105, extCount, "Should extract 105 External Examiners");
        assertEquals(105, intCount, "Should extract 105 Internal Examiners");
        assertEquals(105, skilledCount, "Should extract 105 Skilled Assistants");
        assertEquals(105, techCount, "Should extract 105 Lab Technicians");

        // Verify Data Integrity on a sample External Examiner
        LabClaimRecord sampleExt = records.stream().filter(r -> "EXTERNAL_EXAMINER".equals(r.getStaffRole())).findFirst().orElse(null);
        assertNotNull(sampleExt);
        assertNotNull(sampleExt.getStaffName());
        assertNotNull(sampleExt.getBankAccountNumber());
        assertNotNull(sampleExt.getIfscCode());
        assertTrue(sampleExt.getTotalAmount().doubleValue() > 0, "External Total Amount should be > 0");

        // Verify Data Integrity on a sample Skilled Assistant
        LabClaimRecord sampleSkilled = records.stream().filter(r -> "SKILLED_ASSISTANT".equals(r.getStaffRole())).findFirst().orElse(null);
        assertNotNull(sampleSkilled);
        assertNotNull(sampleSkilled.getStaffName());
        assertTrue(sampleSkilled.getTotalAmount().doubleValue() >= 100, "Skilled Asst Total Amount should be >= min 100");

        // Verify Data Integrity on a sample Lab Tech
        LabClaimRecord sampleTech = records.stream().filter(r -> "LAB_ATTENDER".equals(r.getStaffRole())).findFirst().orElse(null);
        assertNotNull(sampleTech);
        assertNotNull(sampleTech.getStaffName());
        assertTrue(sampleTech.getTotalAmount().doubleValue() >= 50, "Lab Tech Total Amount should be >= min 50");

        System.out.println("Excel parsing test passed successfully! Extracted " + records.size() + " records across all 4 roles.");
    }
}

package com.exam;

import com.exam.entity.Faculty;
import com.exam.service.AppointmentOrderService;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ExaminerValuationTest {

    private final AppointmentOrderService service = new AppointmentOrderService(null);

    @Test
    public void testExaminerValuationDepartmentAndBoardReplacement() throws Exception {
        Faculty faculty = new Faculty(
                UUID.randomUUID(),
                "Dr. NALLATHAMBI",
                "EMP102",
                "Department of ECE",
                "Associate Professor",
                "7010026516",
                "nallathambi@krce.ac.in",
                "K. RAMAKRISHNAN COLLEGE OF ENGINEERING",
                true,
                true
        );

        byte[] wordBytes = service.generateAppointmentOrderWord(
                faculty,
                "Examiner for Valuation",
                "APRIL/MAY-2026",
                LocalDate.of(2026, 8, 2),
                "ENGLISH", // Even if ENGLISH is passed as board param, Examiner Valuation MUST use staff dept ECE!
                "9.00 a.m. - 05.00 p.m.",
                "COE OFFICE",
                null, null, null, null, null, null
        );

        assertNotNull(wordBytes, "Word document should be generated successfully");
        assertTrue(wordBytes.length > 0, "Generated Word document should not be empty");

        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(wordBytes))) {
            StringBuilder fullText = new StringBuilder();
            int pIdx = 0;
            for (XWPFParagraph p : doc.getParagraphs()) {
                pIdx++;
                String pText = p.getText();
                fullText.append("P[").append(pIdx).append("]: ").append(pText).append("\n");
            }
            String docText = fullText.toString();
            System.out.println("=== GENERATED EXAMINER VALUATION DOC TEXT ===");
            System.out.println(docText);

            assertTrue(docText.contains("Dr. NALLATHAMBI") || docText.contains("Dr NALLATHAMBI"), "Document should contain faculty name");
            assertTrue(docText.contains("ECE"), "Document should contain staff department ECE");
            assertTrue(docText.contains("Ref : KRCE/COE/VALUATION/ APRIL/MAY-2026/ ECE - 9"), "Ref line must contain ECE - 9");
            assertTrue(docText.contains("Board\t: ECE"), "Board line must be Board\\t: ECE");
            assertFalse(docText.contains("Board\t: ENGLISH"), "Board should NOT be ENGLISH");
            assertFalse(docText.contains("ENG - 9"), "Ref line should NOT contain ENG - 9");

            System.out.println("EXAMINER VALUATION TEST PASSED 100%! All ENGLISH / ENG references replaced with ECE!");
        }
    }
}

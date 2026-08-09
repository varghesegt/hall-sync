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

public class AuditValuationAppointmentTest {

    private final AppointmentOrderService service = new AppointmentOrderService(null);

    @Test
    public void testAuditValuationDepartmentAndBoardReplacement() throws Exception {
        Faculty faculty = new Faculty(
                UUID.randomUUID(),
                "Dr. KALPANA DEVI",
                "EMP101",
                "Department of ECE",
                "Associate Professor",
                "9443780910",
                "kalpana@krce.ac.in",
                "K. RAMAKRISHNAN COLLEGE OF ENGINEERING",
                true,
                true
        );

        byte[] wordBytes = service.generateAppointmentOrderWord(
                faculty,
                "Examiner for Audit Valuation",
                "APRIL/MAY-2026",
                LocalDate.of(2026, 8, 2),
                "CSE",
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
            System.out.println("=== GENERATED AUDIT VALUATION DOC TEXT ===");
            System.out.println(docText);

            assertTrue(docText.contains("Dr. KALPANA DEVI"), "Document should contain faculty name");
            assertTrue(docText.contains("Department of ECE") || docText.contains("ECE,"), "Document should contain staff department ECE");
            assertTrue(docText.contains("Ref : KRCE/COE/AUDIT VALUATION/ APRIL/MAY-2026/ ECE- 2"), "Ref line must contain ECE- 2");
            assertTrue(docText.contains("Board\t: ECE"), "Board line must be Board\\t: ECE");
            assertFalse(docText.contains("CSE"), "Entire document should contain ZERO occurrences of CSE!");

            System.out.println("TEST PASSED 100%! All CSE references successfully replaced with ECE!");
        }
    }
}

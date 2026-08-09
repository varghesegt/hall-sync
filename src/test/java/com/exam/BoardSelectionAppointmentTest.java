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

public class BoardSelectionAppointmentTest {

    private final AppointmentOrderService service = new AppointmentOrderService(null);

    @Test
    public void testPhysicsBoardSelectionForAuditValuation() throws Exception {
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
                LocalDate.of(2026, 8, 3),
                "PHYSICS", // Explicitly selected PHYSICS board!
                "9.00 a.m. - 05.00 p.m.",
                "COE OFFICE",
                null, null, null, null, null, null
        );

        assertNotNull(wordBytes);
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(wordBytes))) {
            StringBuilder fullText = new StringBuilder();
            for (XWPFParagraph p : doc.getParagraphs()) {
                fullText.append(p.getText()).append("\n");
            }
            String docText = fullText.toString();
            System.out.println("=== PHYSICS BOARD AUDIT VALUATION DOC ===");
            System.out.println(docText);

            assertTrue(docText.contains("Dr. KALPANA DEVI"));
            assertTrue(docText.contains("Department of ECE"));
            assertTrue(docText.contains("Board: PHYSICS") || docText.contains("Board\t: PHYSICS"));
            assertTrue(docText.contains("PHY- 2") || docText.contains("PHY-2") || docText.contains("PHY - 2"));
            System.out.println("PHYSICS board test passed!");
        }
    }

    @Test
    public void testEnglishBoardSelectionForValuation() throws Exception {
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
                LocalDate.of(2026, 8, 3),
                "ENGLISH", // Explicitly selected ENGLISH board!
                "9.00 a.m. - 05.00 p.m.",
                "COE OFFICE",
                null, null, null, null, null, null
        );

        assertNotNull(wordBytes);
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(wordBytes))) {
            StringBuilder fullText = new StringBuilder();
            for (XWPFParagraph p : doc.getParagraphs()) {
                fullText.append(p.getText()).append("\n");
            }
            String docText = fullText.toString();
            System.out.println("=== ENGLISH BOARD VALUATION DOC ===");
            System.out.println(docText);

            assertTrue(docText.contains("Dr. NALLATHAMBI") || docText.contains("Dr NALLATHAMBI"));
            assertTrue(docText.contains("ECE,") || docText.contains("Associate Professor, ECE."));
            assertTrue(docText.contains("Board\t: ENGLISH") || docText.contains("Board: ENGLISH"));
            assertTrue(docText.contains("ENG - 9"));
            System.out.println("ENGLISH board test passed!");
        }
    }
}

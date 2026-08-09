package com.exam;

import org.apache.poi.xwpf.usermodel.*;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

public class InspectDocxTemplateTest {

    @Test
    public void inspectTemplate() throws Exception {
        String path = "src/main/resources/templates/Lab_Claim_Template.docx";
        try (InputStream is = new FileInputStream(path);
             XWPFDocument doc = new XWPFDocument(is)) {

            System.out.println("=== PARAGRAPHS ===");
            for (int i = 0; i < doc.getParagraphs().size(); i++) {
                String text = doc.getParagraphs().get(i).getText();
                if (!text.trim().isEmpty()) {
                    System.out.println("P[" + i + "]: " + text);
                }
            }

            System.out.println("\n=== TABLES ===");
            List<XWPFTable> tables = doc.getTables();
            System.out.println("Total Tables: " + tables.size());

            for (int t = 0; t < tables.size(); t++) {
                XWPFTable table = tables.get(t);
                System.out.println("\n--- TABLE " + t + " (" + table.getRows().size() + " rows) ---");
                for (int r = 0; r < table.getRows().size(); r++) {
                    XWPFTableRow row = table.getRow(r);
                    StringBuilder sb = new StringBuilder();
                    sb.append("R[").append(r).append("]: ");
                    for (int c = 0; c < row.getTableCells().size(); c++) {
                        sb.append("[").append(row.getCell(c).getText().replace("\n", " ")).append("] ");
                    }
                    System.out.println(sb.toString());
                }
            }
        }
    }
}

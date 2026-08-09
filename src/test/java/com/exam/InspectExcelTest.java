package com.exam;

import org.apache.poi.ss.usermodel.*;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.io.FileInputStream;

public class InspectExcelTest {

    @Test
    public void inspectPracticalClaimExcels() {
        String[] paths = {
            "C:/Users/Asus/Downloads/06.06.2026_Practical Claim April May 2026.xlsx",
            "C:/Users/Asus/Downloads/Practical Claim Form (April_May-2026) (Responses).xlsx"
        };

        DataFormatter formatter = new DataFormatter();

        for (String path : paths) {
            System.out.println("==================================================");
            System.out.println("FILE: " + path);
            File f = new File(path);
            if (!f.exists()) {
                System.out.println("File does not exist!");
                continue;
            }

            try (FileInputStream fis = new FileInputStream(f);
                 Workbook wb = WorkbookFactory.create(fis)) {

                for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                    Sheet sheet = wb.getSheetAt(i);
                    System.out.println("--- Sheet " + i + ": " + sheet.getSheetName() + " ---");

                    for (int r = 0; r <= Math.min(15, sheet.getLastRowNum()); r++) {
                        Row row = sheet.getRow(r);
                        if (row == null) continue;

                        StringBuilder sb = new StringBuilder();
                        sb.append("Row ").append(r + 1).append(": ");
                        boolean hasContent = false;
                        for (int c = 0; c < Math.min(30, row.getLastCellNum()); c++) {
                            Cell cell = row.getCell(c);
                            String val = formatter.formatCellValue(cell);
                            if (val != null && !val.trim().isEmpty()) {
                                hasContent = true;
                            }
                            sb.append("[").append(val != null ? val.trim() : "").append("] ");
                        }
                        if (hasContent) {
                            System.out.println(sb.toString());
                        }
                    }
                }

            } catch (Exception e) {
                System.out.println("Error reading " + path + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}

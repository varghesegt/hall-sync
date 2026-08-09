package com.exam.claims.service;

import com.exam.claims.entity.LabClaimRecord;
import com.exam.claims.util.AmountToWordsConverter;
import org.apache.poi.xwpf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class LabWordGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(LabWordGeneratorService.class);

    private byte[] templateBytes;

    public LabWordGeneratorService() {
        try {
            ClassPathResource resource = new ClassPathResource("templates/Lab_Claim_Template.docx");
            if (resource.exists()) {
                try (InputStream is = resource.getInputStream()) {
                    this.templateBytes = is.readAllBytes();
                    log.info("Successfully loaded Lab_Claim_Template.docx into memory ({} bytes)", templateBytes.length);
                }
            } else {
                log.warn("Lab_Claim_Template.docx template not found in resources/templates.");
            }
        } catch (Exception e) {
            log.error("Error initializing LabWordGeneratorService template: {}", e.getMessage(), e);
        }
    }

    public byte[] generateLabClaimWord(LabClaimRecord record) throws IOException {
        return generateBatchWord(List.of(record));
    }

    public byte[] generateBatchWord(List<LabClaimRecord> records) throws IOException {
        Map<String, List<LabClaimRecord>> grouped = groupRecordsBySession(records);

        if (grouped.size() == 1) {
            // For a single session, populate and return the document template directly
            // This preserves ALL images, logos, headers, footers, relationships 100%!
            return populateSingleSessionTemplate(grouped.values().iterator().next());
        }

        ByteArrayOutputStream finalOut = new ByteArrayOutputStream();
        XWPFDocument finalDoc = new XWPFDocument();

        int sessionIndex = 0;
        for (Map.Entry<String, List<LabClaimRecord>> entry : grouped.entrySet()) {
            byte[] singlePageDocx = populateSingleSessionTemplate(entry.getValue());

            if (singlePageDocx != null) {
                try (InputStream is = new ByteArrayInputStream(singlePageDocx);
                     XWPFDocument sessionDoc = new XWPFDocument(is)) {

                    if (sessionIndex > 0) {
                        XWPFParagraph breakPara = finalDoc.createParagraph();
                        breakPara.setPageBreak(true);
                    }
                    copyDocumentContent(sessionDoc, finalDoc);
                }
            }
            sessionIndex++;
        }

        finalDoc.write(finalOut);
        finalDoc.close();
        return finalOut.toByteArray();
    }

    private byte[] populateSingleSessionTemplate(List<LabClaimRecord> sessionRecords) {
        if (templateBytes == null) {
            log.error("Cannot populate docx template: templateBytes is null.");
            return null;
        }

        try (InputStream is = new ByteArrayInputStream(templateBytes);
             XWPFDocument doc = new XWPFDocument(is)) {

            LabClaimRecord sample = sessionRecords.get(0);
            LabClaimRecord ext = findRole(sessionRecords, "EXTERNAL_EXAMINER");
            LabClaimRecord intRec = findRole(sessionRecords, "INTERNAL_EXAMINER");
            LabClaimRecord skilled = findRole(sessionRecords, "SKILLED_ASSISTANT");
            LabClaimRecord tech = findRole(sessionRecords, "LAB_ATTENDER", "TECHNICIAN", "SUPPORTING_STAFF");

            // 1. Cleanly update Paragraph 3 Season without text overlap
            if (doc.getParagraphs().size() > 3 && sample.getExamSeason() != null && !sample.getExamSeason().isBlank()) {
                XWPFParagraph p3 = doc.getParagraphs().get(3);
                for (int r = p3.getRuns().size() - 1; r >= 0; r--) {
                    p3.removeRun(r);
                }
                XWPFRun rNew = p3.createRun();
                rNew.setFontFamily("Times New Roman");
                rNew.setFontSize(11);
                rNew.setText("End Semester - Practical Examinations - " + sample.getExamSeason().trim());
            }

            List<XWPFTable> tables = doc.getTables();
            if (tables.size() >= 2) {
                // Table 0: Session Parameters Grid
                XWPFTable t0 = tables.get(0);
                String degree = sample.getSemester() != null && sample.getSemester().contains("M") ? "PG" : "UG";
                String examDateStr = sample.getExamDate() != null ? sample.getExamDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : "";
                String subCodeName = (sample.getSubjectCode() != null ? sample.getSubjectCode() : "") + " / " + (sample.getSubjectName() != null ? sample.getSubjectName() : "");
                String subType = sample.getSubjectName() != null && sample.getSubjectName().contains("TCPL") ? "TCPL" : "Practical";

                setCellContent(t0, 0, 0, "Degree : " + degree);
                setCellContent(t0, 0, 1, "Sub.Code/Name : " + subCodeName);
                setCellContent(t0, 0, 2, "No. of Registered : " + (sample.getRegisteredCount() != null ? sample.getRegisteredCount() : ""));
                setCellContent(t0, 0, 3, "Exam Date : " + examDateStr);

                setCellContent(t0, 1, 0, "Dept. of Candidate : " + getVal(sample.getDepartment()));
                setCellContent(t0, 1, 1, "Subject Type : " + subType);
                setCellContent(t0, 1, 2, "No. of Examined : " + (sample.getPresentCount() != null ? sample.getPresentCount() : ""));
                setCellContent(t0, 1, 3, "Session : " + getVal(sample.getSession()));

                setCellContent(t0, 2, 0, "Semester : " + getVal(sample.getSemester()));

                // Table 1: Main Matrix (18 Rows)
                XWPFTable t1 = tables.get(1);

                // Staff Metadata Rows (Rows 1 to 5)
                fillRow(t1, 1, getName(ext), getName(intRec), getName(skilled), getName(tech));
                fillRow(t1, 2, getDesig(ext), getDesig(intRec), getDesig(skilled), getDesig(tech));
                fillRow(t1, 3, getDept(ext), getDept(intRec), getDept(skilled), getDept(tech));
                fillRow(t1, 4, getCollege(ext), "KRCE", "KRCE", getCollege(tech));
                fillRow(t1, 5, getPhone(ext), getPhone(intRec), getPhone(skilled), getPhone(tech));

                // Claim Amounts (Rows 7 to 10)
                fillRow(t1, 7, formatClaim(ext), formatClaim(intRec), formatClaim(skilled), formatClaim(tech));
                fillRow(t1, 8, formatTaDa(ext), "---", "---", "---");
                fillRow(t1, 9, formatTotal(ext), formatTotal(intRec), formatTotal(skilled), formatTotal(tech));
                fillRow(t1, 10, formatWords(ext), formatWords(intRec), formatWords(skilled), formatWords(tech));

                // Bank Details (Rows 13 to 16)
                fillRow(t1, 13, getAcc(ext), getAcc(intRec), getAcc(skilled), getAcc(tech));
                fillRow(t1, 14, getBank(ext), getBank(intRec), getBank(skilled), getBank(tech));
                fillRow(t1, 15, getIfsc(ext), getIfsc(intRec), getIfsc(skilled), getIfsc(tech));
                fillRow(t1, 16, getBranch(ext), getBranch(intRec), getBranch(skilled), getBranch(tech));
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Error populating docx template for session: {}", e.getMessage(), e);
            return null;
        }
    }

    private void fillRow(XWPFTable table, int rowIdx, String c1, String c2, String c3, String c4) {
        if (rowIdx >= table.getRows().size()) return;
        XWPFTableRow row = table.getRow(rowIdx);
        if (row.getTableCells().size() >= 5) {
            setCellContent(row.getCell(1), c1);
            setCellContent(row.getCell(2), c2);
            setCellContent(row.getCell(3), c3);
            setCellContent(row.getCell(4), c4);
        }
    }

    private void setCellContent(XWPFTable table, int rowIdx, int colIdx, String text) {
        if (rowIdx < table.getRows().size()) {
            XWPFTableRow row = table.getRow(rowIdx);
            if (colIdx < row.getTableCells().size()) {
                setCellContent(row.getCell(colIdx), text);
            }
        }
    }

    private void setCellContent(XWPFTableCell cell, String text) {
        if (cell == null) return;
        if (!cell.getParagraphs().isEmpty()) {
            XWPFParagraph p = cell.getParagraphs().get(0);
            if (!p.getRuns().isEmpty()) {
                p.getRuns().get(0).setText(text != null ? text : "", 0);
                for (int i = p.getRuns().size() - 1; i > 0; i--) {
                    p.removeRun(i);
                }
            } else {
                XWPFRun r = p.createRun();
                r.setFontFamily("Times New Roman");
                r.setFontSize(9);
                r.setText(text != null ? text : "");
            }
        } else {
            XWPFParagraph p = cell.addParagraph();
            XWPFRun r = p.createRun();
            r.setFontFamily("Times New Roman");
            r.setFontSize(9);
            r.setText(text != null ? text : "");
        }
    }

    private Map<String, List<LabClaimRecord>> groupRecordsBySession(List<LabClaimRecord> records) {
        Map<String, List<LabClaimRecord>> map = new LinkedHashMap<>();
        for (LabClaimRecord r : records) {
            String dateKey = r.getExamDate() != null ? r.getExamDate().toString() : "NO_DATE";
            String codeKey = r.getSubjectCode() != null ? r.getSubjectCode().trim().toUpperCase() : "NO_CODE";
            String deptKey = r.getDepartment() != null ? r.getDepartment().trim().toUpperCase() : "NO_DEPT";
            String key = dateKey + "_" + codeKey + "_" + deptKey;
            map.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
        }
        return map;
    }

    private LabClaimRecord findRole(List<LabClaimRecord> list, String... roles) {
        Set<String> roleSet = new HashSet<>(Arrays.asList(roles));
        for (LabClaimRecord r : list) {
            if (r.getStaffRole() != null && roleSet.contains(r.getStaffRole().toUpperCase().trim())) {
                return r;
            }
        }
        return null;
    }

    private String getName(LabClaimRecord r) { return r != null && r.getStaffName() != null ? r.getStaffName() : "-NA-"; }
    private String getDesig(LabClaimRecord r) { return r != null && r.getDesignation() != null ? r.getDesignation() : "---"; }
    private String getDept(LabClaimRecord r) { return r != null && r.getDepartment() != null ? r.getDepartment() : "-NA-"; }
    private String getCollege(LabClaimRecord r) {
        if (r == null) return "-NA-";
        String name = r.getInstitutionName() != null ? r.getInstitutionName() : "KRCE";
        if (r.getDistanceKm() != null && r.getDistanceKm().compareTo(BigDecimal.ZERO) > 0) {
            name += " (<" + r.getDistanceKm() + " Km)";
        }
        return name;
    }
    private String getPhone(LabClaimRecord r) { return r != null && r.getMobileNo() != null ? r.getMobileNo() : "-NA-"; }
    private String getAcc(LabClaimRecord r) { return r != null && r.getBankAccountNumber() != null ? r.getBankAccountNumber() : "-NA-"; }
    private String getBank(LabClaimRecord r) { return r != null && r.getBankName() != null ? r.getBankName() : "-NA-"; }
    private String getIfsc(LabClaimRecord r) { return r != null && r.getIfscCode() != null ? r.getIfscCode() : "-NA-"; }
    private String getBranch(LabClaimRecord r) { return r != null && r.getBranch() != null ? r.getBranch() : "-NA-"; }

    private String formatClaim(LabClaimRecord r) {
        return r != null && r.getRemunerationAmount() != null ? "Rs. " + r.getRemunerationAmount().intValue() + " /-" : "Rs. -NA-/-";
    }

    private String formatTaDa(LabClaimRecord r) {
        if (r == null) return "---";
        String ta = r.getTaAmount() != null ? "TA : Rs. " + r.getTaAmount().intValue() + " /-" : "TA : ---";
        String da = r.getDaAmount() != null ? "DA : Rs. " + r.getDaAmount().intValue() + " /-" : "DA : ---";
        return ta + " ; " + da;
    }

    private String formatTotal(LabClaimRecord r) {
        return r != null && r.getTotalAmount() != null ? "Rs. " + r.getTotalAmount().intValue() + " /-" : "Rs. -NA-/-";
    }

    private String formatWords(LabClaimRecord r) {
        if (r == null || r.getTotalAmount() == null || r.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) return "";
        return AmountToWordsConverter.convert(r.getTotalAmount());
    }

    private String getVal(String v) { return v != null ? v : ""; }

    private void copyDocumentContent(XWPFDocument src, XWPFDocument target) {
        for (IBodyElement elem : src.getBodyElements()) {
            if (elem instanceof XWPFParagraph) {
                XWPFParagraph p = (XWPFParagraph) elem;
                XWPFParagraph newP = target.createParagraph();
                newP.getCTP().set(p.getCTP().copy());
            } else if (elem instanceof XWPFTable) {
                XWPFTable t = (XWPFTable) elem;
                XWPFTable newT = target.createTable();
                newT.getCTTbl().set(t.getCTTbl().copy());
            }
        }
    }
}

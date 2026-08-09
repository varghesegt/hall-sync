package com.exam.claims.service;

import com.exam.claims.entity.LabClaimRecord;
import com.exam.claims.util.AmountToWordsConverter;
import com.exam.claims.util.ClaimConstants;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class LabPdfGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(LabPdfGeneratorService.class);

    private static final Font FONT_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
    private static final Font FONT_SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.DARK_GRAY);
    private static final Font FONT_HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
    private static final Font FONT_BODY = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
    private static final Font FONT_BODY_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.BLACK);
    private static final Font FONT_SMALL = FontFactory.getFont(FontFactory.HELVETICA, 7, Color.GRAY);

    public byte[] generateLabClaimPdf(LabClaimRecord record) throws DocumentException {
        return generateBatchPdf(List.of(record));
    }

    public byte[] generateBatchPdf(List<LabClaimRecord> records) throws DocumentException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 20, 20, 20, 20);
        PdfWriter.getInstance(document, baos);

        document.open();

        for (int i = 0; i < records.size(); i++) {
            if (i > 0) document.newPage();
            addSingleRecordToDoc(document, records.get(i));
        }

        document.close();
        return baos.toByteArray();
    }

    private void addSingleRecordToDoc(Document doc, LabClaimRecord rec) throws DocumentException {
        // Header
        Paragraph p1 = new Paragraph(ClaimConstants.COLLEGE_NAME, FONT_TITLE);
        p1.setAlignment(Element.ALIGN_CENTER);
        doc.add(p1);

        Paragraph p2 = new Paragraph(ClaimConstants.COLLEGE_AUTONOMY + " - " + ClaimConstants.OFFICE_TITLE, FONT_SUBTITLE);
        p2.setAlignment(Element.ALIGN_CENTER);
        doc.add(p2);

        Paragraph p3 = new Paragraph("REMUNERATION BILL FOR PRACTICAL EXAMINATIONS", FONT_SUBTITLE);
        p3.setAlignment(Element.ALIGN_CENTER);
        p3.setSpacingAfter(10);
        doc.add(p3);

        // Staff Info Table
        PdfPTable infoTable = new PdfPTable(4);
        infoTable.setWidthPercentage(100);
        infoTable.setWidths(new float[]{20, 30, 20, 30});

        addInfoCell(infoTable, "Staff Name:", rec.getStaffName());
        addInfoCell(infoTable, "Staff Role:", rec.getStaffRole());
        addInfoCell(infoTable, "Designation:", rec.getDesignation() != null ? rec.getDesignation() : "N/A");
        addInfoCell(infoTable, "Institution:", rec.getInstitutionName() != null ? rec.getInstitutionName() : "Internal");
        addInfoCell(infoTable, "Exam Date:", rec.getExamDate() != null ? rec.getExamDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) : "N/A");
        addInfoCell(infoTable, "Session:", rec.getSession() != null ? rec.getSession() : "N/A");

        infoTable.setSpacingAfter(10);
        doc.add(infoTable);

        // Exam Details Table
        PdfPTable detailTable = new PdfPTable(6);
        detailTable.setWidthPercentage(100);
        detailTable.setWidths(new float[]{15, 15, 30, 15, 12, 13});

        addHeaderCell(detailTable, "Department");
        addHeaderCell(detailTable, "Subject Code");
        addHeaderCell(detailTable, "Subject Name");
        addHeaderCell(detailTable, "Batch No.");
        addHeaderCell(detailTable, "Present");
        addHeaderCell(detailTable, "Rem. (Rs)");

        addDataCell(detailTable, rec.getDepartment() != null ? rec.getDepartment() : "N/A");
        addDataCell(detailTable, rec.getSubjectCode() != null ? rec.getSubjectCode() : "N/A");
        addDataCell(detailTable, rec.getSubjectName() != null ? rec.getSubjectName() : "N/A");
        addDataCell(detailTable, rec.getBatchNumber() != null ? rec.getBatchNumber() : "Batch I");
        addDataCell(detailTable, String.valueOf(rec.getPresentCount() != null ? rec.getPresentCount() : 0));
        addDataCell(detailTable, "Rs. " + rec.getRemunerationAmount());

        detailTable.setSpacingAfter(10);
        doc.add(detailTable);

        // Summary Table
        PdfPTable sumTable = new PdfPTable(2);
        sumTable.setWidthPercentage(50);
        sumTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        sumTable.setWidths(new float[]{60, 40});

        addSummaryRow(sumTable, "Remuneration Amount:", "Rs. " + rec.getRemunerationAmount());
        if (rec.getTaAmount() != null && rec.getTaAmount().doubleValue() > 0) {
            addSummaryRow(sumTable, "Travelling Allowance (TA):", "Rs. " + rec.getTaAmount());
        }
        if (rec.getDaAmount() != null && rec.getDaAmount().doubleValue() > 0) {
            addSummaryRow(sumTable, "Dearness Allowance (DA):", "Rs. " + rec.getDaAmount());
        }
        addSummaryRowBold(sumTable, "TOTAL CLAIM AMOUNT:", "Rs. " + rec.getTotalAmount());

        sumTable.setSpacingAfter(10);
        doc.add(sumTable);

        // Amount in Words
        String words = AmountToWordsConverter.convert(rec.getTotalAmount());
        Paragraph pWords = new Paragraph("Amount in Words: " + words, FONT_BODY_BOLD);
        pWords.setSpacingAfter(15);
        doc.add(pWords);

        // Bank Details
        Paragraph pBank = new Paragraph("Bank Details for Payment:", FONT_SUBTITLE);
        doc.add(pBank);

        PdfPTable bankTable = new PdfPTable(4);
        bankTable.setWidthPercentage(100);
        bankTable.setWidths(new float[]{25, 25, 25, 25});

        addInfoCell(bankTable, "A/C No:", rec.getBankAccountNumber() != null ? rec.getBankAccountNumber() : "N/A");
        addInfoCell(bankTable, "IFSC:", rec.getIfscCode() != null ? rec.getIfscCode() : "N/A");
        addInfoCell(bankTable, "Bank Name:", rec.getBankName() != null ? rec.getBankName() : "N/A");
        addInfoCell(bankTable, "Branch:", rec.getBranch() != null ? rec.getBranch() : "N/A");
        bankTable.setSpacingAfter(30);
        doc.add(bankTable);

        // Signatures
        PdfPTable sigTable = new PdfPTable(3);
        sigTable.setWidthPercentage(100);

        PdfPCell c1 = new PdfPCell(new Phrase("Signature of Claimant", FONT_BODY));
        c1.setBorder(Rectangle.NO_BORDER);
        c1.setHorizontalAlignment(Element.ALIGN_LEFT);

        PdfPCell c2 = new PdfPCell(new Phrase("HOD Signature", FONT_BODY));
        c2.setBorder(Rectangle.NO_BORDER);
        c2.setHorizontalAlignment(Element.ALIGN_CENTER);

        PdfPCell c3 = new PdfPCell(new Phrase("Controller of Examinations", FONT_BODY));
        c3.setBorder(Rectangle.NO_BORDER);
        c3.setHorizontalAlignment(Element.ALIGN_RIGHT);

        sigTable.addCell(c1);
        sigTable.addCell(c2);
        sigTable.addCell(c3);
        doc.add(sigTable);
    }

    private void addInfoCell(PdfPTable table, String label, String val) {
        PdfPCell lCell = new PdfPCell(new Phrase(label, FONT_BODY_BOLD));
        lCell.setBorder(Rectangle.NO_BORDER);
        table.addCell(lCell);

        PdfPCell vCell = new PdfPCell(new Phrase(val, FONT_BODY));
        vCell.setBorder(Rectangle.NO_BORDER);
        table.addCell(vCell);
    }

    private void addHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_HEADER));
        cell.setBackgroundColor(new Color(40, 50, 80));
        cell.setPadding(4);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addDataCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_BODY));
        cell.setPadding(4);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addSummaryRow(PdfPTable table, String label, String val) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, FONT_BODY));
        c1.setBorder(Rectangle.NO_BORDER);
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase(val, FONT_BODY));
        c2.setBorder(Rectangle.NO_BORDER);
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(c2);
    }

    private void addSummaryRowBold(PdfPTable table, String label, String val) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, FONT_BODY_BOLD));
        c1.setBorder(Rectangle.BOTTOM);
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase(val, FONT_BODY_BOLD));
        c2.setBorder(Rectangle.BOTTOM);
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(c2);
    }
}

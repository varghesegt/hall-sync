package com.exam.claims.service;

import com.exam.claims.entity.ClaimRecord;
import com.exam.claims.entity.ScriptDetail;
import com.exam.claims.util.AmountToWordsConverter;
import com.exam.claims.util.ClaimConstants;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generates PDF claim forms matching the exact layout from the provided templates.
 * Supports 3 formats: EXAMINER, ASSISTANT EXAMINER, CHIEF EXAMINER.
 */
@Service
public class PdfGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(PdfGeneratorService.class);

    // Fonts
    private static final Font FONT_TITLE = new Font(Font.TIMES_ROMAN, 22, Font.BOLD);
    private static final Font FONT_SUBTITLE = new Font(Font.TIMES_ROMAN, 14, Font.BOLD);
    private static final Font FONT_AUTONOMY = new Font(Font.TIMES_ROMAN, 11, Font.NORMAL);
    private static final Font FONT_HEADER = new Font(Font.TIMES_ROMAN, 10, Font.BOLD);
    private static final Font FONT_NORMAL = new Font(Font.TIMES_ROMAN, 10, Font.NORMAL);
    private static final Font FONT_SMALL = new Font(Font.TIMES_ROMAN, 10, Font.NORMAL);
    private static final Font FONT_BOLD = new Font(Font.TIMES_ROMAN, 10, Font.BOLD);
    private static final Font FONT_ITALIC = new Font(Font.TIMES_ROMAN, 10, Font.ITALIC);
    private static final Font FONT_BOLD_SMALL = new Font(Font.TIMES_ROMAN, 10, Font.BOLD);
    private static final Font FONT_LARGE_BOLD = new Font(Font.TIMES_ROMAN, 12, Font.BOLD);

    private static final Font FONT_BANK_HEADER = new Font(Font.TIMES_ROMAN, 14, Font.BOLD);
    private static final Font FONT_BANK_NORMAL = new Font(Font.TIMES_ROMAN, 14, Font.NORMAL);
    private static final Font FONT_CHIEF_SIG = new Font(Font.TIMES_ROMAN, 10, Font.BOLD);

    private static final Color BORDER_COLOR = new Color(0, 0, 0);

    private final com.exam.repository.master.TenantRepository tenantRepository;

    public PdfGeneratorService(com.exam.repository.master.TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    /**
     * Generate a multi-page PDF for all claims in a batch.
     */
    public byte[] generateBatchPdf(List<ClaimRecord> records) throws DocumentException, IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 20, 20, 15, 15);
        PdfWriter.getInstance(document, baos);
        document.open();

        for (int i = 0; i < records.size(); i++) {
            if (i > 0) document.newPage();
            generateClaimPage(document, records.get(i));
        }

        document.close();
        return baos.toByteArray();
    }

    /**
     * Generate a single claim PDF.
     */
    public byte[] generateSinglePdf(ClaimRecord record) throws DocumentException, IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 20, 20, 15, 15);
        PdfWriter.getInstance(document, baos);
        document.open();
        generateClaimPage(document, record);
        document.close();
        return baos.toByteArray();
    }

    /**
     * Generate a single claim page based on post held type.
     */
    private void generateClaimPage(Document document, ClaimRecord record) throws DocumentException {
        // Header
        addHeader(document, record);

        // Personal Info Table
        addPersonalInfoTable(document, record);

        // Body based on post type
        if (record.isAssistantExaminer()) {
            addAssistantExaminerBody(document, record);
        } else if (record.isChiefExaminer()) {
            addChiefExaminerBody(document, record);
        } else {
            addExaminerBody(document, record);
        }

        // Bank Details Footer
        addBankDetails(document, record);
    }

    // ==================== HEADER ====================

    private void addHeader(Document document, ClaimRecord record) throws DocumentException {
        // Load the logo image
        Image logoImg = null;
        try {
            byte[] logoBytes = null;
            String tenantId = com.exam.config.tenant.TenantContext.getCurrentTenant();
            if (tenantId != null) {
                com.exam.config.tenant.TenantContext.clear();
                try {
                    java.util.Optional<com.exam.entity.master.Tenant> tenantOpt = tenantRepository.findByTenantId(tenantId);
                    if (tenantOpt.isPresent() && tenantOpt.get().getLogoBase64() != null && !tenantOpt.get().getLogoBase64().isEmpty()) {
                        String base64 = tenantOpt.get().getLogoBase64();
                        String[] parts = base64.split(",");
                        String imageString = parts.length > 1 ? parts[1] : parts[0];
                        logoBytes = java.util.Base64.getDecoder().decode(imageString);
                    }
                } finally {
                    com.exam.config.tenant.TenantContext.setCurrentTenant(tenantId);
                }
            }
            
            if (logoBytes == null) {
                var resourceStream = getClass().getResourceAsStream("/krce_logo.jpg");
                if (resourceStream != null) {
                    logoBytes = resourceStream.readAllBytes();
                }
            }

            if (logoBytes != null) {
                logoImg = Image.getInstance(logoBytes);
                logoImg.scaleToFit(80, 80);
            }
        } catch (Exception e) {
            log.error("Could not load college logo image", e);
        }

        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{14, 86});
        headerTable.setSpacingAfter(1);

        // Column 1: Logo
        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(0);
        logoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        if (logoImg != null) {
            logoCell.addElement(logoImg);
        }
        headerTable.addCell(logoCell);

        // Column 2: Text block with no vertical divider line
        PdfPCell textCell = new PdfPCell();
        textCell.setBorder(0);
        textCell.setPaddingLeft(4f);
        textCell.setPaddingTop(0);
        textCell.setPaddingBottom(0);
        textCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        Paragraph office = new Paragraph(ClaimConstants.OFFICE_TITLE, FONT_TITLE);
        office.setAlignment(Element.ALIGN_CENTER);
        office.setSpacingAfter(1);
        textCell.addElement(office);

        Paragraph college = new Paragraph(ClaimConstants.COLLEGE_NAME, FONT_SUBTITLE);
        college.setAlignment(Element.ALIGN_CENTER);
        college.setSpacingAfter(1);
        textCell.addElement(college);

        Paragraph autonomy = new Paragraph(ClaimConstants.COLLEGE_AUTONOMY, FONT_AUTONOMY);
        autonomy.setAlignment(Element.ALIGN_CENTER);
        autonomy.setSpacingAfter(1);
        textCell.addElement(autonomy);

        Paragraph exam = new Paragraph(ClaimConstants.getExamTitle(record.getExamSeason()), FONT_LARGE_BOLD);
        exam.setAlignment(Element.ALIGN_CENTER);
        exam.setSpacingAfter(1);
        textCell.addElement(exam);

        headerTable.addCell(textCell);
        document.add(headerTable);

        // Date of Valuation and Sessions line
        String dateStr = record.getValuationDate() != null
            ? record.getValuationDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
            : "N/A";
        String sessions = record.getSessionsAttended() != null ? record.getSessionsAttended() : "N/A";

        PdfPTable dateTable = new PdfPTable(2);
        dateTable.setWidthPercentage(100);
        dateTable.setWidths(new float[]{50, 50});
        dateTable.setSpacingBefore(1);
        dateTable.setSpacingAfter(2);

        PdfPCell dateCell = new PdfPCell(new Phrase(
            "Date of Valuation(dd-mm-yyyy) : " + dateStr, FONT_BOLD));
        dateCell.setBorder(0);
        dateCell.setHorizontalAlignment(Element.ALIGN_LEFT);

        PdfPCell sessionCell = new PdfPCell(new Phrase(
            "No. of Sessions : " + sessions, FONT_BOLD));
        sessionCell.setBorder(0);
        sessionCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        dateTable.addCell(dateCell);
        dateTable.addCell(sessionCell);
        document.add(dateTable);
    }

    // ==================== PERSONAL INFO TABLE ====================

    private void addPersonalInfoTable(Document document, ClaimRecord record) throws DocumentException {
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{14, 20, 14, 14, 24, 8});
        table.setSpacingBefore(2);
        table.setSpacingAfter(2);

        // Header row
        addHeaderCell(table, "Post Held");
        addHeaderCell(table, "Name");
        addHeaderCell(table, "Mobile Number");
        addHeaderCell(table, "Designation");
        addHeaderCell(table, "Name of Institution");
        addHeaderCell(table, "Board");

        // Data row
        addDataCell(table, record.getPostHeld());
        addDataCell(table, record.getFullName());
        addDataCell(table, record.getMobileNo());
        addDataCell(table, record.getDesignation());
        addDataCell(table, record.getInstitutionName());
        addDataCell(table, record.getBoardName());

        document.add(table);
    }

    // ==================== EXAMINER BODY ====================

    private void addExaminerBody(Document document, ClaimRecord record) throws DocumentException {
        // Main body table: left = subjects, right = amounts
        PdfPTable bodyTable = new PdfPTable(2);
        bodyTable.setWidthPercentage(100);
        bodyTable.setWidths(new float[]{50, 50});
        bodyTable.setSpacingAfter(2);

        // LEFT SIDE: Subject Details
        PdfPCell leftCell = new PdfPCell();
        leftCell.setPadding(2);
        leftCell.setBorderColor(BORDER_COLOR);
        leftCell.setBorder(Rectangle.LEFT | Rectangle.RIGHT | Rectangle.TOP);

        // Title
        Paragraph subTitle = new Paragraph("Detail of Subjects Valued (Issue Reg. Pg.No: "
            + (record.getIssueRegPageNo() != null ? record.getIssueRegPageNo() : "N/A") + ")", FONT_SUBTITLE);
        subTitle.setAlignment(Element.ALIGN_CENTER);
        subTitle.setSpacingAfter(1);
        leftCell.addElement(subTitle);

        // FN and AN tables side by side
        PdfPTable fnAnTable = new PdfPTable(2);
        fnAnTable.setWidthPercentage(100);
        fnAnTable.setWidths(new float[]{50, 50});

        // FN Column
        PdfPCell fnCol = new PdfPCell();
        fnCol.setBorder(0);
        fnCol.setPadding(1);

        PdfPTable fnTable = createScriptTable(record, "FN");
        fnCol.addElement(fnTable);
        fnAnTable.addCell(fnCol);

        // AN Column
        PdfPCell anCol = new PdfPCell();
        anCol.setBorder(0);
        anCol.setPadding(1);

        PdfPTable anTable = createScriptTable(record, "AN");
        anCol.addElement(anTable);
        fnAnTable.addCell(anCol);

        leftCell.addElement(fnAnTable);
        bodyTable.addCell(leftCell);

        // RIGHT SIDE: Claim Amount Details
        PdfPCell rightCell = new PdfPCell();
        rightCell.setPadding(2);
        rightCell.setBorderColor(BORDER_COLOR);
        rightCell.setBorder(Rectangle.LEFT | Rectangle.RIGHT | Rectangle.TOP);

        Paragraph amtTitle = new Paragraph("Detail of Claim Amount", FONT_SUBTITLE);
        amtTitle.setAlignment(Element.ALIGN_CENTER);
        amtTitle.setSpacingAfter(2);
        rightCell.addElement(amtTitle);

        // Amount rows
        PdfPTable amtTable = new PdfPTable(3);
        amtTable.setWidthPercentage(100);
        amtTable.setWidths(new float[]{40, 30, 30});

        // Script Amount
        addAmountRow(amtTable, "Script Amount (Rs.)\n(" + record.getTotalScripts() + " Scripts )",
            "UG and PG : 30/script", formatAmount(record.getScriptAmount()));

        // TA
        BigDecimal distanceKm = record.getDistanceKm() != null ? record.getDistanceKm() : BigDecimal.ZERO;
        BigDecimal roundTrip = distanceKm.multiply(new BigDecimal("2"));
        String taDesc = "Rs. 8/Km  (To and Fro)\nRs. 150 (Up to 35 Km)";
        addAmountRow(amtTable, "Travelling Allowance (Rs.)\n(" + roundTrip.intValue() + " Km)",
            taDesc, formatAmount(record.getTravellingAllowance()));

        // DA
        String daDesc = "Rs.300/Day\nRs. 250/Session";
        addAmountRow(amtTable, "Dearness Allowance (Rs.)\n(Session: " + record.getSessionsAttended()
            + "; Int./Ext. : " + (record.getFacultyType() != null ? record.getFacultyType() : "N/A") + ")",
            daDesc, formatAmount(record.getDearnessAllowance()));

        // Total
        addTotalRow(amtTable, "Total Amount (Rs.)", formatAmount(record.getTotalAmount()));

        rightCell.addElement(amtTable);

        // Received line
        String receivedLine = AmountToWordsConverter.formatReceivedLine(record.getTotalAmount());
        Paragraph received = new Paragraph(receivedLine, FONT_AUTONOMY);
        received.setSpacingBefore(6);
        received.setSpacingAfter(6);
        rightCell.addElement(received);

        bodyTable.addCell(rightCell);

        // ROW 2: Signatures
        PdfPCell leftCellSig = new PdfPCell();
        leftCellSig.setBorderColor(BORDER_COLOR);
        leftCellSig.setBorder(Rectangle.LEFT | Rectangle.RIGHT | Rectangle.BOTTOM);

        PdfPCell rightCellSig = new PdfPCell();
        rightCellSig.setBorderColor(BORDER_COLOR);
        rightCellSig.setBorder(Rectangle.LEFT | Rectangle.RIGHT | Rectangle.BOTTOM);

        // Signature line with triple space for signing
        Paragraph sig = new Paragraph("Signature of the Examiner", new Font(Font.TIMES_ROMAN, 11, Font.ITALIC));
        sig.setAlignment(Element.ALIGN_CENTER);
        sig.setSpacingBefore(50);
        sig.setSpacingAfter(2);
        rightCellSig.addElement(sig);

        // Revenue stamp note
        Paragraph stampNote = new Paragraph(
            "(Re. 1/-Revenue Stamp to be affixed if the claim above Rs. 5000/-)", new Font(Font.TIMES_ROMAN, 10, Font.ITALIC));
        stampNote.setAlignment(Element.ALIGN_CENTER);
        rightCellSig.addElement(stampNote);

        bodyTable.addCell(leftCellSig);
        bodyTable.addCell(rightCellSig);

        document.add(bodyTable);
    }

    // ==================== CHIEF EXAMINER BODY ====================

    private void addChiefExaminerBody(Document document, ClaimRecord record) throws DocumentException {
        PdfPTable bodyTable = new PdfPTable(2);
        bodyTable.setWidthPercentage(100);
        bodyTable.setWidths(new float[]{50, 50});
        bodyTable.setSpacingAfter(2);

        // LEFT SIDE: Script Amount detail
        PdfPCell leftCell = new PdfPCell();
        leftCell.setPadding(4);
        leftCell.setBorderColor(BORDER_COLOR);
        leftCell.setBorder(Rectangle.LEFT | Rectangle.RIGHT | Rectangle.TOP);

        Paragraph scriptTitle = new Paragraph("Script Amount detail", FONT_SUBTITLE);
        scriptTitle.setAlignment(Element.ALIGN_CENTER);
        scriptTitle.setSpacingAfter(6);
        leftCell.addElement(scriptTitle);

        Paragraph maxScripts = new Paragraph("Max. Scripts Valued                    : " + record.getMaxScriptsValued(), FONT_NORMAL);
        maxScripts.setSpacingAfter(4);
        leftCell.addElement(maxScripts);
        
        Paragraph maxScriptsAmt = new Paragraph("Amount for Max. Scripts Valued    : Rs." + formatAmount(record.getMaxScriptsAmount()), FONT_NORMAL);
        maxScriptsAmt.setSpacingAfter(4);
        leftCell.addElement(maxScriptsAmt);
        
        Paragraph tenPercent = new Paragraph("10% of the amount for Max. Scripts Valued  : Rs." + formatAmount(record.getTenPercentAmount()), FONT_NORMAL);
        leftCell.addElement(tenPercent);

        bodyTable.addCell(leftCell);

        // RIGHT SIDE: Claim Amount Details
        PdfPCell rightCell = new PdfPCell();
        rightCell.setPadding(2);
        rightCell.setBorderColor(BORDER_COLOR);
        rightCell.setBorder(Rectangle.LEFT | Rectangle.RIGHT | Rectangle.TOP);

        Paragraph amtTitle = new Paragraph("Detail of Claim Amount", FONT_SUBTITLE);
        amtTitle.setAlignment(Element.ALIGN_CENTER);
        amtTitle.setSpacingAfter(2);
        rightCell.addElement(amtTitle);

        PdfPTable amtTable = new PdfPTable(2);
        amtTable.setWidthPercentage(100);
        amtTable.setWidths(new float[]{60, 40});

        // Overall Script Amount
        addSimpleAmountRow(amtTable, "Overall Script amount (Rs.)", formatAmount(record.getOverallScriptAmount()));

        // TA
        BigDecimal distanceKm = record.getDistanceKm() != null ? record.getDistanceKm() : BigDecimal.ZERO;
        BigDecimal roundTrip = distanceKm.multiply(new BigDecimal("2"));
        PdfPCell taLabelCell = new PdfPCell();
        taLabelCell.setBorder(0);
        taLabelCell.setPaddingBottom(2);
        Paragraph taLabel = new Paragraph("Travelling Allowance (Rs.)\n(" + roundTrip.intValue() + "Km)", FONT_NORMAL);
        Paragraph taRate = new Paragraph("Rs. 8/Km  (To and Fro)\nRs. 150 (Up to 35 Km)", FONT_ITALIC);
        taLabelCell.addElement(taLabel);
        taLabelCell.addElement(taRate);
        amtTable.addCell(taLabelCell);

        PdfPCell taAmtCell = new PdfPCell(new Phrase(formatAmount(record.getTravellingAllowance()), FONT_BOLD));
        taAmtCell.setBorder(0);
        taAmtCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        taAmtCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        amtTable.addCell(taAmtCell);

        // DA
        PdfPCell daLabelCell = new PdfPCell();
        daLabelCell.setBorder(0);
        daLabelCell.setPaddingBottom(2);
        Paragraph daLabel = new Paragraph("Dearness Allowance (Rs.)\n(Session: " + record.getSessionsAttended()
            + " ; Int./Ext. : " + (record.getFacultyType() != null ? record.getFacultyType() : "N/A") + ")", FONT_NORMAL);
        Paragraph daRate = new Paragraph("Rs.300/Day\nRs. 250/Session", FONT_ITALIC);
        daLabelCell.addElement(daLabel);
        daLabelCell.addElement(daRate);
        amtTable.addCell(daLabelCell);

        PdfPCell daAmtCell = new PdfPCell(new Phrase(formatAmount(record.getDearnessAllowance()), FONT_BOLD));
        daAmtCell.setBorder(0);
        daAmtCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        daAmtCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        amtTable.addCell(daAmtCell);

        // Total
        PdfPCell totalLabel = new PdfPCell(new Phrase("Total Amount (Rs.)", FONT_BOLD));
        totalLabel.setBorder(0);
        totalLabel.setBorderWidthTop(1);
        totalLabel.setPaddingTop(2);
        amtTable.addCell(totalLabel);

        PdfPCell totalAmt = new PdfPCell(new Phrase(formatAmount(record.getTotalAmount()), FONT_LARGE_BOLD));
        totalAmt.setBorder(0);
        totalAmt.setBorderWidthTop(1);
        totalAmt.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalAmt.setPaddingTop(2);
        amtTable.addCell(totalAmt);

        rightCell.addElement(amtTable);

        // Received line
        String receivedLine = AmountToWordsConverter.formatReceivedLine(record.getTotalAmount());
        Paragraph received = new Paragraph(receivedLine, FONT_AUTONOMY);
        received.setSpacingBefore(6);
        received.setSpacingAfter(6);
        rightCell.addElement(received);

        bodyTable.addCell(rightCell);

        // ROW 2: Signatures
        PdfPCell leftCellSig = new PdfPCell();
        leftCellSig.setBorderColor(BORDER_COLOR);
        leftCellSig.setBorder(Rectangle.LEFT | Rectangle.RIGHT | Rectangle.BOTTOM);

        PdfPCell rightCellSig = new PdfPCell();
        rightCellSig.setBorderColor(BORDER_COLOR);
        rightCellSig.setBorder(Rectangle.LEFT | Rectangle.RIGHT | Rectangle.BOTTOM);

        // Signature with triple space to sign properly
        Paragraph sig = new Paragraph("Signature of the Chief Examiner", new Font(Font.TIMES_ROMAN, 11, Font.ITALIC));
        sig.setAlignment(Element.ALIGN_CENTER);
        sig.setSpacingBefore(50);
        sig.setSpacingAfter(2);
        rightCellSig.addElement(sig);

        // Stamp details
        Paragraph stampNote = new Paragraph(
            "(Re. 1/-Revenue Stamp to be affixed if the claim above Rs. 5000/-)", new Font(Font.TIMES_ROMAN, 10, Font.ITALIC));
        stampNote.setAlignment(Element.ALIGN_CENTER);
        rightCellSig.addElement(stampNote);

        bodyTable.addCell(leftCellSig);
        bodyTable.addCell(rightCellSig);

        document.add(bodyTable);
    }

    // ==================== ASSISTANT EXAMINER BODY ====================

    private void addAssistantExaminerBody(Document document, ClaimRecord record) throws DocumentException {
        PdfPTable bodyTable = new PdfPTable(2);
        bodyTable.setWidthPercentage(100);
        bodyTable.setWidths(new float[]{50, 50});
        bodyTable.setSpacingAfter(2);

        // LEFT SIDE: Assistant Description
        PdfPCell leftCell = new PdfPCell();
        leftCell.setPadding(4);
        leftCell.setBorderColor(BORDER_COLOR);
        leftCell.setBorder(Rectangle.LEFT | Rectangle.RIGHT | Rectangle.TOP);
        leftCell.setMinimumHeight(80);

        Paragraph title = new Paragraph("Assistant Examiner Claim", FONT_SUBTITLE);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(10);
        leftCell.addElement(title);

        Paragraph sessParagraph = new Paragraph(
            "Sessions Attended : " + (record.getSessionsAttended() != null ? record.getSessionsAttended() : "N/A"),
            FONT_NORMAL);
        sessParagraph.setAlignment(Element.ALIGN_CENTER);
        leftCell.addElement(sessParagraph);

        bodyTable.addCell(leftCell);

        // RIGHT SIDE: Claim Amount (Structured Table)
        PdfPCell rightCell = new PdfPCell();
        rightCell.setPadding(4);
        rightCell.setBorderColor(BORDER_COLOR);
        rightCell.setBorder(Rectangle.LEFT | Rectangle.RIGHT | Rectangle.TOP);
        rightCell.setMinimumHeight(80);

        Paragraph amtTitle = new Paragraph("Detail of Claim Amount", FONT_SUBTITLE);
        amtTitle.setAlignment(Element.ALIGN_CENTER);
        amtTitle.setSpacingAfter(8);
        rightCell.addElement(amtTitle);

        PdfPTable amtTable = new PdfPTable(3);
        amtTable.setWidthPercentage(100);
        amtTable.setWidths(new float[]{40, 30, 30});

        // Base Remuneration
        addAmountRow(amtTable, "Base Remuneration (Rs.)\n(Session: " + (record.getSessionsAttended() != null ? record.getSessionsAttended() : "N/A") + ")",
            "Rs. 550/Full Day\nRs. 275/Half Day", formatAmount(record.getScriptAmount()));

        // TA
        addAmountRow(amtTable, "Travelling Allowance (Rs.)",
            "Rs. 0 (Internal)", formatAmount(record.getTravellingAllowance()));

        // DA
        boolean isHoliday = record.isGovernmentHoliday();
        String daDetails = "Holiday: Rs.300/250\nWorking Day: Rs.0";
        addAmountRow(amtTable, "Dearness Allowance (Rs.)\n(Holiday/Sunday: " + (isHoliday ? "YES" : "NO") + ")",
            daDetails, formatAmount(record.getDearnessAllowance()));

        // Total
        addTotalRow(amtTable, "Total Amount (Rs.)", formatAmount(record.getTotalAmount()));

        rightCell.addElement(amtTable);

        // Received line
        String receivedLine = AmountToWordsConverter.formatReceivedLine(record.getTotalAmount());
        Paragraph received = new Paragraph(receivedLine, FONT_AUTONOMY);
        received.setSpacingBefore(6);
        received.setSpacingAfter(6);
        rightCell.addElement(received);

        bodyTable.addCell(rightCell);

        // ROW 2: Signatures
        PdfPCell leftCellSig = new PdfPCell();
        leftCellSig.setBorderColor(BORDER_COLOR);
        leftCellSig.setBorder(Rectangle.LEFT | Rectangle.RIGHT | Rectangle.BOTTOM);

        PdfPCell rightCellSig = new PdfPCell();
        rightCellSig.setBorderColor(BORDER_COLOR);
        rightCellSig.setBorder(Rectangle.LEFT | Rectangle.RIGHT | Rectangle.BOTTOM);

        // Signature with triple space to sign properly
        Paragraph sig = new Paragraph("Signature of the Assistant Examiner", new Font(Font.TIMES_ROMAN, 11, Font.ITALIC));
        sig.setAlignment(Element.ALIGN_CENTER);
        sig.setSpacingBefore(50);
        sig.setSpacingAfter(2);
        rightCellSig.addElement(sig);

        // Stamp details
        Paragraph stampNote = new Paragraph(
            "(Re. 1/-Revenue Stamp to be affixed if the claim above Rs. 5000/-)", new Font(Font.TIMES_ROMAN, 10, Font.ITALIC));
        stampNote.setAlignment(Element.ALIGN_CENTER);
        rightCellSig.addElement(stampNote);

        bodyTable.addCell(leftCellSig);
        bodyTable.addCell(rightCellSig);

        document.add(bodyTable);
    }

    // ==================== BANK DETAILS ====================

    private void addBankDetails(Document document, ClaimRecord record) throws DocumentException {
        PdfPTable bankTable = new PdfPTable(2);
        bankTable.setWidthPercentage(100);
        bankTable.setWidths(new float[]{70, 30});
        bankTable.setSpacingBefore(2);

        // Left side: grid of bank details
        PdfPTable detailsTable = new PdfPTable(2);
        detailsTable.setWidthPercentage(100);
        detailsTable.setWidths(new float[]{40, 60});

        // Row 1: Account Number
        addBankGridCell(detailsTable, "ACCOUNT NUMBER :", FONT_BANK_HEADER, Element.ALIGN_CENTER, true);
        addBankGridCell(detailsTable, record.getBankAccountNumber() != null ? "Account Number : " + record.getBankAccountNumber() : "N/A", FONT_BANK_NORMAL, Element.ALIGN_CENTER, false);

        // Row 2: IFSC Code
        addBankGridCell(detailsTable, "IFSC CODE :", FONT_BANK_HEADER, Element.ALIGN_CENTER, true);
        addBankGridCell(detailsTable, record.getIfscCode() != null ? record.getIfscCode() : "N/A", FONT_BANK_NORMAL, Element.ALIGN_CENTER, false);

        // Row 3: Bank Name
        addBankGridCell(detailsTable, "BANK NAME :", FONT_BANK_HEADER, Element.ALIGN_CENTER, true);
        addBankGridCell(detailsTable, record.getBankName() != null ? record.getBankName() : "N/A", FONT_BANK_NORMAL, Element.ALIGN_CENTER, false);

        // Row 4: Branch
        addBankGridCell(detailsTable, "BRANCH :", FONT_BANK_HEADER, Element.ALIGN_CENTER, true);
        addBankGridCell(detailsTable, record.getBranch() != null ? record.getBranch() : "N/A", FONT_BANK_NORMAL, Element.ALIGN_CENTER, false);

        PdfPCell leftCell = new PdfPCell(detailsTable);
        leftCell.setPadding(0);
        leftCell.setBorder(0);
        bankTable.addCell(leftCell);

        // Right side: Signature (Chief Examiner is standard across layouts)
        String sigText = "Signature of the Chief Examiner";

        PdfPCell rightCell = new PdfPCell(new Phrase(sigText, FONT_CHIEF_SIG));
        rightCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        rightCell.setVerticalAlignment(Element.ALIGN_BOTTOM);
        rightCell.setPaddingBottom(3);
        bankTable.addCell(rightCell);

        document.add(bankTable);
    }

    // ==================== HELPER METHODS ====================

    private PdfPTable createScriptTable(ClaimRecord record, String sessionType) {
        PdfPTable table = new PdfPTable(3);
        try {
            table.setWidths(new float[]{15, 40, 30});
        } catch (DocumentException e) {
            // ignore
        }
        table.setWidthPercentage(100);

        // Row 0: Merged cell for FN/AN
        PdfPCell sessionCell = new PdfPCell(new Phrase(sessionType, FONT_BOLD));
        sessionCell.setColspan(3);
        sessionCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        sessionCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        sessionCell.setPadding(2f);
        table.addCell(sessionCell);

        // Row 1: Headers
        addSmallHeaderCell(table, "S.N.");
        addSmallHeaderCell(table, "Subject Code");
        addSmallHeaderCell(table, "No. of Scripts");

        // Filter details by session type
        List<ScriptDetail> details = record.getScriptDetails().stream()
            .filter(d -> sessionType.equalsIgnoreCase(d.getSessionType()))
            .collect(Collectors.toList());

        // Show up to 10 rows
        for (int i = 0; i < 10; i++) {
            String sn = String.valueOf(i + 1);
            String code = "";
            String scripts = "";

            if (i < details.size()) {
                code = details.get(i).getSubjectCode() != null ? details.get(i).getSubjectCode() : "";
                scripts = details.get(i).getNoOfScripts() != null && details.get(i).getNoOfScripts() > 0
                    ? String.valueOf(details.get(i).getNoOfScripts()) : "";
            }

            addSmallDataCell(table, sn);
            addSmallDataCell(table, code);
            addSmallDataCell(table, scripts);
        }

        return table;
    }

    private void addHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_HEADER));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(2);
        table.addCell(cell);
    }

    private void addDataCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", FONT_NORMAL));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(2);
        table.addCell(cell);
    }

    private void addSmallHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_BOLD_SMALL));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(1.0f);
        table.addCell(cell);
    }

    private void addSmallDataCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_SMALL));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(1.0f);
        table.addCell(cell);
    }

    private void addAmountRow(PdfPTable table, String label, String rate, String amount) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, FONT_NORMAL));
        labelCell.setBorder(0);
        labelCell.setPaddingBottom(2);
        table.addCell(labelCell);

        PdfPCell rateCell = new PdfPCell(new Phrase(rate, FONT_ITALIC));
        rateCell.setBorder(0);
        rateCell.setPaddingBottom(2);
        rateCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(rateCell);

        PdfPCell amountCell = new PdfPCell(new Phrase(amount, FONT_BOLD));
        amountCell.setBorder(0);
        amountCell.setPaddingBottom(2);
        amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(amountCell);
    }

    private void addTotalRow(PdfPTable table, String label, String amount) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, FONT_BOLD));
        labelCell.setColspan(2);
        labelCell.setBorder(0);
        labelCell.setBorderWidthTop(1);
        labelCell.setPaddingTop(2);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(labelCell);

        PdfPCell amountCell = new PdfPCell(new Phrase(amount, FONT_LARGE_BOLD));
        amountCell.setBorder(0);
        amountCell.setBorderWidthTop(1);
        amountCell.setPaddingTop(2);
        amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(amountCell);
    }

    private void addSimpleAmountRow(PdfPTable table, String label, String amount) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, FONT_NORMAL));
        labelCell.setBorder(0);
        labelCell.setPaddingBottom(2);
        table.addCell(labelCell);

        PdfPCell amountCell = new PdfPCell(new Phrase(amount, FONT_BOLD));
        amountCell.setBorder(0);
        amountCell.setPaddingBottom(2);
        amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(amountCell);
    }

    private void addBankGridCell(PdfPTable table, String text, Font font, int alignment, boolean isHeader) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(2);
        cell.setMinimumHeight(22);
        if (isHeader) {
            cell.setBackgroundColor(new Color(242, 242, 242));
        }
        table.addCell(cell);
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) return "0.00";
        return String.format("%.2f", amount);
    }
}

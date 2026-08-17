package com.exam.claims.service;

import com.exam.claims.entity.ClaimRecord;
import com.exam.claims.entity.ScriptDetail;
import com.exam.claims.util.AmountToWordsConverter;
import com.exam.claims.util.ClaimConstants;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.xmlbeans.XmlCursor;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generates Word (.docx) claim documents using Apache POI XWPF.
 * Matches the same Landscape A4 side-by-side layout as the PDF generator.
 */
@Service
public class WordGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(WordGeneratorService.class);

    private final com.exam.repository.master.TenantRepository tenantRepository;

    public WordGeneratorService(com.exam.repository.master.TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    /**
     * Generate a multi-page Word document for all claims in a batch.
     */
    public byte[] generateBatchWord(List<ClaimRecord> records) throws IOException {
        XWPFDocument document = new XWPFDocument();
        setDocumentToA4Landscape(document);

        for (int i = 0; i < records.size(); i++) {
            if (i > 0) {
                // Page break between claims
                XWPFParagraph breakPara = document.createParagraph();
                breakPara.setPageBreak(true);
            }
            generateClaimPage(document, records.get(i));
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        document.write(baos);
        document.close();
        return baos.toByteArray();
    }

    /**
     * Generate a single claim Word document.
     */
    public byte[] generateSingleWord(ClaimRecord record) throws IOException {
        XWPFDocument document = new XWPFDocument();
        setDocumentToA4Landscape(document);
        generateClaimPage(document, record);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        document.write(baos);
        document.close();
        return baos.toByteArray();
    }

    private void generateClaimPage(XWPFDocument document, ClaimRecord record) {
        if (record.isRevaluation()) {
            addRevaluationPage(document, record);
            return;
        }

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

        // Bank Details
        addBankDetails(document, record);
    }

    // ==================== LANDSCAPE PAGE SETTING ====================

    private void setDocumentToA4Landscape(XWPFDocument document) {
        CTDocument1 doc = document.getDocument();
        CTBody body = doc.getBody();
        CTSectPr sectPr = body.isSetSectPr() ? body.getSectPr() : body.addNewSectPr();
        
        CTPageSz pageSz = sectPr.isSetPgSz() ? sectPr.getPgSz() : sectPr.addNewPgSz();
        pageSz.setW(java.math.BigInteger.valueOf(16838)); // A4 Landscape width (11.69 inches)
        pageSz.setH(java.math.BigInteger.valueOf(11906)); // A4 Landscape height (8.27 inches)
        pageSz.setOrient(STPageOrientation.LANDSCAPE);
        
        CTPageMar pageMar = sectPr.isSetPgMar() ? sectPr.getPgMar() : sectPr.addNewPgMar();
        pageMar.setLeft(java.math.BigInteger.valueOf(720));   // 0.5 in margin for tight Landscape fit
        pageMar.setRight(java.math.BigInteger.valueOf(720));
        pageMar.setTop(java.math.BigInteger.valueOf(360));    // Tight 0.25 in top margin to fit single page
        pageMar.setBottom(java.math.BigInteger.valueOf(360)); // Tight 0.25 in bottom margin to fit single page
    }

    // ==================== HEADER ====================

    private void addHeader(XWPFDocument document, ClaimRecord record) {
        XWPFTable titleTable = document.createTable(1, 2);
        setTableWidth(titleTable, "100%");
        clearTableBorders(titleTable);
        titleTable.setCellMargins(0, 0, 0, 0);

        // Column 1: Logo cell
        XWPFTableCell logoCell = titleTable.getRow(0).getCell(0);
        XWPFParagraph logoPara = logoCell.getParagraphs().get(0);
        logoPara.setAlignment(ParagraphAlignment.CENTER);
        logoPara.setSpacingBefore(0);
        logoPara.setSpacingAfter(0);
        XWPFRun logoRun = logoPara.createRun();
        
        try {
            byte[] logoBytes = null;
            int pictureType = Document.PICTURE_TYPE_JPEG;
            String uniqueSuffix = java.util.UUID.randomUUID().toString().substring(0, 8);
            String fileName = "krce_logo_" + uniqueSuffix + ".jpg";
            
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
                        
                        if (logoBytes != null && logoBytes.length > 8 && logoBytes[0] == (byte) 137 && logoBytes[1] == (byte) 80 && logoBytes[2] == (byte) 78 && logoBytes[3] == (byte) 71) {
                            pictureType = Document.PICTURE_TYPE_PNG;
                            fileName = "krce_logo_" + uniqueSuffix + ".png";
                        }
                    }
                } finally {
                    com.exam.config.tenant.TenantContext.setCurrentTenant(tenantId);
                }
            }

            if (logoBytes != null) {
                try {
                    java.io.ByteArrayInputStream imageBis = new java.io.ByteArrayInputStream(logoBytes);
                    java.awt.image.BufferedImage bimg = javax.imageio.ImageIO.read(imageBis);
                    
                    if (bimg != null) {
                        int origWidth = bimg.getWidth();
                        int origHeight = bimg.getHeight();
                        // Scale to fit within 75x75 points max while preserving aspect ratio
                        double scale = Math.min(75.0 / origWidth, 75.0 / origHeight);
                        int widthEMU = org.apache.poi.util.Units.toEMU(origWidth * scale);
                        int heightEMU = org.apache.poi.util.Units.toEMU(origHeight * scale);
                        
                        // Regenerate image cleanly as PNG to strip out any weird JPEG EXIF/markers Word might reject
                        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                        javax.imageio.ImageIO.write(bimg, "png", baos);
                        byte[] cleanPngBytes = baos.toByteArray();
                        
                        java.io.ByteArrayInputStream cleanBis = new java.io.ByteArrayInputStream(cleanPngBytes);
                        logoRun.addPicture(cleanBis, Document.PICTURE_TYPE_PNG, "krce_logo_" + uniqueSuffix + ".png", widthEMU, heightEMU);
                    } else {
                        // Fallback if ImageIO could not read the image
                        java.io.ByteArrayInputStream fallbackBis = new java.io.ByteArrayInputStream(logoBytes);
                        logoRun.addPicture(fallbackBis, pictureType, fileName, org.apache.poi.util.Units.toEMU(75), org.apache.poi.util.Units.toEMU(75));
                    }
                } catch (Exception e) {
                    log.warn("Could not regenerate clean image for Word embedding", e);
                    java.io.ByteArrayInputStream fallbackBis = new java.io.ByteArrayInputStream(logoBytes);
                    try {
                        logoRun.addPicture(fallbackBis, pictureType, fileName, org.apache.poi.util.Units.toEMU(75), org.apache.poi.util.Units.toEMU(75));
                    } catch (Exception innerE) {
                        log.error("Fallback image embedding also failed", innerE);
                    }
                }
            } else {
                try (var is = getClass().getResourceAsStream("/krce_logo.jpg")) {
                    if (is != null) {
                        logoRun.addPicture(is, Document.PICTURE_TYPE_JPEG, "krce_logo.jpg",
                            org.apache.poi.util.Units.toEMU(75), org.apache.poi.util.Units.toEMU(75));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Could not insert logo into Word", e);
        }

        // Column 2: Text cell with no vertical divider border
        XWPFTableCell textCell = titleTable.getRow(0).getCell(1);
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr tcPr = textCell.getCTTc().isSetTcPr() ? textCell.getCTTc().getTcPr() : textCell.getCTTc().addNewTcPr();

        // Set small left padding in cell via TcMar
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcMar tcMar = tcPr.isSetTcMar() ? tcPr.getTcMar() : tcPr.addNewTcMar();
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth leftMar = tcMar.addNewLeft();
        leftMar.setW(java.math.BigInteger.valueOf(80));
        leftMar.setType(org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth.DXA);

        boolean isRev = record.isRevaluation();
        int officeFs = isRev ? 14 : 22;
        int collegeFs = isRev ? 11 : 14;
        int autonomyFs = isRev ? 9 : 11;
        int examFs = isRev ? 10 : 12;

        XWPFParagraph officePara = textCell.getParagraphs().get(0);
        addCenteredParagraphToCell(officePara, ClaimConstants.OFFICE_TITLE, officeFs, true);

        XWPFParagraph collegePara = textCell.addParagraph();
        addCenteredParagraphToCell(collegePara, ClaimConstants.COLLEGE_NAME, collegeFs, true);

        XWPFParagraph autonomyPara = textCell.addParagraph();
        addCenteredParagraphToCell(autonomyPara, ClaimConstants.COLLEGE_AUTONOMY, autonomyFs, false);

        XWPFParagraph examPara = textCell.addParagraph();
        String examTitle = isRev
            ? "ESE - Re-Valuation - " + (record.getExamSeason() != null ? record.getExamSeason() : "DEC 2025") + " Examinations"
            : ClaimConstants.getExamTitle(record.getExamSeason());
        addCenteredParagraphToCell(examPara, examTitle, examFs, true);

        setRowColWidths(titleTable, new int[]{2200, 13200});

        // Small spacer paragraph
        XWPFParagraph headerSpacer = document.createParagraph();
        headerSpacer.setSpacingBefore(0);
        headerSpacer.setSpacingAfter(0);
        XWPFRun spacerRun = headerSpacer.createRun();
        spacerRun.setFontSize(1);

        // Date and Sessions line
        String dateStr = record.getValuationDate() != null
            ? record.getValuationDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
            : "N/A";
        String sessions = record.getSessionsAttended() != null ? record.getSessionsAttended() : "N/A";

        XWPFParagraph datePara = document.createParagraph();
        datePara.setAlignment(ParagraphAlignment.LEFT);
        datePara.setSpacingBefore(isRev ? 5 : 20);
        datePara.setSpacingAfter(isRev ? 5 : 20);

        CTP ctp = datePara.getCTP();
        CTPPr ppr = ctp.getPPr() == null ? ctp.addNewPPr() : ctp.getPPr();
        CTTabs tabs = ppr.getTabs() == null ? ppr.addNewTabs() : ppr.getTabs();
        CTTabStop tab = tabs.addNewTab();
        tab.setVal(STTabJc.RIGHT);
        tab.setPos(java.math.BigInteger.valueOf(15700)); // Push completely to the right margin

        XWPFRun leftRun = datePara.createRun();
        leftRun.setText("Date of Valuation(dd-mm-yyyy) : " + dateStr);
        leftRun.setBold(true);
        leftRun.setFontSize(isRev ? 8 : 9);
        leftRun.setFontFamily("Times New Roman");

        XWPFRun tabRun = datePara.createRun();
        tabRun.addTab();

        XWPFRun rightRun = datePara.createRun();
        rightRun.setText("No. of Sessions : " + sessions);
        rightRun.setBold(true);
        rightRun.setFontSize(9);
        rightRun.setFontFamily("Times New Roman");
    }

    private void addCenteredParagraphToCell(XWPFParagraph para, String text, int fontSize, boolean bold) {
        para.setAlignment(ParagraphAlignment.CENTER);
        para.setSpacingBefore(0);
        para.setSpacingAfter(4);
        XWPFRun run = para.createRun();
        run.setText(text);
        run.setFontSize(fontSize);
        run.setBold(bold);
        run.setFontFamily("Times New Roman");
    }

    // ==================== PERSONAL INFO TABLE ====================

    private void addPersonalInfoTable(XWPFDocument document, ClaimRecord record) {
        XWPFTable table = document.createTable(2, 6);
        initializeTable(table, 2, 6);
        setTableWidth(table, "100%");
        setTableBorders(table);
        table.setCellMargins(20, 80, 20, 80); // Tight cell margins to prevent double-page overflow

        // Header row
        formatCell(table.getRow(0).getCell(0), "Post Held", true, 9, null, ParagraphAlignment.CENTER);
        formatCell(table.getRow(0).getCell(1), "Name", true, 9, null, ParagraphAlignment.CENTER);
        formatCell(table.getRow(0).getCell(2), "Mobile Number", true, 9, null, ParagraphAlignment.CENTER);
        formatCell(table.getRow(0).getCell(3), "Designation", true, 9, null, ParagraphAlignment.CENTER);
        formatCell(table.getRow(0).getCell(4), "Name of Institution", true, 9, null, ParagraphAlignment.CENTER);
        formatCell(table.getRow(0).getCell(5), "Board", true, 9, null, ParagraphAlignment.CENTER);

        // Data row
        formatCell(table.getRow(1).getCell(0), record.getPostHeld(), false, 9, null, ParagraphAlignment.CENTER);
        formatCell(table.getRow(1).getCell(1), record.getFullName(), false, 9, null, ParagraphAlignment.CENTER);
        formatCell(table.getRow(1).getCell(2), record.getMobileNo(), false, 9, null, ParagraphAlignment.CENTER);
        formatCell(table.getRow(1).getCell(3), record.getDesignation(), false, 9, null, ParagraphAlignment.CENTER);
        formatCell(table.getRow(1).getCell(4), record.getInstitutionName(), false, 9, null, ParagraphAlignment.CENTER);
        formatCell(table.getRow(1).getCell(5), record.getBoardName(), false, 9, null, ParagraphAlignment.CENTER);

        // Align column widths beautifully for Landscape (Total ~15400 twips)
        setRowColWidths(table, new int[]{2000, 3000, 2000, 2000, 4900, 1500});
    }

    // ==================== EXAMINER BODY ====================

    private void addExaminerBody(XWPFDocument document, ClaimRecord record) {
        XWPFParagraph spacing = document.createParagraph();
        spacing.setSpacingBefore(0);
        spacing.setSpacingAfter(0);
        XWPFRun spacerRun = spacing.createRun();
        spacerRun.setFontSize(2);
        spacerRun.setText("");

        // Create Parent table with solid borders, 2 rows and 2 columns
        XWPFTable parentTable = document.createTable(2, 2);
        initializeTable(parentTable, 2, 2);
        setTableWidth(parentTable, "100%");
        setOuterTableBorders(parentTable);
        parentTable.setCellMargins(0, 80, 0, 80);

        // Width allocations
        XWPFTableCell leftCell = parentTable.getRow(0).getCell(0);
        XWPFTableCell rightCell = parentTable.getRow(0).getCell(1);
        leftCell.setWidth("8000");
        rightCell.setWidth("7400");
        
        XWPFTableCell leftCellSig = parentTable.getRow(1).getCell(0);
        XWPFTableCell rightCellSig = parentTable.getRow(1).getCell(1);
        leftCellSig.setWidth("8000");
        rightCellSig.setWidth("7400");

        // --- LEFT COLUMN: Subject Details ---
        XWPFParagraph titlePara = leftCell.getParagraphs().isEmpty() ? leftCell.addParagraph() : leftCell.getParagraphs().get(0);
        addParagraph(titlePara, "Detail of Subjects Valued (Issue Reg. Pg.No: "
            + (record.getIssueRegPageNo() != null ? record.getIssueRegPageNo() : "N/A") + ")", 11, true, ParagraphAlignment.CENTER);

        // Nested 1x2 layout table to place FN and AN tables side by side
        XWPFTable fnAnParent = createNestedTable(leftCell, 1, 2);
        setTableWidth(fnAnParent, "100%");
        clearTableBorders(fnAnParent);
        fnAnParent.setCellMargins(0, 0, 0, 0); // Reset padding to align perfectly
        XWPFTableCell fnCell = fnAnParent.getRow(0).getCell(0);
        XWPFTableCell anCell = fnAnParent.getRow(0).getCell(1);
        fnCell.setWidth("4000");
        anCell.setWidth("4000");

        int scriptRows = record.isRevaluation() ? 17 : 12;

        // FN Table
        XWPFTable fnTable = createNestedTable(fnCell, scriptRows, 3);
        setTableWidth(fnTable, "100%");
        setTableBorders(fnTable);
        fnTable.setCellMargins(0, 30, 0, 30); // tight padding for scripts table
        populateScriptTable(fnTable, record, "FN");

        // AN Table
        XWPFTable anTable = createNestedTable(anCell, scriptRows, 3);
        setTableWidth(anTable, "100%");
        setTableBorders(anTable);
        anTable.setCellMargins(0, 30, 0, 30);
        populateScriptTable(anTable, record, "AN");

        // --- RIGHT COLUMN: Claim Details ---
        XWPFParagraph amtTitle = rightCell.getParagraphs().isEmpty() ? rightCell.addParagraph() : rightCell.getParagraphs().get(0);
        addParagraph(amtTitle, "Detail of Claim Amount", 14, true, ParagraphAlignment.CENTER);

        XWPFTable amtTable = createNestedTable(rightCell, 4, 3);
        setTableWidth(amtTable, "100%");
        setTableBorders(amtTable);
        amtTable.setCellMargins(0, 60, 0, 60);

        // Script Amount Row
        XWPFTableRow r0 = amtTable.getRow(0);
        formatCell(r0.getCell(0), "Script Amount (Rs.)\n(" + record.getTotalScripts() + " Scripts)", false, 10, null, ParagraphAlignment.LEFT);
        formatCell(r0.getCell(1), "UG and PG : 30/script", false, 10, null, ParagraphAlignment.CENTER);
        formatCell(r0.getCell(2), formatAmount(record.getScriptAmount()), true, 10, null, ParagraphAlignment.RIGHT);

        // TA Row
        BigDecimal distanceKm = record.getDistanceKm() != null ? record.getDistanceKm() : BigDecimal.ZERO;
        BigDecimal roundTrip = distanceKm.multiply(new BigDecimal("2"));
        XWPFTableRow r1 = amtTable.getRow(1);
        formatCell(r1.getCell(0), "Travelling Allowance (Rs.)\n(" + roundTrip.intValue() + " Km)", false, 10, null, ParagraphAlignment.LEFT);
        formatCell(r1.getCell(1), "Rs. 8/Km (To and Fro)\nRs. 150 (Up to 35 Km)", false, 10, null, ParagraphAlignment.CENTER);
        formatCell(r1.getCell(2), formatAmount(record.getTravellingAllowance()), true, 10, null, ParagraphAlignment.RIGHT);

        // DA Row
        XWPFTableRow r2 = amtTable.getRow(2);
        formatCell(r2.getCell(0), "Dearness Allowance (Rs.)\n(Session: " + record.getSessionsAttended() + "; Int./Ext.: " + (record.getFacultyType() != null ? record.getFacultyType() : "N/A") + ")", false, 10, null, ParagraphAlignment.LEFT);
        formatCell(r2.getCell(1), "Rs.300/Day\nRs. 250/Session", false, 10, null, ParagraphAlignment.CENTER);
        formatCell(r2.getCell(2), formatAmount(record.getDearnessAllowance()), true, 10, null, ParagraphAlignment.RIGHT);

        // Total Row (Horizontally merge cells 0 and 1)
        mergeCellsHorizontal(amtTable, 3, 0, 1);
        XWPFTableRow r3 = amtTable.getRow(3);
        formatCell(r3.getCell(0), "Total Amount (Rs.)", true, 10, null, ParagraphAlignment.LEFT);
        formatCell(r3.getCell(2), formatAmount(record.getTotalAmount()), true, 12, null, ParagraphAlignment.RIGHT);

        // Widths for amount table columns
        setRowColWidths(amtTable, new int[]{3200, 2400, 1800});

        // Received Summary
        XWPFParagraph wordsPara = rightCell.addParagraph();
        wordsPara.setSpacingBefore(0);
        wordsPara.setSpacingAfter(0);
        addParagraph(wordsPara, AmountToWordsConverter.formatReceivedLine(record.getTotalAmount()), 11, false, ParagraphAlignment.LEFT);

        // Signature in the bottom row (Right Cell) - align to bottom natively instead of empty paragraphs
        rightCellSig.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.BOTTOM);
        XWPFParagraph sigTextPara = rightCellSig.getParagraphs().isEmpty() ? rightCellSig.addParagraph() : rightCellSig.getParagraphs().get(0);
        sigTextPara.setSpacingBefore(0); // Slight gap
        sigTextPara.setSpacingAfter(0);
        addParagraph(sigTextPara, "Signature of the Examiner", 11, true, ParagraphAlignment.CENTER);

        XWPFParagraph stampPara = rightCellSig.addParagraph();
        stampPara.setSpacingBefore(0);
        stampPara.setSpacingAfter(0);
        addParagraph(stampPara, "(Re. 1/-Revenue Stamp to be affixed if the claim above Rs. 5000/-)", 10, false, ParagraphAlignment.CENTER);
    }

    // ==================== CHIEF EXAMINER BODY ====================

    private void addChiefExaminerBody(XWPFDocument document, ClaimRecord record) {
        XWPFParagraph spacing = document.createParagraph();
        spacing.setSpacingBefore(0);
        spacing.setSpacingAfter(0);
        XWPFRun spacerRun = spacing.createRun();
        spacerRun.setFontSize(2);
        spacerRun.setText("");

        // Create Parent table with solid borders, 2 rows and 2 columns
        XWPFTable parentTable = document.createTable(2, 2);
        initializeTable(parentTable, 2, 2);
        setTableWidth(parentTable, "100%");
        setOuterTableBorders(parentTable);
        parentTable.setCellMargins(20, 80, 20, 80);

        XWPFTableCell leftCell = parentTable.getRow(0).getCell(0);
        XWPFTableCell rightCell = parentTable.getRow(0).getCell(1);
        leftCell.setWidth("8000");
        rightCell.setWidth("7400");
        
        XWPFTableCell leftCellSig = parentTable.getRow(1).getCell(0);
        XWPFTableCell rightCellSig = parentTable.getRow(1).getCell(1);
        leftCellSig.setWidth("8000");
        rightCellSig.setWidth("7400");

        // --- LEFT COLUMN: Script Amount detail ---
        XWPFParagraph titlePara = leftCell.getParagraphs().isEmpty() ? leftCell.addParagraph() : leftCell.getParagraphs().get(0);
        addParagraph(titlePara, "Script Amount detail", 14, true, ParagraphAlignment.CENTER);

        // Details Nested Table for perfect bullet-alignment (invisible borders)
        XWPFTable detailsTable = createNestedTable(leftCell, 3, 2);
        setTableWidth(detailsTable, "100%");
        clearTableBorders(detailsTable);
        detailsTable.setCellMargins(20, 60, 20, 60);

        formatCell(detailsTable.getRow(0).getCell(0), "Max. Scripts Valued", false, 11, null, ParagraphAlignment.LEFT);
        formatCell(detailsTable.getRow(0).getCell(1), " : " + record.getMaxScriptsValued(), false, 11, null, ParagraphAlignment.LEFT);

        formatCell(detailsTable.getRow(1).getCell(0), "Amount for Max. Scripts Valued", false, 11, null, ParagraphAlignment.LEFT);
        formatCell(detailsTable.getRow(1).getCell(1), " : Rs." + formatAmount(record.getMaxScriptsAmount()), false, 11, null, ParagraphAlignment.LEFT);

        formatCell(detailsTable.getRow(2).getCell(0), "10% of the amount for Max. Scripts Valued", false, 11, null, ParagraphAlignment.LEFT);
        formatCell(detailsTable.getRow(2).getCell(1), " : Rs." + formatAmount(record.getTenPercentAmount()), false, 11, null, ParagraphAlignment.LEFT);

        setRowColWidths(detailsTable, new int[]{5000, 2400});

        // --- RIGHT COLUMN: Claim Details ---
        XWPFParagraph amtTitle = rightCell.getParagraphs().isEmpty() ? rightCell.addParagraph() : rightCell.getParagraphs().get(0);
        addParagraph(amtTitle, "Detail of Claim Amount", 14, true, ParagraphAlignment.CENTER);

        // 3-Column Amount table for Chief Examiner matching the Examiner & spec visual layout
        XWPFTable amtTable = createNestedTable(rightCell, 4, 3);
        setTableWidth(amtTable, "100%");
        setTableBorders(amtTable);
        amtTable.setCellMargins(20, 60, 20, 60);

        // Merge horizontal cells for Row 0 and Row 3 (Overall Script & Total)
        mergeCellsHorizontal(amtTable, 0, 0, 1);
        mergeCellsHorizontal(amtTable, 3, 0, 1);

        // Overall Script Amount (Row 0)
        XWPFTableRow r0 = amtTable.getRow(0);
        formatCell(r0.getCell(0), "Overall Script amount (Rs.)", false, 10, null, ParagraphAlignment.LEFT);
        formatCell(r0.getCell(2), formatAmount(record.getOverallScriptAmount()), true, 10, null, ParagraphAlignment.RIGHT);

        // TA (Row 1)
        BigDecimal distanceKm = record.getDistanceKm() != null ? record.getDistanceKm() : BigDecimal.ZERO;
        BigDecimal roundTrip = distanceKm.multiply(new BigDecimal("2"));
        XWPFTableRow r1 = amtTable.getRow(1);
        formatCell(r1.getCell(0), "Travelling Allowance (Rs.)\n(" + roundTrip.intValue() + " Km)", false, 10, null, ParagraphAlignment.LEFT);
        formatCell(r1.getCell(1), "Rs. 8/Km (To and Fro)\nRs. 150 (Up to 35 Km)", false, 10, null, ParagraphAlignment.CENTER);
        formatCell(r1.getCell(2), formatAmount(record.getTravellingAllowance()), true, 10, null, ParagraphAlignment.RIGHT);

        // DA (Row 2)
        XWPFTableRow r2 = amtTable.getRow(2);
        formatCell(r2.getCell(0), "Dearness Allowance (Rs.)\n(Session: " + record.getSessionsAttended() + "; Int./Ext.: " + (record.getFacultyType() != null ? record.getFacultyType() : "N/A") + ")", false, 10, null, ParagraphAlignment.LEFT);
        formatCell(r2.getCell(1), "Rs.300/Day\nRs. 250/Session", false, 10, null, ParagraphAlignment.CENTER);
        formatCell(r2.getCell(2), formatAmount(record.getDearnessAllowance()), true, 10, null, ParagraphAlignment.RIGHT);

        // Total Amount (Row 3)
        XWPFTableRow r3 = amtTable.getRow(3);
        formatCell(r3.getCell(0), "Total Amount (Rs.)", true, 10, null, ParagraphAlignment.LEFT);
        formatCell(r3.getCell(2), formatAmount(record.getTotalAmount()), true, 12, null, ParagraphAlignment.RIGHT);

        // Width allocations
        setRowColWidths(amtTable, new int[]{3200, 2400, 1800});

        // Words
        XWPFParagraph wordsPara = rightCell.addParagraph();
        wordsPara.setSpacingBefore(10);
        wordsPara.setSpacingAfter(10);
        addParagraph(wordsPara, AmountToWordsConverter.formatReceivedLine(record.getTotalAmount()), 11, false, ParagraphAlignment.LEFT);

        // Signature in the bottom row (Right Cell) - triple line space using empty paragraphs
        XWPFParagraph sigPara = rightCellSig.getParagraphs().isEmpty() ? rightCellSig.addParagraph() : rightCellSig.getParagraphs().get(0);
        sigPara.setSpacingBefore(0);
        sigPara.setSpacingAfter(0);
        XWPFRun emptyRun1 = sigPara.createRun();
        emptyRun1.setFontSize(11);
        emptyRun1.setText("");

        XWPFParagraph sigSpace2 = rightCellSig.addParagraph();
        sigSpace2.setSpacingBefore(0);
        sigSpace2.setSpacingAfter(0);
        XWPFRun emptyRun2 = sigSpace2.createRun();
        emptyRun2.setFontSize(11);
        emptyRun2.setText("");

        XWPFParagraph sigSpace3 = rightCellSig.addParagraph();
        sigSpace3.setSpacingBefore(0);
        sigSpace3.setSpacingAfter(0);
        XWPFRun emptyRun3 = sigSpace3.createRun();
        emptyRun3.setFontSize(11);
        emptyRun3.setText("");

        XWPFParagraph sigTextPara = rightCellSig.addParagraph();
        sigTextPara.setSpacingBefore(0);
        sigTextPara.setSpacingAfter(0);
        addParagraph(sigTextPara, "Signature of the Chief Examiner", 11, true, ParagraphAlignment.CENTER);

        XWPFParagraph stampPara = rightCellSig.addParagraph();
        stampPara.setSpacingBefore(0);
        stampPara.setSpacingAfter(0);
        addParagraph(stampPara, "(Re. 1/-Revenue Stamp to be affixed if the claim above Rs. 5000/-)", 10, false, ParagraphAlignment.CENTER);
    }

    // ==================== ASSISTANT EXAMINER BODY ====================

    private void addAssistantExaminerBody(XWPFDocument document, ClaimRecord record) {
        XWPFParagraph spacing = document.createParagraph();
        spacing.setSpacingBefore(0);
        spacing.setSpacingAfter(0);
        XWPFRun spacerRun = spacing.createRun();
        spacerRun.setFontSize(2);
        spacerRun.setText("");

        // Create Parent table with solid borders, 2 rows and 2 columns
        XWPFTable parentTable = document.createTable(2, 2);
        initializeTable(parentTable, 2, 2);
        setTableWidth(parentTable, "100%");
        setOuterTableBorders(parentTable);
        parentTable.setCellMargins(20, 80, 20, 80);

        XWPFTableCell leftCell = parentTable.getRow(0).getCell(0);
        XWPFTableCell rightCell = parentTable.getRow(0).getCell(1);
        leftCell.setWidth("8000");
        rightCell.setWidth("7400");
        
        XWPFTableCell leftCellSig = parentTable.getRow(1).getCell(0);
        XWPFTableCell rightCellSig = parentTable.getRow(1).getCell(1);
        leftCellSig.setWidth("8000");
        rightCellSig.setWidth("7400");

        // --- LEFT COLUMN: Assistant Description ---
        XWPFParagraph titlePara = leftCell.getParagraphs().isEmpty() ? leftCell.addParagraph() : leftCell.getParagraphs().get(0);
        addParagraph(titlePara, "Assistant Examiner Claim", 14, true, ParagraphAlignment.CENTER);

        XWPFParagraph detailsPara = leftCell.addParagraph();
        detailsPara.setSpacingBefore(30);
        detailsPara.setSpacingAfter(30);
        addParagraph(detailsPara, "Sessions Attended : " + (record.getSessionsAttended() != null ? record.getSessionsAttended() : "N/A"), 11, false, ParagraphAlignment.CENTER);

        // --- RIGHT COLUMN: Amount details (Structured Table) ---
        XWPFParagraph amtTitle = rightCell.getParagraphs().isEmpty() ? rightCell.addParagraph() : rightCell.getParagraphs().get(0);
        addParagraph(amtTitle, "Detail of Claim Amount", 14, true, ParagraphAlignment.CENTER);

        XWPFTable amtTable = createNestedTable(rightCell, 4, 3);
        setTableWidth(amtTable, "100%");
        setTableBorders(amtTable);
        amtTable.setCellMargins(0, 60, 0, 60);

        // Base Remuneration Row
        XWPFTableRow r0 = amtTable.getRow(0);
        formatCell(r0.getCell(0), "Base Remuneration (Rs.)\n(Session: " + (record.getSessionsAttended() != null ? record.getSessionsAttended() : "N/A") + ")", false, 10, null, ParagraphAlignment.LEFT);
        formatCell(r0.getCell(1), "Rs. 550/Full Day\nRs. 275/Half Day", false, 10, null, ParagraphAlignment.CENTER);
        formatCell(r0.getCell(2), formatAmount(record.getScriptAmount()), true, 10, null, ParagraphAlignment.RIGHT);

        // TA Row
        XWPFTableRow r1 = amtTable.getRow(1);
        formatCell(r1.getCell(0), "Travelling Allowance (Rs.)", false, 10, null, ParagraphAlignment.LEFT);
        formatCell(r1.getCell(1), "Rs. 0 (Internal)", false, 10, null, ParagraphAlignment.CENTER);
        formatCell(r1.getCell(2), formatAmount(record.getTravellingAllowance()), true, 10, null, ParagraphAlignment.RIGHT);

        // DA Row
        XWPFTableRow r2 = amtTable.getRow(2);
        boolean isHoliday = record.isGovernmentHoliday();
        String daDetails = "Holiday: Rs.300/250\nWorking Day: Rs.0";
        formatCell(r2.getCell(0), "Dearness Allowance (Rs.)\n(Holiday/Sunday: " + (isHoliday ? "YES" : "NO") + ")", false, 10, null, ParagraphAlignment.LEFT);
        formatCell(r2.getCell(1), daDetails, false, 10, null, ParagraphAlignment.CENTER);
        formatCell(r2.getCell(2), formatAmount(record.getDearnessAllowance()), true, 10, null, ParagraphAlignment.RIGHT);

        // Total Row
        mergeCellsHorizontal(amtTable, 3, 0, 1);
        XWPFTableRow r3 = amtTable.getRow(3);
        formatCell(r3.getCell(0), "Total Amount (Rs.)", true, 10, null, ParagraphAlignment.LEFT);
        formatCell(r3.getCell(2), formatAmount(record.getTotalAmount()), true, 12, null, ParagraphAlignment.RIGHT);

        setRowColWidths(amtTable, new int[]{3200, 2400, 1800});

        // Received Summary
        XWPFParagraph wordsPara = rightCell.addParagraph();
        wordsPara.setSpacingBefore(10);
        wordsPara.setSpacingAfter(10);
        addParagraph(wordsPara, AmountToWordsConverter.formatReceivedLine(record.getTotalAmount()), 11, false, ParagraphAlignment.LEFT);

        // Signature in the bottom row (Right Cell)
        rightCellSig.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.BOTTOM);
        XWPFParagraph sigTextPara = rightCellSig.getParagraphs().isEmpty() ? rightCellSig.addParagraph() : rightCellSig.getParagraphs().get(0);
        sigTextPara.setSpacingBefore(0);
        sigTextPara.setSpacingAfter(0);
        addParagraph(sigTextPara, "Signature of the Assistant Examiner", 11, true, ParagraphAlignment.CENTER);

        XWPFParagraph stampPara = rightCellSig.addParagraph();
        stampPara.setSpacingBefore(0);
        stampPara.setSpacingAfter(0);
        addParagraph(stampPara, "(Re. 1/-Revenue Stamp to be affixed if the claim above Rs. 5000/-)", 10, false, ParagraphAlignment.CENTER);
    }

    // ==================== BANK DETAILS ====================

    private void addBankDetails(XWPFDocument document, ClaimRecord record) {
        XWPFParagraph spacing = document.createParagraph();
        spacing.setSpacingBefore(0);
        spacing.setSpacingAfter(0);
        XWPFRun spacerRun = spacing.createRun();
        spacerRun.setFontSize(2);
        spacerRun.setText("");

        XWPFTable bankTable = document.createTable(4, 3);
        initializeTable(bankTable, 4, 3);
        setTableWidth(bankTable, "100%");
        setTableBorders(bankTable);

        boolean isRev = record.isRevaluation();
        int labelFs = isRev ? 8 : 12;
        int valueFs = isRev ? 8 : 12;
        int sigFs = isRev ? 8 : 10;
        int cellMargin = isRev ? 10 : 20;

        bankTable.setCellMargins(cellMargin, isRev ? 30 : 80, cellMargin, isRev ? 30 : 80);

        // Merge third column (Signature block) vertically across all 4 rows
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr tcPr = bankTable.getRow(0).getCell(2).getCTTc().addNewTcPr();
        tcPr.addNewVMerge().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STMerge.RESTART);
        for (int i = 1; i <= 3; i++) {
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr tcPrMerge = bankTable.getRow(i).getCell(2).getCTTc().addNewTcPr();
            tcPrMerge.addNewVMerge().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STMerge.CONTINUE);
        }

        formatCell(bankTable.getRow(0).getCell(0), "ACCOUNT NUMBER :", true, labelFs, "F2F2F2", ParagraphAlignment.CENTER);
        formatCell(bankTable.getRow(0).getCell(1), safeStr(record.getBankAccountNumber()), false, valueFs, null, ParagraphAlignment.CENTER);
        
        formatCell(bankTable.getRow(0).getCell(2), "Signature of the Chief Examiner", true, sigFs, null, ParagraphAlignment.CENTER);
        bankTable.getRow(0).getCell(2).setVerticalAlignment(XWPFTableCell.XWPFVertAlign.BOTTOM);

        formatCell(bankTable.getRow(1).getCell(0), "IFSC CODE :", true, labelFs, "F2F2F2", ParagraphAlignment.CENTER);
        formatCell(bankTable.getRow(1).getCell(1), safeStr(record.getIfscCode()), false, valueFs, null, ParagraphAlignment.CENTER);

        formatCell(bankTable.getRow(2).getCell(0), "BANK NAME :", true, labelFs, "F2F2F2", ParagraphAlignment.CENTER);
        formatCell(bankTable.getRow(2).getCell(1), safeStr(record.getBankName()), false, valueFs, null, ParagraphAlignment.CENTER);

        formatCell(bankTable.getRow(3).getCell(0), "BRANCH :", true, labelFs, "F2F2F2", ParagraphAlignment.CENTER);
        formatCell(bankTable.getRow(3).getCell(1), safeStr(record.getBranch()), false, valueFs, null, ParagraphAlignment.CENTER);

        // Column widths for bank details (Total 15400 twips)
        setRowColWidths(bankTable, new int[]{4000, 6000, 5400});
    }

    // ==================== REVALUATION BODY (EXACT SCRANED LAYOUT) ====================

    private void addRevaluationPage(XWPFDocument document, ClaimRecord record) {
        // 1. Header (Logo + Title)
        addHeader(document, record);

        // 2. Personal Info Table (3 rows x 8 cols) matching exact scanned sample
        XWPFTable infoTable = document.createTable(3, 8);
        initializeTable(infoTable, 3, 8);
        setTableWidth(infoTable, "100%");
        setTableBorders(infoTable);
        infoTable.setCellMargins(10, 30, 10, 30);

        // Row 0: Post Held | EXAMINER | (merge 2-5) Date of Valuation | (merge 6-7) date
        formatCell(infoTable.getRow(0).getCell(0), "Post Held", true, 8, null, ParagraphAlignment.CENTER);
        formatCell(infoTable.getRow(0).getCell(1), record.getPostHeld() != null ? record.getPostHeld() : "EXAMINER", true, 8, null, ParagraphAlignment.CENTER);
        mergeCellsHorizontal(infoTable, 0, 2, 5);
        formatCell(infoTable.getRow(0).getCell(2), "Date of Valuation(dd.mm.yyyy) :", true, 8, null, ParagraphAlignment.LEFT);
        mergeCellsHorizontal(infoTable, 0, 6, 7);
        String dateStr = record.getValuationDate() != null ? record.getValuationDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : "";
        formatCell(infoTable.getRow(0).getCell(6), dateStr, false, 8, null, ParagraphAlignment.CENTER);

        // Row 1: Name | (merge 1-2) name | Mobile Number | (merge 4-5) mobile | Designation | designation
        formatCell(infoTable.getRow(1).getCell(0), "Name", true, 8, null, ParagraphAlignment.CENTER);
        mergeCellsHorizontal(infoTable, 1, 1, 2);
        formatCell(infoTable.getRow(1).getCell(1), safeStr(record.getFullName()), false, 8, null, ParagraphAlignment.CENTER);
        formatCell(infoTable.getRow(1).getCell(3), "Mobile Number", true, 7, null, ParagraphAlignment.CENTER);
        mergeCellsHorizontal(infoTable, 1, 4, 5);
        formatCell(infoTable.getRow(1).getCell(4), safeStr(record.getMobileNo()), false, 8, null, ParagraphAlignment.CENTER);
        formatCell(infoTable.getRow(1).getCell(6), "Designation", true, 7, null, ParagraphAlignment.CENTER);
        formatCell(infoTable.getRow(1).getCell(7), safeStr(record.getDesignation()), true, 8, null, ParagraphAlignment.CENTER);

        // Row 2: Name of Institution | (merge 1-5) institution | Board | board
        formatCell(infoTable.getRow(2).getCell(0), "Name of Institution", true, 7, null, ParagraphAlignment.CENTER);
        mergeCellsHorizontal(infoTable, 2, 1, 5);
        formatCell(infoTable.getRow(2).getCell(1), safeStr(record.getInstitutionName()), false, 8, null, ParagraphAlignment.CENTER);
        formatCell(infoTable.getRow(2).getCell(6), "Board", true, 8, null, ParagraphAlignment.CENTER);
        formatCell(infoTable.getRow(2).getCell(7), safeStr(record.getBoardName()), false, 8, null, ParagraphAlignment.CENTER);

        setRowColWidths(infoTable, new int[]{1800, 1800, 1800, 1800, 1800, 1800, 2300, 2300});

        // Small Spacer
        XWPFParagraph spacing = document.createParagraph();
        spacing.setSpacingBefore(0);
        spacing.setSpacingAfter(0);
        XWPFRun spacerRun = spacing.createRun();
        spacerRun.setFontSize(1);

        // 3. Parent Table (2 rows x 2 cols)
        XWPFTable parentTable = document.createTable(2, 2);
        initializeTable(parentTable, 2, 2);
        setTableWidth(parentTable, "100%");
        setOuterTableBorders(parentTable);
        parentTable.setCellMargins(0, 30, 0, 30);

        XWPFTableCell leftCell = parentTable.getRow(0).getCell(0);
        XWPFTableCell rightCell = parentTable.getRow(0).getCell(1);
        XWPFTableCell leftCellSig = parentTable.getRow(1).getCell(0);
        XWPFTableCell rightCellSig = parentTable.getRow(1).getCell(1);

        leftCell.setWidth("8000");
        rightCell.setWidth("7400");
        leftCellSig.setWidth("8000");
        rightCellSig.setWidth("7400");

        // --- LEFT COLUMN: Subject Tables ---
        XWPFParagraph titlePara = leftCell.getParagraphs().isEmpty() ? leftCell.addParagraph() : leftCell.getParagraphs().get(0);
        addParagraph(titlePara, "Detail of Subjects Valued (Issue Reg. Pg.No: "
            + (record.getIssueRegPageNo() != null ? record.getIssueRegPageNo() : "1") + ")", 9, true, ParagraphAlignment.CENTER);

        // Nested 1x2 layout table for side-by-side FN and AN tables
        XWPFTable fnAnParent = createNestedTable(leftCell, 1, 2);
        setTableWidth(fnAnParent, "100%");
        clearTableBorders(fnAnParent);
        fnAnParent.setCellMargins(0, 0, 0, 0);

        XWPFTableCell fnCell = fnAnParent.getRow(0).getCell(0);
        XWPFTableCell anCell = fnAnParent.getRow(0).getCell(1);
        fnCell.setWidth("4000");
        anCell.setWidth("4000");

        // FN Table (17 rows: Header + SubHeaders + 15 Data Rows)
        XWPFTable fnTable = createNestedTable(fnCell, 17, 3);
        setTableWidth(fnTable, "100%");
        setTableBorders(fnTable);
        fnTable.setCellMargins(0, 10, 0, 10);
        populateScriptTable(fnTable, record, "FN");

        // AN Table (17 rows: Header + SubHeaders + 15 Data Rows)
        XWPFTable anTable = createNestedTable(anCell, 17, 3);
        setTableWidth(anTable, "100%");
        setTableBorders(anTable);
        anTable.setCellMargins(0, 10, 0, 10);
        populateScriptTable(anTable, record, "AN");

        // --- RIGHT COLUMN: Detail of Claim Amount ---
        XWPFParagraph amtTitle = rightCell.getParagraphs().isEmpty() ? rightCell.addParagraph() : rightCell.getParagraphs().get(0);
        addParagraph(amtTitle, "Detail of Claim Amount", 10, true, ParagraphAlignment.CENTER);

        // Claim Amount Table: 4 rows x 2 cols (Description | Rate & Amount)
        XWPFTable ct = createNestedTable(rightCell, 4, 2);
        setTableWidth(ct, "100%");
        setTableBorders(ct);
        ct.setCellMargins(10, 30, 10, 30);

        // Row 0: Script Amount
        formatCellMultiline(ct.getRow(0).getCell(0), "Script Amount (Rs.)\n(" + (record.getTotalScripts() != null ? record.getTotalScripts() : 0) + " Scripts)", true, 8, null, ParagraphAlignment.LEFT);
        formatCellMultiline(ct.getRow(0).getCell(1), "UG and PG : 30/script\n(Min. Rs.100/- per subject)\nRs. " + formatAmount(record.getScriptAmount()), false, 7, null, ParagraphAlignment.LEFT);

        // Row 1: Travelling Allowance
        BigDecimal distanceKm = record.getDistanceKm() != null ? record.getDistanceKm() : BigDecimal.ZERO;
        BigDecimal roundTrip = distanceKm.multiply(new BigDecimal("2"));
        formatCellMultiline(ct.getRow(1).getCell(0), "Travelling Allowance (Rs.)\n(" + roundTrip.intValue() + " Km)", true, 8, null, ParagraphAlignment.LEFT);
        formatCellMultiline(ct.getRow(1).getCell(1), "Rs. 8/Km (To and Fro)\nRs. 150 (Up to 35 Km)\nRs. " + formatAmount(record.getTravellingAllowance()), false, 7, null, ParagraphAlignment.LEFT);

        // Row 2: Dearness Allowance
        formatCellMultiline(ct.getRow(2).getCell(0), "Dearness Allowance (Rs.)\n(Session: " + (record.getSessionsAttended() != null ? record.getSessionsAttended() : "Only AN") + "; Int./Ext. : " + (record.getFacultyType() != null ? record.getFacultyType() : "INT.") + ")", true, 7, null, ParagraphAlignment.LEFT);
        formatCellMultiline(ct.getRow(2).getCell(1), "Rs.300/Day\nRs. 250/Session\nRs. " + formatAmount(record.getDearnessAllowance()), false, 7, null, ParagraphAlignment.LEFT);

        // Row 3: Total Amount
        formatCell(ct.getRow(3).getCell(0), "Total Amount (Rs.)", true, 9, null, ParagraphAlignment.LEFT);
        formatCell(ct.getRow(3).getCell(1), "Rs. " + formatAmount(record.getTotalAmount()), true, 9, null, ParagraphAlignment.RIGHT);

        setRowColWidths(ct, new int[]{3400, 4000});

        // Received Line below table
        XWPFParagraph wordsPara = rightCell.addParagraph();
        wordsPara.setSpacingBefore(3);
        wordsPara.setSpacingAfter(0);
        addParagraph(wordsPara, AmountToWordsConverter.formatReceivedLine(record.getTotalAmount()), 8, false, ParagraphAlignment.LEFT);

        // Signature on bottom right
        rightCellSig.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.BOTTOM);
        XWPFParagraph sigTextPara = rightCellSig.getParagraphs().isEmpty() ? rightCellSig.addParagraph() : rightCellSig.getParagraphs().get(0);
        sigTextPara.setSpacingBefore(0);
        sigTextPara.setSpacingAfter(0);
        addParagraph(sigTextPara, "Signature of the Examiner", 9, true, ParagraphAlignment.CENTER);

        XWPFParagraph stampPara = rightCellSig.addParagraph();
        stampPara.setSpacingBefore(0);
        stampPara.setSpacingAfter(0);
        addParagraph(stampPara, "(Re. 1/-Revenue Stamp to be affixed if the claim above Rs. 5000/-)", 6, false, ParagraphAlignment.CENTER);

        // 4. Bank Details Table (4 rows x 3 cols with merged Chief Examiner sig)
        addBankDetails(document, record);
    }

    private void formatCellMultiline(XWPFTableCell cell, String text, boolean bold, int fontSize, String bgColor, ParagraphAlignment alignment) {
        if (bgColor != null) {
            cell.setColor(bgColor);
        }
        while (cell.getParagraphs().size() > 1) {
            cell.removeParagraph(1);
        }
        XWPFParagraph para = cell.getParagraphs().get(0);
        para.setAlignment(alignment);
        para.setSpacingBefore(0);
        para.setSpacingAfter(0);

        for (int i = para.getRuns().size() - 1; i >= 0; i--) {
            para.removeRun(i);
        }

        if (text != null && text.contains("\n")) {
            String[] lines = text.split("\n");
            for (int i = 0; i < lines.length; i++) {
                if (i > 0) {
                    para = cell.addParagraph();
                    para.setAlignment(alignment);
                    para.setSpacingBefore(0);
                    para.setSpacingAfter(0);
                }
                XWPFRun run = para.createRun();
                run.setText(lines[i]);
                run.setBold(bold);
                run.setFontSize(fontSize);
                run.setFontFamily("Times New Roman");
            }
        } else {
            XWPFRun run = para.createRun();
            run.setText(text != null && !text.isEmpty() ? text : " ");
            run.setBold(bold);
            run.setFontSize(fontSize);
            run.setFontFamily("Times New Roman");
        }
    }

    // ==================== HELPER METHODS ====================

    private void mergeCellsHorizontal(XWPFTable table, int row, int fromCol, int toCol) {
        XWPFTableRow tableRow = table.getRow(row);
        if (tableRow == null) return;
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr tcPr = 
            tableRow.getCell(fromCol).getCTTc().isSetTcPr() ? 
            tableRow.getCell(fromCol).getCTTc().getTcPr() : 
            tableRow.getCell(fromCol).getCTTc().addNewTcPr();
        tcPr.addNewHMerge().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STMerge.RESTART);
        for (int colIndex = fromCol + 1; colIndex <= toCol; colIndex++) {
            XWPFTableCell cell = tableRow.getCell(colIndex);
            if (cell != null) {
                org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr tcPrMerge = 
                    cell.getCTTc().isSetTcPr() ? 
                    cell.getCTTc().getTcPr() : 
                    cell.getCTTc().addNewTcPr();
                tcPrMerge.addNewHMerge().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STMerge.CONTINUE);
            }
        }
    }

    private XWPFTable createNestedTable(XWPFTableCell cell, int rows, int cols) {
        // Directly append the nested table to the cell's XML body.
        // This avoids complex and bug-prone XML cursor manipulation, which is
        // the primary cause of XmlValueDisconnectedException in nested tables.
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTbl ctTbl = cell.getCTTc().addNewTbl();
        XWPFTable table = new XWPFTable(ctTbl, cell);
        initializeTable(table, rows, cols);

        // Every cell in WordprocessingML must end with a paragraph element.
        // Append an empty paragraph after the nested table to satisfy the spec.
        XWPFParagraph extraPara = cell.addParagraph();
        extraPara.setSpacingBefore(0);
        extraPara.setSpacingAfter(0);
        XWPFRun extraRun = extraPara.createRun();
        extraRun.setFontSize(1);
        extraRun.setText("");

        return table;
    }

    private void initializeTable(XWPFTable table, int rows, int cols) {
        if (rows == 0 || cols == 0) return;

        // Ensure table has at least 'rows' rows in the POI structure
        while (table.getRows().size() < rows) {
            table.createRow();
        }

        // Verify and populate cells
        for (int i = 0; i < rows; i++) {
            XWPFTableRow row = table.getRow(i);
            if (row == null) {
                if (i < table.getRows().size()) {
                    row = table.getRows().get(i);
                }
                if (row == null) {
                    row = table.createRow();
                }
            }
            if (row != null) {
                while (row.getTableCells().size() < cols) {
                    row.createCell();
                }
            }
        }
    }

    private void setTableWidth(XWPFTable table, String width) {
        CTTblPr tblPr = table.getCTTbl().getTblPr();
        if (tblPr == null) {
            tblPr = table.getCTTbl().addNewTblPr();
        }
        CTTblWidth tblW = tblPr.isSetTblW() ? tblPr.getTblW() : tblPr.addNewTblW();

        if (width.endsWith("%")) {
            tblW.setType(STTblWidth.PCT);
            tblW.setW(java.math.BigInteger.valueOf(Integer.parseInt(width.replace("%", "")) * 50));
        } else {
            tblW.setType(STTblWidth.DXA);
            tblW.setW(java.math.BigInteger.valueOf(Integer.parseInt(width)));
        }
    }

    private void populateScriptTable(XWPFTable table, ClaimRecord record, String sessionType) {
        List<ScriptDetail> details = record.getScriptDetails().stream()
            .filter(d -> sessionType.equalsIgnoreCase(d.getSessionType()))
            .collect(Collectors.toList());

        boolean isRev = record.isRevaluation();
        int headerFs = isRev ? 8 : 10;
        int subHeaderFs = isRev ? 8 : 10;
        int dataFs = isRev ? 7 : 10;

        // Row 0: Merged cell for FN/AN
        mergeCellsHorizontal(table, 0, 0, 2);
        formatCell(table.getRow(0).getCell(0), sessionType, true, headerFs, null, ParagraphAlignment.CENTER);

        // Row 1: Headers
        formatCell(table.getRow(1).getCell(0), "S.N.", true, subHeaderFs, null, ParagraphAlignment.CENTER);
        formatCell(table.getRow(1).getCell(1), "Subject Code", true, subHeaderFs, null, ParagraphAlignment.CENTER);
        formatCell(table.getRow(1).getCell(2), "No. of Scripts", true, subHeaderFs, null, ParagraphAlignment.CENTER);

        // Display 15 rows for revaluation or 10 rows for standard evaluation
        int rowCount = isRev ? 15 : 10;
        for (int i = 0; i < rowCount; i++) {
            XWPFTableRow row = table.getRow(i + 2);
            String sn = String.valueOf(i + 1);
            String code = "";
            String scripts = "";

            if (i < details.size()) {
                code = details.get(i).getSubjectCode() != null ? details.get(i).getSubjectCode() : "";
                scripts = details.get(i).getNoOfScripts() != null && details.get(i).getNoOfScripts() > 0
                    ? String.valueOf(details.get(i).getNoOfScripts()) : "";
            }

            formatCell(row.getCell(0), sn, false, dataFs, null, ParagraphAlignment.CENTER);
            formatCell(row.getCell(1), code, false, dataFs, null, ParagraphAlignment.CENTER);
            formatCell(row.getCell(2), scripts, false, dataFs, null, ParagraphAlignment.CENTER);
        }

        // Set column widths (Total 4000 twips)
        setRowColWidths(table, new int[]{800, 1800, 1400});
    }

    private void formatCell(XWPFTableCell cell, String text, boolean bold, int fontSize, String bgColor, ParagraphAlignment alignment) {
        if (bgColor != null) {
            cell.setColor(bgColor);
        }

        // Keep only the first paragraph to avoid duplicate spacing
        while (cell.getParagraphs().size() > 1) {
            cell.removeParagraph(1);
        }

        XWPFParagraph para = cell.getParagraphs().get(0);
        para.setAlignment(alignment);
        para.setSpacingBefore(0);
        para.setSpacingAfter(0);

        // Clear existing runs
        for (int i = para.getRuns().size() - 1; i >= 0; i--) {
            para.removeRun(i);
        }

        XWPFRun run = para.createRun();
        run.setText(text != null && !text.isEmpty() ? text : " "); // Use space instead of empty string to force font size
        run.setBold(bold);
        run.setFontSize(fontSize);
        run.setFontFamily("Times New Roman");
    }

    private void setRowColWidths(XWPFTable table, int[] widths) {
        for (XWPFTableRow row : table.getRows()) {
            for (int i = 0; i < widths.length && i < row.getTableCells().size(); i++) {
                XWPFTableCell cell = row.getCell(i);
                cell.setWidth(String.valueOf(widths[i]));
            }
        }
    }

    private void setTableBorders(XWPFTable table) {
        CTTblPr tblPr = table.getCTTbl().getTblPr();
        CTTblBorders borders = tblPr.isSetTblBorders() ? tblPr.getTblBorders() : tblPr.addNewTblBorders();

        setBorder(borders.addNewLeft(), STBorder.SINGLE, 4, "000000");
        setBorder(borders.addNewRight(), STBorder.SINGLE, 4, "000000");
        setBorder(borders.addNewTop(), STBorder.SINGLE, 4, "000000");
        setBorder(borders.addNewBottom(), STBorder.SINGLE, 4, "000000");
        setBorder(borders.addNewInsideH(), STBorder.SINGLE, 4, "000000");
        setBorder(borders.addNewInsideV(), STBorder.SINGLE, 4, "000000");
    }

    private void setOuterTableBorders(XWPFTable table) {
        CTTblPr tblPr = table.getCTTbl().getTblPr();
        CTTblBorders borders = tblPr.isSetTblBorders() ? tblPr.getTblBorders() : tblPr.addNewTblBorders();

        setBorder(borders.addNewLeft(), STBorder.SINGLE, 4, "000000");
        setBorder(borders.addNewRight(), STBorder.SINGLE, 4, "000000");
        setBorder(borders.addNewTop(), STBorder.SINGLE, 4, "000000");
        setBorder(borders.addNewBottom(), STBorder.SINGLE, 4, "000000");
        setBorder(borders.addNewInsideH(), STBorder.NONE, 0, "auto");
        setBorder(borders.addNewInsideV(), STBorder.SINGLE, 4, "000000");
    }

    private void clearTableBorders(XWPFTable table) {
        CTTblPr tblPr = table.getCTTbl().getTblPr();
        CTTblBorders borders = tblPr.isSetTblBorders() ? tblPr.getTblBorders() : tblPr.addNewTblBorders();

        setBorder(borders.addNewLeft(), STBorder.NONE, 0, "auto");
        setBorder(borders.addNewRight(), STBorder.NONE, 0, "auto");
        setBorder(borders.addNewTop(), STBorder.NONE, 0, "auto");
        setBorder(borders.addNewBottom(), STBorder.NONE, 0, "auto");
        setBorder(borders.addNewInsideH(), STBorder.NONE, 0, "auto");
        setBorder(borders.addNewInsideV(), STBorder.NONE, 0, "auto");
    }

    private void setBorder(CTBorder border, STBorder.Enum type, int size, String color) {
        border.setVal(type);
        border.setSz(java.math.BigInteger.valueOf(size));
        border.setSpace(java.math.BigInteger.valueOf(0));
        border.setColor(color);
    }

    private void addCenteredParagraph(XWPFDocument document, String text, int fontSize, boolean bold) {
        XWPFParagraph para = document.createParagraph();
        para.setAlignment(ParagraphAlignment.CENTER);
        para.setSpacingBefore(0);
        para.setSpacingAfter(10);
        XWPFRun run = para.createRun();
        run.setText(text);
        run.setFontSize(fontSize);
        run.setBold(bold);
        run.setFontFamily("Times New Roman");
    }

    private void addParagraph(XWPFDocument document, String text, int fontSize, boolean bold) {
        XWPFParagraph para = document.createParagraph();
        para.setSpacingBefore(0);
        para.setSpacingAfter(10);
        XWPFRun run = para.createRun();
        run.setText(text);
        run.setFontSize(fontSize);
        run.setBold(bold);
        run.setFontFamily("Times New Roman");
    }

    private void addParagraph(XWPFParagraph para, String text, int fontSize, boolean bold, ParagraphAlignment alignment) {
        para.setAlignment(alignment);
        para.setSpacingBefore(0);
        para.setSpacingAfter(10);
        XWPFRun run = para.createRun();
        run.setText(text);
        run.setFontSize(fontSize);
        run.setBold(bold);
        run.setFontFamily("Times New Roman");
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) return "0.00";
        return String.format("%.2f", amount);
    }

    private String safeStr(String s) {
        return s != null ? s : "N/A";
    }
}

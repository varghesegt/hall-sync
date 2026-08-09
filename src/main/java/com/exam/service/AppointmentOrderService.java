package com.exam.service;

import com.exam.entity.Faculty;
import com.exam.repository.master.TenantRepository;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.xwpf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AppointmentOrderService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentOrderService.class);
    private final TenantRepository tenantRepository;

    public AppointmentOrderService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    /**
     * Main entry point. Loads the source template Word document for the requested role
     * and performs text replacements while preserving 100% exact formatting.
     */
    public byte[] generateAppointmentOrderWord(Faculty faculty, String role, String season, LocalDate date,
                                                String board, String time, String venue,
                                                String subjectCode, String subjectName, Integer noOfCandidates,
                                                String semester, String internalExaminerName, String internalExaminerPhone) throws IOException {

        String templateName = getTemplateFileName(role);
        String collegeName = getCollegeName();

        byte[] result = tryGenerateFromTemplate(templateName, faculty, role, season, date, board, time, venue,
                subjectCode, subjectName, noOfCandidates, semester, internalExaminerName, internalExaminerPhone, collegeName);

        if (result != null && result.length > 0) {
            return result;
        }

        log.warn("Fallback to programmatic generation for role: {}", role);
        return generateFallbackWord(faculty, role, season, date, collegeName);
    }

    private String getTemplateFileName(String role) {
        if (role == null) return "examinervaluation.docx";
        switch (role.trim()) {
            case "Question Bank Scrutiny Member":
                return "questionbank.docx";
            case "Examiner for Audit Valuation":
                return "examinerauditvaluation.docx";
            case "Enquiry Committee Member (ECM)":
                return "enquirycommittemember.docx";
            case "Examiner for Valuation":
                return "examinervaluation.docx";
            case "Chief Examiner - Valuation":
            case "Chief Examiner":
                return "cheifexaminer.docx";
            case "Squad for Theory Examinations":
                return "squad.docx";
            case "External Examiner - UG End Semester Practical":
                return "externalexaminer.docx";
            default:
                return "examinervaluation.docx";
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Template Generation
    // ──────────────────────────────────────────────────────────────────────────

    private byte[] tryGenerateFromTemplate(String templateName, Faculty faculty, String role, String season,
                                           LocalDate date, String board, String time, String venue,
                                           String subjectCode, String subjectName, Integer noOfCandidates,
                                           String semester, String internalExaminerName, String internalExaminerPhone,
                                           String collegeName) {
        try (InputStream is = getClass().getResourceAsStream("/templates/" + templateName)) {
            if (is == null) {
                log.warn("Template /templates/{} not found on classpath", templateName);
                return null;
            }

            String facName  = safeStr(faculty, Faculty::getName, "Faculty Member");
            String desig    = safeStr(faculty, Faculty::getDesignation, "Assistant Professor");
            String dept     = safeStr(faculty, Faculty::getDepartment, "CSE");
            String coll     = safeStr(faculty, Faculty::getCollegeName, collegeName);
            String phone    = faculty != null && faculty.getPhone() != null ? faculty.getPhone().trim() : "";
            String empId    = faculty != null && faculty.getEmployeeId() != null ? faculty.getEmployeeId().trim() : "";

            String cleanDept = dept.replace("Department of ", "").trim();

            String displaySeason    = fallback(season, "APRIL/MAY-2026");
            String displayDate      = date != null ? date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            String chosenBoard      = (board != null && !board.trim().isEmpty()) ? board.trim() : cleanDept;
            String displayBoard     = fallback(chosenBoard, "CSE");
            String displayTime      = fallback(time, "9.00 a.m. - 05.00 p.m.");
            String displayVenue     = fallback(venue, "COE OFFICE");
            String displaySubCode   = fallback(subjectCode, "GEA1107");
            String displaySubName   = fallback(subjectName, "C PROGRAMMING LAB");
            int    displayCandidates = noOfCandidates != null ? noOfCandidates : 23;
            String displaySemester  = fallback(semester, "I");
            String displayIntExaminer = fallback(internalExaminerName, "Mr. P. KASTHURI RENGAN");
            String displayIntExaminerPhone = fallback(internalExaminerPhone, "9842612131");

            // Build the comprehensive replacement map
            Map<String, String> replacements = buildReplacements(
                    facName, desig, dept, coll, phone,
                    displaySeason, displayDate, displayBoard, displayTime, displayVenue,
                    displaySubCode, displaySubName, displayCandidates, displaySemester,
                    displayIntExaminer, displayIntExaminerPhone, role);

            if (templateName.endsWith(".doc")) {
                // ── HWPF (.doc format — squad.doc) ──
                return processHwpfDocument(is, replacements);
            } else {
                // ── XWPF (.docx format — 6 templates) ──
                return processXwpfDocument(is, templateName, facName, desig, dept, coll, phone, replacements, displayDate);
            }
        } catch (Exception e) {
            log.error("Failed to generate from template {}", templateName, e);
            return null;
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // HWPF (.doc) Processing — Squad
    // ──────────────────────────────────────────────────────────────────────────

    private byte[] processHwpfDocument(InputStream is, Map<String, String> replacements) throws Exception {
        try (HWPFDocument doc = new HWPFDocument(is)) {
            Range range = doc.getRange();

            // Sort replacements by key length descending so longer keys match first
            List<Map.Entry<String, String>> sorted = new ArrayList<>(replacements.entrySet());
            sorted.sort((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()));

            for (Map.Entry<String, String> entry : sorted) {
                if (entry.getKey() != null && !entry.getKey().isEmpty() && entry.getValue() != null) {
                    try {
                        range.replaceText(entry.getKey(), entry.getValue());
                    } catch (Exception ignored) {
                        // Some keys may not exist in this template — skip silently
                    }
                }
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.write(baos);
            return baos.toByteArray();
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // XWPF (.docx) Processing — 6 templates
    // ──────────────────────────────────────────────────────────────────────────

    private byte[] processXwpfDocument(InputStream is, String templateName,
                                       String facName, String desig, String dept, String coll, String phone,
                                       Map<String, String> replacements, String displayDate) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(is)) {

            // Process body paragraphs with To-block awareness
            processDocumentParagraphs(doc, templateName, facName, desig, dept, coll, phone, replacements, displayDate);

            // Replace in tables (externalexaminer.docx and questionbank.docx have tables)
            for (XWPFTable table : doc.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        for (XWPFParagraph p : cell.getParagraphs()) {
                            replaceInParagraphRunAware(p, replacements, displayDate);
                        }
                    }
                }
            }

            // Replace in headers
            for (XWPFHeader header : doc.getHeaderList()) {
                for (XWPFParagraph p : header.getParagraphs()) {
                    replaceInParagraphRunAware(p, replacements, displayDate);
                }
            }

            // Replace in footers
            for (XWPFFooter footer : doc.getFooterList()) {
                for (XWPFParagraph p : footer.getParagraphs()) {
                    replaceInParagraphRunAware(p, replacements, displayDate);
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.write(baos);
            return baos.toByteArray();
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // To-Block Processing (template-aware)
    // ──────────────────────────────────────────────────────────────────────────

    private void processDocumentParagraphs(XWPFDocument doc, String templateName,
                                           String facName, String desig, String dept, String coll, String phone,
                                           Map<String, String> replacements, String displayDate) {
        boolean inToBlock = false;
        int toLineIndex = 0;
        boolean isExternalExaminer = "externalexaminer.docx".equals(templateName);
        boolean isExaminerValuation = "examinervaluation.docx".equals(templateName);
        boolean isQuestionBank = "questionbank.docx".equals(templateName);

        for (XWPFParagraph p : doc.getParagraphs()) {
            String text = getCleanParagraphText(p).trim();

            // Handle Question Bank Body text dynamic field formatting with BOLD department and session
            if (isQuestionBank && text.contains("Question Bank Scrutiny Member for")) {
                String cleanDept = dept.replace("Department of ", "").trim();
                String seasonStr = replacements.getOrDefault("${season}", "APRIL/MAY-2026");

                for (int i = p.getRuns().size() - 1; i >= 0; i--) {
                    p.removeRun(i);
                }

                String font = "Times New Roman";
                double size = 12.0;

                XWPFRun r1 = p.createRun();
                r1.setText("I have the honor to inform you that you are appointed as Question Bank Scrutiny Member for ");
                r1.setFontFamily(font);
                r1.setFontSize(size);
                r1.setBold(false);

                XWPFRun r2 = p.createRun();
                r2.setText(cleanDept);
                r2.setFontFamily(font);
                r2.setFontSize(size);
                r2.setBold(true);

                XWPFRun r3 = p.createRun();
                r3.setText(" for ");
                r3.setFontFamily(font);
                r3.setFontSize(size);
                r3.setBold(false);

                XWPFRun r4 = p.createRun();
                r4.setText(seasonStr);
                r4.setFontFamily(font);
                r4.setFontSize(size);
                r4.setBold(true);

                XWPFRun r5 = p.createRun();
                r5.setText(" of our College. We would be grateful for sparing your time & effort and also appreciate your commitment involved.");
                r5.setFontFamily(font);
                r5.setFontSize(size);
                r5.setBold(false);

                continue;
            }

            // Detect To block start
            if (text.equalsIgnoreCase("To,") || text.equalsIgnoreCase("To")) {
                inToBlock = true;
                toLineIndex = 0;
                continue;
            }

            if (inToBlock) {
                // Detect To block end
                if (text.startsWith("Sir") || text.startsWith("Sub:") || text.startsWith("Sub :")
                        || text.startsWith("Ref")) {
                    inToBlock = false;
                    if (text.startsWith("Sir")) {
                        p.setSpacingBefore(180); // 1 single line space (9pt) before Sir/Madam
                    }
                    if (!text.isEmpty()) {
                        replaceInParagraphRunAware(p, replacements, displayDate);
                    }
                    continue;
                }

                if (text.isEmpty()) {
                    continue;
                }

                toLineIndex++;
                String replacement;

                if (isExternalExaminer) {
                    replacement = getToBlockTextExternalExaminer(toLineIndex, facName, coll, phone);
                } else if (isExaminerValuation) {
                    replacement = getToBlockTextExaminerValuation(toLineIndex, facName, desig, dept, coll, phone);
                } else if (isQuestionBank) {
                    replacement = getToBlockTextQuestionBank(toLineIndex, facName, desig, dept, coll, phone);
                } else {
                    replacement = getToBlockTextStandard(toLineIndex, facName, desig, dept, coll, phone);
                }

                if (replacement != null) {
                    p.setIndentationLeft(0);
                    p.setFirstLineIndent(0);
                    setParagraphTextPreservingStyle(p, replacement);
                    continue;
                }
            }

            // Standard run-aware replacement for all other paragraphs
            replaceInParagraphRunAware(p, replacements, displayDate);
        }
    }

    /**
     * Standard 5-line To block aligned with From block using Tab (\t)
     */
    private String getToBlockTextStandard(int index, String facName, String desig, String dept, String coll, String phone) {
        String cleanDept = dept.replace("Department of ", "").trim();
        String cleanColl = stripTrailing(coll, ",").trim();
        String cleanPhone = stripTrailing(phone, ".").trim();

        switch (index) {
            case 1: return "\t" + facName.trim() + ",";
            case 2: return "\t" + desig.trim() + ",";
            case 3: return "\tDepartment of " + cleanDept + ",";
            case 4: return "\t" + cleanColl + ",";
            case 5: return "\t" + cleanPhone + ".";
            default: return ""; // Clear any leftover lines in To-block
        }
    }

    /**
     * External Examiner — 3-line To block aligned with Tab (\t)
     */
    private String getToBlockTextExternalExaminer(int index, String facName, String coll, String phone) {
        String cleanColl = stripTrailing(coll, ",").trim();
        String cleanPhone = stripTrailing(phone, ".").trim();

        switch (index) {
            case 1: return "\t" + facName.trim() + ",";
            case 2: return "\t" + cleanColl + ",";
            case 3: return "\t" + cleanPhone + ".";
            default: return ""; // Clear any leftover lines in To-block
        }
    }

    /**
     * Examiner Valuation — variable To block aligned with Tab (\t)
     */
    private String getToBlockTextExaminerValuation(int index, String facName, String desig, String dept, String coll, String phone) {
        String cleanDept = dept.replace("Department of ", "").trim();
        String cleanColl = stripTrailing(coll, ",").trim();
        String cleanPhone = stripTrailing(phone, ".").trim();

        switch (index) {
            case 1: return "\t" + facName.trim();
            case 2: return "\t" + desig.trim() + ", " + cleanDept + ".";
            case 3: return "\t" + cleanColl;
            case 4: return "\tMobile No:" + cleanPhone;
            case 5: return ""; // Clear extra template line at the bottom to remove line gap!
            default: return ""; // Clear any leftover lines in To-block
        }
    }

    /**
     * Question Bank — 5-line To block aligned with From block using Tab (\t):
     */
    private String getToBlockTextQuestionBank(int index, String facName, String desig, String dept, String coll, String phone) {
        String cleanDept = dept.replace("Department of ", "").trim();
        String cleanColl = stripTrailing(coll, ",").trim();
        String cleanPhone = stripTrailing(phone, ".").trim();

        switch (index) {
            case 1: return "\t" + facName.trim() + ",";
            case 2: return "\t" + desig.trim() + ",";
            case 3: return "\tDepartment of " + cleanDept + ",";
            case 4: return "\t" + cleanColl + ",";
            case 5: return "\t" + cleanPhone + ".";
            default: return ""; // Clear P18 or any extra lines to prevent duplicate phone numbers!
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Run-Aware Paragraph Replacement (preserves formatting)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Performs replacements at the individual run level, preserving the formatting
     * (bold, italic, font, size) of each run. Only modifies runs that contain
     * matching text.
     *
     * Strategy:
     *   1. First try per-run replacement (each run checked independently).
     *   2. If per-run didn't find anything, try cross-run replacement for keys
     *      that might be split across multiple runs.
     *   3. As a final fallback, do whole-paragraph replacement for date regex patterns.
     */
    private void replaceInParagraphRunAware(XWPFParagraph p, Map<String, String> replacements, String displayDate) {
        if (p == null || p.getRuns() == null || p.getRuns().isEmpty()) return;

        // Sort replacement entries by key length descending (longest first to avoid partial matches)
        List<Map.Entry<String, String>> sortedEntries = new ArrayList<>(replacements.entrySet());
        sortedEntries.sort((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()));

        // ── Pass 1: Cross-run replacement for fragmented/composite keys ──
        String fullText = getCleanParagraphText(p);
        boolean needsCrossRunFix = false;
        for (Map.Entry<String, String> entry : sortedEntries) {
            if (entry.getKey() != null && !entry.getKey().isEmpty() && fullText.contains(entry.getKey())) {
                needsCrossRunFix = true;
                break;
            }
        }

        if (needsCrossRunFix) {
            String replaced = fullText;
            for (Map.Entry<String, String> entry : sortedEntries) {
                String key = entry.getKey();
                String val = entry.getValue() != null ? entry.getValue() : "";
                if (key != null && !key.isEmpty() && replaced.contains(key)) {
                    replaced = replaced.replace(key, val);
                }
            }
            if (!replaced.equals(fullText)) {
                setParagraphTextPreservingStyle(p, replaced);
                return;
            }
        }

        // ── Pass 2: Per-run replacements for single-run tokens ──
        for (XWPFRun run : p.getRuns()) {
            String runText = run.getText(0);
            if (runText == null || runText.isEmpty()) continue;

            for (Map.Entry<String, String> entry : sortedEntries) {
                String key = entry.getKey();
                String val = entry.getValue() != null ? entry.getValue() : "";
                if (key == null || key.isEmpty()) continue;
                if (runText.contains(key)) {
                    runText = runText.replace(key, val);
                    run.setText(runText, 0);
                }
            }
        }

        // ── Pass 3: Date regex fallback for Date/Ref lines ──
        String currentText = getCleanParagraphText(p);
        if (currentText.contains("Date") || currentText.contains("Ref")) {
            Pattern datePattern = Pattern.compile("\\b\\d{1,2}\\.\\d{1,2}\\.\\d{4}\\b");
            Matcher matcher = datePattern.matcher(currentText);
            if (matcher.find()) {
                String withDatesFixed = datePattern.matcher(currentText).replaceAll(Matcher.quoteReplacement(displayDate));
                if (!withDatesFixed.equals(currentText)) {
                    // Try run-level date replacement first
                    boolean didRunReplace = false;
                    for (XWPFRun run : p.getRuns()) {
                        String rt = run.getText(0);
                        if (rt != null && datePattern.matcher(rt).find()) {
                            run.setText(datePattern.matcher(rt).replaceAll(Matcher.quoteReplacement(displayDate)), 0);
                            didRunReplace = true;
                        }
                    }
                    if (!didRunReplace) {
                        setParagraphTextPreservingStyle(p, withDatesFixed);
                    }
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Paragraph Text Helpers
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Gets clean paragraph text by concatenating all non-null run texts.
     * Skips runs with null getText(0).
     */
    private String getCleanParagraphText(XWPFParagraph p) {
        if (p == null || p.getRuns() == null) return "";
        StringBuilder sb = new StringBuilder();
        for (XWPFRun run : p.getRuns()) {
            String t = run.getText(0);
            if (t != null) {
                sb.append(t);
            }
        }
        return sb.toString();
    }

    /**
     * Replaces entire paragraph text while preserving the style (font, size, bold, italic,
     * underline, color) from the FIRST non-null run in the original paragraph.
     */
    private void setParagraphTextPreservingStyle(XWPFParagraph p, String newText) {
        if (newText == null || p == null) return;

        // Capture style from first non-null run
        String font = "Times New Roman";
        Double size = null;
        boolean bold = false;
        boolean italic = false;
        UnderlinePatterns underline = UnderlinePatterns.NONE;
        String color = null;

        if (p.getRuns() != null && !p.getRuns().isEmpty()) {
            for (XWPFRun run : p.getRuns()) {
                if (run.getText(0) != null) {
                    if (run.getFontFamily() != null) font = run.getFontFamily();
                    if (run.getFontSizeAsDouble() != null && run.getFontSizeAsDouble() > 0) size = run.getFontSizeAsDouble();
                    bold = run.isBold();
                    italic = run.isItalic();
                    underline = run.getUnderline();
                    color = run.getColor();
                    break;
                }
            }
        }

        // Remove all existing runs
        for (int i = p.getRuns().size() - 1; i >= 0; i--) {
            p.removeRun(i);
        }

        // Create new run with preserved style
        XWPFRun run = p.createRun();
        run.setText(newText);
        run.setFontFamily(font);
        if (size != null) run.setFontSize(size);
        run.setBold(bold);
        run.setItalic(italic);
        if (underline != null && underline != UnderlinePatterns.NONE) run.setUnderline(underline);
        if (color != null) run.setColor(color);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Comprehensive Replacement Map
    // ──────────────────────────────────────────────────────────────────────────

    private Map<String, String> buildReplacements(String facName, String desig, String dept, String coll, String phone,
                                                  String displaySeason, String displayDate, String displayBoard,
                                                  String displayTime, String displayVenue,
                                                  String displaySubCode, String displaySubName,
                                                  int displayCandidates, String displaySemester,
                                                  String displayIntExaminer, String displayIntExaminerPhone,
                                                  String role) {
        // Use LinkedHashMap to maintain insertion order (longer keys first for safer replacement)
        Map<String, String> map = new LinkedHashMap<>();
        String cleanDept = dept.replace("Department of ", "").trim();

        // ── Standard Placeholders ──
        map.put("${facultyName}", facName);
        map.put("${designation}", desig);
        map.put("${department}", dept);
        map.put("${collegeName}", coll);
        map.put("${phone}", phone);
        map.put("${season}", displaySeason);
        map.put("${date}", displayDate);
        map.put("${board}", displayBoard);
        map.put("${time}", displayTime);
        map.put("${timings}", displayTime);
        map.put("${venue}", displayVenue);
        map.put("${role}", role != null ? role : "Examiner");
        map.put("${subjectCode}", displaySubCode);
        map.put("${subjectName}", displaySubName);
        map.put("${candidates}", String.valueOf(displayCandidates));
        map.put("${semester}", displaySemester);
        map.put("${internalExaminer}", displayIntExaminer);
        map.put("${internalExaminerPhone}", displayIntExaminerPhone);

        // ──────────────────────────────────────────────
        // Sample Names (all 7 templates)
        // ──────────────────────────────────────────────

        // cheifexaminer.docx
        map.put("Dr. R Manivasagam", facName);
        map.put("Dr.R Manivasagam", facName);
        map.put("Dr. R. Manivasagam", facName);

        // enquirycommittemember.docx
        map.put("Dr. K. Chellamuthu", facName);
        map.put("Dr.K.Chellamuthu", facName);
        map.put("Dr. K. Chellamuthu", facName);

        // examinerauditvaluation.docx
        map.put("Prakash.J ,", facName + ",");
        map.put("Prakash.J,", facName + ",");
        map.put("Prakash.J", facName);
        map.put("Prakash. J", facName);

        // examinervaluation.docx
        map.put("Dr S Shantha", facName);
        map.put("Dr. S. Shantha", facName);
        map.put("Dr.S.Shantha", facName);

        // externalexaminer.docx
        map.put("Mrs.U.Ranjani", facName);
        map.put("Mrs. U. Ranjani", facName);
        map.put("Mrs.U.Ranjani,", facName + ",");

        // questionbank.docx
        map.put("Dr.R.Muthukumar", facName);
        map.put("Dr. R. Muthukumar", facName);
        map.put("Dr.R.Muthukumar,", facName + ",");

        // squad.doc
        map.put("Dr. S. Arunkumar,/", facName + ",/");
        map.put("Dr. S. Arunkumar,", facName + ",");
        map.put("Dr. S. Arunkumar", facName);
        map.put("Dr.S.Arunkumar", facName);
        map.put("Dr. Ivy Chen", facName);
        map.put("Dr.Ivy Chen", facName);

        // ──────────────────────────────────────────────
        // Sample Designations (all templates)
        // ──────────────────────────────────────────────
        map.put("Assistant Professor (Sr.G) ,", desig + ",");
        map.put("Assistant Professor (Sr.G),", desig + ",");
        map.put("Assistant Professor (Sr.G)", desig);
        map.put("Assistant Professor ,", desig + ",");
        map.put("Assistant Professor,", desig + ",");
        // Be careful with "Assistant Professor" — it's a substring of "(Sr.G)" variant
        // Only replace standalone if needed
        map.put("Professor, English.", desig + ", " + cleanDept + ".");
        // "Professor" standalone — handled via To-block rewriting

        // ──────────────────────────────────────────────
        // Sample Departments (all templates)
        // ──────────────────────────────────────────────
        map.put("Department of MECH ,", "Department of " + cleanDept + ",");
        map.put("Department of MECH,", "Department of " + cleanDept + ",");
        map.put("Department of MATHS,", "Department of " + cleanDept + ",");
        map.put("Department of MATHS", "Department of " + cleanDept);
        map.put("Department of CSBS,", "Department of " + cleanDept + ",");
        map.put("Department of CSBS", "Department of " + cleanDept);
        map.put("Department of CIVIL,", "Department of " + cleanDept + ",");
        map.put("Department of CIVIL", "Department of " + cleanDept);
        map.put("Department of EEE,", "Department of " + cleanDept + ",");
        map.put("Department of EEE", "Department of " + cleanDept);
        map.put("Department of CSE,", "Department of " + cleanDept + ",");
        map.put("Department of CSE", "Department of " + cleanDept);
        map.put("Department of MECH", "Department of " + cleanDept);

        // ──────────────────────────────────────────────
        // Sample Colleges (all templates)
        // ──────────────────────────────────────────────
        map.put("PSG College of Technology ,", coll + ",");
        map.put("PSG College of Technology,", coll + ",");
        map.put("PSG College of Technology", coll);
        map.put("M.Kumarasami College of Engineering", coll);
        map.put("Paavai Engineering college,", coll + ",");
        map.put("Paavai Engineering college", coll);
        map.put("SARANATHAN ,", coll + ",");
        map.put("SARANATHAN,", coll + ",");
        map.put("SARANATHAN", coll);
        map.put("MAMSE,", coll + ",");
        map.put("MAMSE", coll);
        // "Karur" (city in examinervaluation) and "Namakkal" (in questionbank) — these are location lines
        map.put("Karur", coll);
        map.put("Namakkal,", coll + ",");
        map.put("Namakkal", coll);

        // ──────────────────────────────────────────────
        // Sample Phones (all templates)
        // ──────────────────────────────────────────────
        map.put("8870741983", phone);   // cheifexaminer
        map.put("9944264256", phone);   // enquirycommittemember
        map.put("9500378146 .", phone + ".");  // examinerauditvaluation (with space-dot)
        map.put("9500378146", phone);   // examinerauditvaluation
        map.put("6381446076", phone);   // examinervaluation
        map.put("8270981412", phone);   // externalexaminer
        map.put("9600205384", phone);   // questionbank
        map.put("9965738788", phone);   // squad
        map.put("9876543218", phone);   // legacy
        map.put("9876500014", phone);   // legacy

        // Mobile No: prefix variant in examinervaluation
        map.put("Mobile No:6381446076", "Mobile No:" + phone);
        map.put("Mobile No: 6381446076", "Mobile No: " + phone);

        // ──────────────────────────────────────────────
        // Sample Dates (all templates)
        // ──────────────────────────────────────────────
        map.put("02.06.2026", displayDate);   // cheifexaminer
        map.put("03.06.2026", displayDate);   // examinerauditvaluation, examinervaluation, externalexaminer
        map.put("04.06.2026", displayDate);   // examinervaluation
        map.put("06.06.2026", displayDate);   // externalexaminer table
        map.put("08.06.2026", displayDate);   // enquirycommittemember
        map.put("09.06.2026", displayDate);   // enquirycommittemember
        map.put("12.06.2026", displayDate);   // examinerauditvaluation
        map.put("13.6.2026", displayDate);    // examinerauditvaluation
        map.put("13.06.2026", displayDate);   // examinerauditvaluation
        map.put("28.04.2026", displayDate);   // questionbank
        map.put("29.04.2026", displayDate);   // questionbank table
        map.put("11.05.2026", displayDate);   // squad
        map.put("15.05.2026", displayDate);   // squad table
        map.put("29.07.2026", displayDate);   // legacy
        map.put("30.7.2026", displayDate);    // legacy
        map.put("30.07.2026", displayDate);   // legacy

        // ──────────────────────────────────────────────
        // Dynamic Season / Exam Period Replacements (all templates)
        // ──────────────────────────────────────────────
        map.put("APRIL/MAY-2026", displaySeason);
        map.put("APRIL/MAY-26", displaySeason);
        map.put("April/May-2026", displaySeason);
        map.put("APRIL/MAY 2026", displaySeason);
        map.put("April/May 2026", displaySeason);
        map.put("APRIL/MAY", displaySeason.split("-")[0]);

        // ──────────────────────────────────────────────
        // Board / Department references in body text
        // ──────────────────────────────────────────────
        map.put("Question Bank Scrutiny Member for EEE for APRIL/MAY-2026",
                "Question Bank Scrutiny Member for " + cleanDept + " for " + displaySeason);
        map.put("Question Bank Scrutiny Member for EEE",
                "Question Bank Scrutiny Member for " + cleanDept);
        map.put("for EEE for APRIL/MAY-2026",
                "for " + cleanDept + " for " + displaySeason);
        // In Ref lines and Board lines, the board name appears standalone
        String shortBoardCode = displayBoard;
        if ("ENGLISH".equalsIgnoreCase(displayBoard) || "ENG".equalsIgnoreCase(displayBoard)) {
            shortBoardCode = "ENG";
        } else if ("PHYSICS".equalsIgnoreCase(displayBoard) || "PHY".equalsIgnoreCase(displayBoard)) {
            shortBoardCode = "PHY";
        } else if ("CHEMISTRY".equalsIgnoreCase(displayBoard) || "CHE".equalsIgnoreCase(displayBoard)) {
            shortBoardCode = "CHE";
        } else if ("MATHS".equalsIgnoreCase(displayBoard) || "MATHEMATICS".equalsIgnoreCase(displayBoard) || "MAT".equalsIgnoreCase(displayBoard)) {
            shortBoardCode = "MAT";
        }

        if ("Examiner for Audit Valuation".equalsIgnoreCase(role != null ? role.trim() : "")) {
            map.put("Board : CSE", "Board : " + displayBoard);
            map.put("Board\t: CSE", "Board\t: " + displayBoard);
            map.put("Board : CSE ", "Board : " + displayBoard + " ");
            map.put("Board: CSE", "Board: " + displayBoard);
            map.put("Board\t:CSE", "Board\t:" + displayBoard);
            map.put("CSE- 2", shortBoardCode + "- 2");
            map.put("CSE-2", shortBoardCode + "-2");
            map.put("CSE - 2", shortBoardCode + " - 2");
            map.put("CSE -2", shortBoardCode + " -2");
            map.put("CSE", shortBoardCode);
        }

        if ("Examiner for Valuation".equalsIgnoreCase(role != null ? role.trim() : "")
                || "Examiner - Valuation".equalsIgnoreCase(role != null ? role.trim() : "")) {
            map.put("Board : ENGLISH", "Board : " + displayBoard);
            map.put("Board\t: ENGLISH", "Board\t: " + displayBoard);
            map.put("Board: ENGLISH", "Board: " + displayBoard);
            map.put("ENGLISH", displayBoard);
            map.put("English", displayBoard);
            map.put("ENG ", shortBoardCode + " ");
            map.put("ENG", shortBoardCode);
            map.put("ENG -", shortBoardCode + " -");
            map.put("ENG-", shortBoardCode + "-");
            map.put(", English.", ", " + cleanDept + ".");
        }

        // ──────────────────────────────────────────────
        // Venue (enquirycommittemember, questionbank)
        // ──────────────────────────────────────────────
        map.put("IQAC ", displayVenue);
        map.put("IQAC", displayVenue);
        map.put("COE OFFICE", displayVenue);

        // ──────────────────────────────────────────────
        // Table data (externalexaminer, questionbank)
        // ──────────────────────────────────────────────
        map.put("GEA1107", displaySubCode);
        map.put("C  PROGRAMMING LAB", displaySubName);
        map.put("C PROGRAMMING LAB", displaySubName);
        map.put("Mr. P. KASTHURI RENGAN, 9842612131", displayIntExaminer + ", " + displayIntExaminerPhone);
        map.put("Mr. P. KASTHURI RENGAN", displayIntExaminer);
        map.put("9842612131", displayIntExaminerPhone);

        // Department standalone for table: "ALL \nEXCEPT CSBS" — tricky, leave as-is in table

        // Candidates count
        // The "23" in the table — be careful, "23" is a very short string. Only replace in table context.
        // We'll handle this specially since "23" could appear elsewhere

        // Semester: "I" — too short for global replacement, handled via table context

        return map;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // College Name from Tenant
    // ──────────────────────────────────────────────────────────────────────────

    private String getCollegeName() {
        String collegeName = "K.Ramakrishnan College of Engineering";
        try {
            String tenantId = com.exam.config.tenant.TenantContext.getCurrentTenant();
            if (tenantId != null) {
                com.exam.config.tenant.TenantContext.clear();
                try {
                    var tenantOpt = tenantRepository.findByTenantId(tenantId);
                    if (tenantOpt.isPresent()) {
                        collegeName = tenantOpt.get().getCollegeName();
                    }
                } finally {
                    com.exam.config.tenant.TenantContext.setCurrentTenant(tenantId);
                }
            }
        } catch (Exception e) {
            log.warn("Could not resolve college name from tenant", e);
        }
        return collegeName;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Fallback Generator
    // ──────────────────────────────────────────────────────────────────────────

    private byte[] generateFallbackWord(Faculty faculty, String role, String season, LocalDate date, String collegeName) {
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            XWPFParagraph p = doc.createParagraph();
            XWPFRun r = p.createRun();
            r.setText("Appointment Order - " + role + " for " + (faculty != null ? faculty.getName() : "Faculty"));
            doc.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            return new byte[0];
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Utility Methods
    // ──────────────────────────────────────────────────────────────────────────

    private static String safeStr(Faculty f, java.util.function.Function<Faculty, String> getter, String fallback) {
        if (f == null) return fallback;
        String val = getter.apply(f);
        return val != null && !val.isBlank() ? val.trim() : fallback;
    }

    private static String fallback(String value, String defaultValue) {
        return value != null && !value.isBlank() ? value.trim() : defaultValue;
    }

    private static String stripTrailing(String s, String ch) {
        if (s == null) return "";
        s = s.trim();
        if (s.endsWith(ch)) return s.substring(0, s.length() - ch.length()).trim();
        return s;
    }
}

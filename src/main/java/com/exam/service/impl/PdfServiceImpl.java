package com.exam.service.impl;

import com.exam.dto.PdfAllocationView;
import com.exam.exception.ResourceNotFoundException;
import com.exam.entity.AllocationBatch;
import com.exam.entity.ExamSession;
import com.exam.repository.AllocationBatchRepository;
import com.exam.repository.AllocationRepository;
import com.exam.service.PdfService;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfDocumentInfo;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.WriterProperties;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.UUID;
import java.util.Objects;
import java.util.Collections;
import java.util.stream.Collectors;

@Service
public class PdfServiceImpl implements PdfService {

    private static final Logger logger = LoggerFactory.getLogger(PdfServiceImpl.class);

    private final AllocationRepository allocationRepo;
    private final AllocationBatchRepository batchRepo;

    public PdfServiceImpl(AllocationRepository allocationRepo, AllocationBatchRepository batchRepo) {
        this.allocationRepo = allocationRepo;
        this.batchRepo = batchRepo;
    }

    @Override
    public void generateHallPdf(AllocationBatch batch, List<PdfAllocationView> allocations, java.io.OutputStream outStream) {
        ExamSession session = batch.getExamSession();
        
        Map<String, List<PdfAllocationView>> groupedByHall = allocations.stream()
                .sorted(Comparator.comparing(PdfAllocationView::hallName))
                .collect(Collectors.groupingBy(PdfAllocationView::hallName));

        WriterProperties props = new WriterProperties().setFullCompressionMode(true);
        try (PdfWriter writer = new PdfWriter(outStream, props)) {
            writer.setCloseStream(false);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, PageSize.A4);
            document.setMargins(25, 30, 25, 30);

            PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont regFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            List<String> sortedHalls = groupedByHall.keySet().stream().sorted().toList();

            int hallCounter = 0;
            for (String hallName : sortedHalls) {
                hallCounter++;
                List<PdfAllocationView> hallAllocations = groupedByHall.get(hallName);
                
                // --- Hall Slip Header ---
                document.add(new Paragraph("Office of the Controller of Examinations")
                        .setFont(regFont).setFontSize(9).setTextAlignment(TextAlignment.CENTER).setMargin(0));
                document.add(new Paragraph(resolveCollegeName().toUpperCase())
                        .setFont(boldFont).setFontSize(12).setTextAlignment(TextAlignment.CENTER).setMargin(0));
                document.add(new Paragraph("Seating Arrangement")
                        .setFont(regFont).setFontSize(10).setTextAlignment(TextAlignment.CENTER).setMarginTop(2).setMarginBottom(5));

                // --- Metadata Line (Hall BOX, Date, Session) ---
                float[] metaWidths = {2, 2, 2};
                Table metaTable = new Table(UnitValue.createPercentArray(metaWidths)).useAllAvailableWidth().setMarginBottom(5);
                
                // Hall Name in a Bold Box
                Cell hallCell = new Cell().add(new Paragraph(hallName).setFont(boldFont).setFontSize(22).setTextAlignment(TextAlignment.CENTER))
                        .setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE)
                        .setPadding(5);
                metaTable.addCell(hallCell);

                // Date and Session info
                String dateStr = session.getExamDate().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                Cell dateCell = new Cell().add(new Paragraph("Date: " + dateStr).setFont(boldFont).setFontSize(10))
                        .setBorder(Border.NO_BORDER)
                        .setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE)
                        .setPaddingLeft(15);
                metaTable.addCell(dateCell);

                Cell sessionCell = new Cell().add(new Paragraph("Session: " + session.getSession()).setFont(boldFont).setFontSize(10).setTextAlignment(TextAlignment.RIGHT))
                        .setBorder(Border.NO_BORDER)
                        .setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE);
                metaTable.addCell(sessionCell);
                
                document.add(metaTable);

                // Determine dimensions dynamically from hallAllocations
                int maxRow = 5;
                int maxColIndex = 4; // default to V
                String[] engineColNames = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII"};
                String[] rowLabels = {"I Row", "II Row", "III Row", "IV Row", "V Row", "VI Row", "VII Row", "VIII Row"};
                
                for (PdfAllocationView a : hallAllocations) {
                    if (a.seatRow() != null && a.seatRow() > maxRow) maxRow = a.seatRow();
                    if (a.seatCol() != null) {
                        for (int i = 0; i < engineColNames.length; i++) {
                            if (engineColNames[i].equalsIgnoreCase(a.seatCol().trim()) && i > maxColIndex) {
                                maxColIndex = i;
                            }
                        }
                    }
                }
                int colCount = Math.min(maxColIndex + 1, engineColNames.length);

                // --- Grid Table (Dynamic Columns) ---
                document.add(new Paragraph("REGISTER NO. OF THE CANDIDATES")
                        .setFont(boldFont).setFontSize(10).setTextAlignment(TextAlignment.CENTER).setMargin(2));

                float[] gridWidths = new float[colCount * 2];
                for (int i = 0; i < colCount; i++) {
                    gridWidths[i * 2] = 0.45f;
                    gridWidths[i * 2 + 1] = 2.55f;
                }
                Table gridTable = new Table(UnitValue.createPercentArray(gridWidths)).useAllAvailableWidth();
                gridTable.setFixedLayout();
                
                // Column Headers
                for (int c = 0; c < colCount; c++) {
                    gridTable.addHeaderCell(new Cell().add(new Paragraph("S\nN\nO").setFont(boldFont).setFontSize(7).setTextAlignment(TextAlignment.CENTER)).setPadding(1).setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE));
                    
                    final String colName = engineColNames[c];
                    String depts = hallAllocations.stream()
                            .filter(a -> a.seatCol() != null && colName.equalsIgnoreCase(a.seatCol().trim()))
                            .map(a -> shortenDept(a.department(), a.registerNumber()))
                            .distinct()
                            .collect(Collectors.joining("/"));
                    
                    String headerText = rowLabels[c] + (depts.isEmpty() ? "" : "\n(" + depts + ")");
                    gridTable.addHeaderCell(new Cell().add(new Paragraph(headerText).setFont(boldFont).setFontSize(8).setTextAlignment(TextAlignment.CENTER)).setPadding(1));
                }

                // Grid Body (Mapped by engine assignments - safe against duplicate keys and nulls)
                Map<String, Map<Integer, PdfAllocationView>> seatMap = hallAllocations.stream()
                        .filter(a -> a.seatCol() != null && a.seatRow() != null)
                        .collect(Collectors.groupingBy(
                                a -> a.seatCol().trim().toUpperCase(),
                                Collectors.toMap(
                                        PdfAllocationView::seatRow,
                                        a -> a,
                                        (first, duplicate) -> first
                                )
                        ));

                for (int r = 1; r <= maxRow; r++) {
                    for (int c = 0; c < colCount; c++) {
                        String colName = engineColNames[c];
                        int sno = (c * maxRow) + r;
                        
                        // SNO Cell
                        gridTable.addCell(new Cell().add(new Paragraph(String.valueOf(sno)).setFont(boldFont).setFontSize(8).setTextAlignment(TextAlignment.CENTER)).setPadding(1));
                        
                        // Reg No Cell
                        PdfAllocationView student = seatMap.getOrDefault(colName.toUpperCase(), Collections.emptyMap()).get(r);
                        if (student != null) {
                            gridTable.addCell(new Cell().add(new Paragraph(Objects.toString(student.registerNumber(), "-")).setFont(regFont).setFontSize(9).setTextAlignment(TextAlignment.CENTER)).setPadding(2));
                        } else {
                            gridTable.addCell(new Cell().add(new Paragraph("-").setFont(regFont).setFontSize(9).setTextAlignment(TextAlignment.CENTER)).setPadding(2));
                        }
                    }
                }
                document.add(gridTable);

                // --- Footer Signatures ---
                Table footerTable = new Table(UnitValue.createPercentArray(new float[]{1, 1})).useAllAvailableWidth().setMarginTop(15);
                footerTable.addCell(new Cell().add(new Paragraph("Name and signature of the Hall superintendent").setFont(boldFont).setFontSize(8).setTextAlignment(TextAlignment.CENTER))
                        .setBorder(Border.NO_BORDER));
                footerTable.addCell(new Cell().add(new Paragraph("Signature of Chief Superintendent with college seal").setFont(boldFont).setFontSize(8).setTextAlignment(TextAlignment.CENTER))
                        .setBorder(Border.NO_BORDER));
                document.add(footerTable);

                // Page Break logic: exactly 2 halls per page
                if (hallCounter % 2 == 0 && hallCounter < sortedHalls.size()) {
                    document.add(new AreaBreak());
                } else if (hallCounter < sortedHalls.size()) {
                    // Compact divider between two halls on the same page
                    document.add(new Paragraph("----------------------------------------------------------------------------------------------------------------------------------")
                            .setFontSize(7).setFontColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY).setTextAlignment(TextAlignment.CENTER).setMarginTop(8).setMarginBottom(8));
                }
            }

            document.close();
            outStream.flush();
        } catch (Exception e) {
            logger.error("CRITICAL PDF ERROR: {}", e.getMessage(), e);
            throw new RuntimeException("PDF Template Construction Failed: " + e.getMessage(), e);
        }
    }

    private String shortenDept(String dept, String regNo) {
        if (regNo == null || regNo.length() < 9) return dept != null ? dept : "N/A";
        
        String upperReg = regNo.toUpperCase();
        
        // Ensure the string has at least 9 characters before attempting extraction
        if (upperReg.length() >= 9) {
            char degreeType = upperReg.charAt(4);
            String code = upperReg.substring(7, 9);
            
            if (degreeType == 'P') {
                return switch (code) {
                    case "PS" -> "ME(PSE)";
                    case "ED" -> "ME(ED)";
                    case "CM" -> "ME(CM)";
                    case "CS" -> "ME(CSE)";
                    case "MB" -> "MBA";
                    default -> "ME(" + code + ")";
                };
            }
            
            return switch (code) {
                case "CS" -> "CSE";
                case "CB" -> "CSBS";
                case "EC" -> "ECE";
                case "ME" -> "MECH";
                case "EE" -> "EEE";
                case "MB" -> "MBA";
                case "AD" -> "AIDS";
                case "AM" -> "AIML";
                case "IT" -> "IT";
                default -> dept != null ? dept : code;
            };
        }
        
        return dept != null ? dept : "N/A";
    }

    private String resolveCollegeName() {
        String tenantId = com.exam.config.tenant.TenantContext.getCurrentTenant();
        if (tenantId == null) {
            return "EXAM SEATING ALLOCATION SYSTEM";
        }
        String cachedName = com.exam.config.tenant.TenantContext.getCollegeName(tenantId);
        if (cachedName != null && !cachedName.trim().isEmpty()) {
            return cachedName;
        }
        return "EXAM SEATING ALLOCATION SYSTEM";
    }
}

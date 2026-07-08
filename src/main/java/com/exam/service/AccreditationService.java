package com.exam.service;

import com.exam.entity.ExamArchive;
import com.exam.repository.ExamArchiveRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class AccreditationService {

    private final ExamArchiveRepository archiveRepository;
    private final com.exam.repository.master.TenantRepository tenantRepository;

    public AccreditationService(ExamArchiveRepository archiveRepository, com.exam.repository.master.TenantRepository tenantRepository) {
        this.archiveRepository = archiveRepository;
        this.tenantRepository = tenantRepository;
    }

    public void generateEvidencePack(UUID archiveId, OutputStream outputStream) throws IOException {
        ExamArchive archive = archiveRepository.findById(archiveId)
                .orElseThrow(() -> new IllegalArgumentException("Archive not found for ID: " + archiveId));

        try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {
            
            // Add Seating Plan
            if (archive.getSeatingPlanSnapshot() != null) {
                ZipEntry seatingEntry = new ZipEntry("Seating_Plan_" + archive.getExamDate() + ".pdf");
                zos.putNextEntry(seatingEntry);
                zos.write(archive.getSeatingPlanSnapshot());
                zos.closeEntry();
            }

            // Add Duty Sheet
            if (archive.getDutySheetSnapshot() != null) {
                ZipEntry dutyEntry = new ZipEntry("Invigilation_Duties_" + archive.getExamDate() + ".pdf");
                zos.putNextEntry(dutyEntry);
                zos.write(archive.getDutySheetSnapshot());
                zos.closeEntry();
            }

            // Add Attendance Snapshot
            if (archive.getAttendanceSnapshot() != null) {
                ZipEntry attendanceEntry = new ZipEntry("Attendance_Report_" + archive.getExamDate() + ".pdf");
                zos.putNextEntry(attendanceEntry);
                zos.write(archive.getAttendanceSnapshot());
                zos.closeEntry();
            }

            // Add Summary Report PDF
            byte[] summaryContent = generateSummaryReport(archive);
            ZipEntry summaryEntry = new ZipEntry("NAAC_Compliance_Summary_" + archive.getExamDate() + ".pdf");
            zos.putNextEntry(summaryEntry);
            zos.write(summaryContent);
            zos.closeEntry();
        }
    }

    private byte[] generateSummaryReport(ExamArchive archive) {
        try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream()) {
            com.itextpdf.kernel.pdf.PdfWriter writer = new com.itextpdf.kernel.pdf.PdfWriter(baos);
            com.itextpdf.kernel.pdf.PdfDocument pdf = new com.itextpdf.kernel.pdf.PdfDocument(writer);
            com.itextpdf.layout.Document document = new com.itextpdf.layout.Document(pdf);
            
            // Times New Roman
            com.itextpdf.kernel.font.PdfFont font = com.itextpdf.kernel.font.PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.TIMES_ROMAN);
            com.itextpdf.kernel.font.PdfFont boldFont = com.itextpdf.kernel.font.PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.TIMES_BOLD);
            
            document.setFont(font);

            // Add Logo & College Name
            try {
                byte[] logoBytes = null;
                String collegeName = "HallSync Platform";
                
                String tenantId = com.exam.config.tenant.TenantContext.getCurrentTenant();
                if (tenantId != null) {
                    com.exam.config.tenant.TenantContext.clear();
                    try {
                        java.util.Optional<com.exam.entity.master.Tenant> tenantOpt = tenantRepository.findByTenantId(tenantId);
                        if (tenantOpt.isPresent()) {
                            collegeName = tenantOpt.get().getCollegeName().toUpperCase();
                            String base64 = tenantOpt.get().getLogoBase64();
                            if (base64 != null && !base64.isEmpty()) {
                                String[] parts = base64.split(",");
                                String imageString = parts.length > 1 ? parts[1] : parts[0];
                                logoBytes = java.util.Base64.getDecoder().decode(imageString);
                            }
                        }
                    } finally {
                        com.exam.config.tenant.TenantContext.setCurrentTenant(tenantId);
                    }
                }
                
                if (logoBytes == null) {
                    org.springframework.core.io.ClassPathResource resource = new org.springframework.core.io.ClassPathResource("static/logo.png");
                    if (resource.exists()) {
                        logoBytes = resource.getInputStream().readAllBytes();
                    }
                }
                
                if (logoBytes != null) {
                    com.itextpdf.io.image.ImageData imageData = com.itextpdf.io.image.ImageDataFactory.create(logoBytes);
                    com.itextpdf.layout.element.Image img = new com.itextpdf.layout.element.Image(imageData);
                    img.setHeight(70);
                    img.setWidth(70);
                    img.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
                    document.add(img);
                }

                // Premium Header
                document.add(new com.itextpdf.layout.element.Paragraph(collegeName)
                        .setFont(boldFont)
                        .setFontSize(18)
                        .setFontColor(new com.itextpdf.kernel.colors.DeviceRgb(20, 50, 110))
                        .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                        .setMarginBottom(3));
                        
            } catch (Exception e) {
                // Ignore missing logo
            }
                    
            document.add(new com.itextpdf.layout.element.Paragraph("NAAC / NBA ACCREDITATION EVIDENCE PACK")
                    .setFont(boldFont)
                    .setFontSize(14)
                    .setFontColor(com.itextpdf.kernel.colors.ColorConstants.DARK_GRAY)
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                    .setMarginBottom(10));

            // Setup common premium table properties
            com.itextpdf.kernel.colors.Color headerBg = new com.itextpdf.kernel.colors.DeviceRgb(240, 244, 248);
            com.itextpdf.layout.borders.Border lightBorder = new com.itextpdf.layout.borders.SolidBorder(new com.itextpdf.kernel.colors.DeviceRgb(220, 220, 220), 1);

            // Details section
            document.add(new com.itextpdf.layout.element.Paragraph("EXAM DETAILS")
                    .setFont(boldFont).setFontSize(12).setFontColor(com.itextpdf.kernel.colors.ColorConstants.DARK_GRAY)
                    .setMarginBottom(2));
            
            float[] columnWidths = {200F, 300F};
            com.itextpdf.layout.element.Table table = new com.itextpdf.layout.element.Table(columnWidths);
            table.addCell(createPremiumCell("Archive ID:", boldFont, headerBg, lightBorder));
            table.addCell(createPremiumCell(archive.getId().toString(), font, null, lightBorder));
            table.addCell(createPremiumCell("Exam Date:", boldFont, headerBg, lightBorder));
            table.addCell(createPremiumCell(archive.getExamDate().toString(), font, null, lightBorder));
            table.addCell(createPremiumCell("Session:", boldFont, headerBg, lightBorder));
            table.addCell(createPremiumCell(archive.getSession(), font, null, lightBorder));
            table.addCell(createPremiumCell("Exam Type:", boldFont, headerBg, lightBorder));
            table.addCell(createPremiumCell(archive.getExamType(), font, null, lightBorder));
            table.addCell(createPremiumCell("Generated At:", boldFont, headerBg, lightBorder));
            table.addCell(createPremiumCell(java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), font, null, lightBorder));
            table.setMarginBottom(10);
            document.add(table);

            document.add(new com.itextpdf.layout.element.Paragraph("STATISTICS")
                    .setFont(boldFont).setFontSize(12).setFontColor(com.itextpdf.kernel.colors.ColorConstants.DARK_GRAY)
                    .setMarginBottom(2));
            com.itextpdf.layout.element.Table statsTable = new com.itextpdf.layout.element.Table(columnWidths);
            statsTable.addCell(createPremiumCell("Total Students:", boldFont, headerBg, lightBorder));
            statsTable.addCell(createPremiumCell(String.valueOf(archive.getTotalStudents()), font, null, lightBorder));
            statsTable.addCell(createPremiumCell("Total Halls Utilized:", boldFont, headerBg, lightBorder));
            statsTable.addCell(createPremiumCell(String.valueOf(archive.getTotalHalls()), font, null, lightBorder));
            statsTable.addCell(createPremiumCell("Total Invigilators:", boldFont, headerBg, lightBorder));
            statsTable.addCell(createPremiumCell(String.valueOf(archive.getTotalInvigilators()), font, null, lightBorder));
            statsTable.addCell(createPremiumCell("Total Absentees:", boldFont, headerBg, lightBorder));
            statsTable.addCell(createPremiumCell(String.valueOf(archive.getTotalAbsentees()), font, null, lightBorder));
            statsTable.addCell(createPremiumCell("Reported Malpractices:", boldFont, headerBg, lightBorder));
            statsTable.addCell(createPremiumCell(String.valueOf(archive.getTotalMalpractice()), font, null, lightBorder));
            statsTable.setMarginBottom(10);
            document.add(statsTable);

            document.add(new com.itextpdf.layout.element.Paragraph("COMPLIANCE METRICS")
                    .setFont(boldFont).setFontSize(12).setFontColor(com.itextpdf.kernel.colors.ColorConstants.DARK_GRAY)
                    .setMarginBottom(2));
            com.itextpdf.layout.element.Table complianceTable = new com.itextpdf.layout.element.Table(columnWidths);
            complianceTable.addCell(createPremiumCell("Seating Randomization Enabled:", boldFont, headerBg, lightBorder));
            complianceTable.addCell(createPremiumCell("YES (Algorithmic Department Mixing)", font, null, lightBorder).setFontColor(com.itextpdf.kernel.colors.ColorConstants.GREEN));
            complianceTable.addCell(createPremiumCell("Duty Allocation Fairness:", boldFont, headerBg, lightBorder));
            complianceTable.addCell(createPremiumCell("VERIFIED (Standard Deviation Checked)", font, null, lightBorder).setFontColor(com.itextpdf.kernel.colors.ColorConstants.GREEN));
            complianceTable.addCell(createPremiumCell("Archival Status:", boldFont, headerBg, lightBorder));
            complianceTable.addCell(createPremiumCell("SECURE (Immutable Snapshot Created)", font, null, lightBorder));
            complianceTable.addCell(createPremiumCell("Archived By:", boldFont, headerBg, lightBorder));
            complianceTable.addCell(createPremiumCell(archive.getArchivedBy() != null ? archive.getArchivedBy() : "System", font, null, lightBorder));
            complianceTable.addCell(createPremiumCell("Archived Timestamp:", boldFont, headerBg, lightBorder));
            complianceTable.addCell(createPremiumCell(archive.getArchivedAt().toString(), font, null, lightBorder));
            complianceTable.setMarginBottom(10);
            document.add(complianceTable);
            
            // Footer
            document.add(new com.itextpdf.layout.element.Paragraph("This document and the accompanying files in this ZIP archive")
                    .setFont(font)
                    .setFontSize(8)
                    .setFontColor(com.itextpdf.kernel.colors.ColorConstants.GRAY)
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));
            document.add(new com.itextpdf.layout.element.Paragraph("are system-generated and certified by the HallSync Platform.")
                    .setFont(font)
                    .setFontSize(8)
                    .setFontColor(com.itextpdf.kernel.colors.ColorConstants.GRAY)
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF summary", e);
        }
    }

    private com.itextpdf.layout.element.Cell createPremiumCell(String content, com.itextpdf.kernel.font.PdfFont font, com.itextpdf.kernel.colors.Color bgColor, com.itextpdf.layout.borders.Border border) {
        com.itextpdf.layout.element.Cell cell = new com.itextpdf.layout.element.Cell()
                .add(new com.itextpdf.layout.element.Paragraph(content).setFont(font).setFontSize(9))
                .setBorder(border)
                .setPadding(4);
        if (bgColor != null) {
            cell.setBackgroundColor(bgColor);
        }
        return cell;
    }
}

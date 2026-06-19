package com.exam.service;

import com.exam.entity.ExamArchive;
import com.exam.repository.ExamArchiveRepository;
import com.exam.repository.MalpracticeCaseRepository;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AccreditationReportService {

    private final ExamArchiveRepository archiveRepository;
    private final MalpracticeCaseRepository malpracticeRepository;

    public AccreditationReportService(ExamArchiveRepository archiveRepository,
                                      MalpracticeCaseRepository malpracticeRepository) {
        this.archiveRepository = archiveRepository;
        this.malpracticeRepository = malpracticeRepository;
    }

    public byte[] generateNaacReport(LocalDate from, LocalDate to) {
        List<ExamArchive> archives = archiveRepository.findByExamDateBetweenOrderByExamDateAsc(from, to);
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // Title
            document.add(new Paragraph("NAAC Examination Report")
                    .setBold()
                    .setFontSize(18)
                    .setTextAlignment(TextAlignment.CENTER));
            
            document.add(new Paragraph("Period: " + from.format(DateTimeFormatter.ISO_DATE) + " to " + to.format(DateTimeFormatter.ISO_DATE))
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20));

            // Summary Stats
            int totalExams = archives.size();
            int totalStudents = archives.stream().mapToInt(ExamArchive::getTotalStudents).sum();
            int totalMalpractice = archives.stream().mapToInt(ExamArchive::getTotalMalpractice).sum();

            document.add(new Paragraph("Summary Statistics:")
                    .setBold().setFontSize(14));
            document.add(new Paragraph("Total Exam Sessions Conducted: " + totalExams));
            document.add(new Paragraph("Total Students Appeared: " + totalStudents));
            document.add(new Paragraph("Total Malpractice Cases: " + totalMalpractice).setMarginBottom(15));

            // Detailed Table
            float[] columnWidths = {2, 2, 2, 2, 2, 2, 2};
            Table table = new Table(UnitValue.createPercentArray(columnWidths));
            table.setWidth(UnitValue.createPercentValue(100));

            String[] headers = {"Date", "Session", "Type", "Students", "Halls", "Absentees", "Malpractice"};
            for (String header : headers) {
                Cell cell = new Cell().add(new Paragraph(header).setBold());
                cell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
                table.addHeaderCell(cell);
            }

            for (ExamArchive a : archives) {
                table.addCell(a.getExamDate().toString());
                table.addCell(a.getSession());
                table.addCell(a.getExamType());
                table.addCell(String.valueOf(a.getTotalStudents()));
                table.addCell(String.valueOf(a.getTotalHalls()));
                table.addCell(String.valueOf(a.getTotalAbsentees()));
                table.addCell(String.valueOf(a.getTotalMalpractice()));
            }

            document.add(table);
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate NAAC report", e);
        }
    }

    public byte[] generateNbaReport(LocalDate from, LocalDate to) {
        // For simplicity, reusing a similar structure, but in a real scenario
        // NBA might require course outcome mapping or different metrics.
        List<ExamArchive> archives = archiveRepository.findByExamDateBetweenOrderByExamDateAsc(from, to);
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            document.add(new Paragraph("NBA Assessment Report")
                    .setBold()
                    .setFontSize(18)
                    .setTextAlignment(TextAlignment.CENTER));
            
            document.add(new Paragraph("Period: " + from.format(DateTimeFormatter.ISO_DATE) + " to " + to.format(DateTimeFormatter.ISO_DATE))
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20));

            float[] columnWidths = {2, 2, 3, 2, 2};
            Table table = new Table(UnitValue.createPercentArray(columnWidths));
            table.setWidth(UnitValue.createPercentValue(100));

            String[] headers = {"Date", "Session", "Type", "Students", "Invigilators"};
            for (String header : headers) {
                Cell cell = new Cell().add(new Paragraph(header).setBold());
                cell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
                table.addHeaderCell(cell);
            }

            for (ExamArchive a : archives) {
                table.addCell(a.getExamDate().toString());
                table.addCell(a.getSession());
                table.addCell(a.getExamType());
                table.addCell(String.valueOf(a.getTotalStudents()));
                table.addCell(String.valueOf(a.getTotalInvigilators()));
            }

            document.add(table);
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate NBA report", e);
        }
    }

    public byte[] generateAuditReport(LocalDate from, LocalDate to) {
        // Internal audit report includes more details like specific times and archived by
        List<ExamArchive> archives = archiveRepository.findByExamDateBetweenOrderByExamDateAsc(from, to);
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            document.add(new Paragraph("Internal Audit Report")
                    .setBold()
                    .setFontSize(18)
                    .setTextAlignment(TextAlignment.CENTER));
            
            document.add(new Paragraph("Generated: " + LocalDate.now().toString())
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20));

            float[] columnWidths = {2, 2, 2, 2, 3, 3};
            Table table = new Table(UnitValue.createPercentArray(columnWidths));
            table.setWidth(UnitValue.createPercentValue(100));

            String[] headers = {"Exam Date", "Type", "Students", "Malpractice", "Archived At", "Archived By"};
            for (String header : headers) {
                Cell cell = new Cell().add(new Paragraph(header).setBold());
                cell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
                table.addHeaderCell(cell);
            }

            for (ExamArchive a : archives) {
                table.addCell(a.getExamDate().toString());
                table.addCell(a.getExamType());
                table.addCell(String.valueOf(a.getTotalStudents()));
                table.addCell(String.valueOf(a.getTotalMalpractice()));
                table.addCell(a.getArchivedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                table.addCell(a.getArchivedBy() != null ? a.getArchivedBy() : "System");
            }

            document.add(table);
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Audit report", e);
        }
    }
}

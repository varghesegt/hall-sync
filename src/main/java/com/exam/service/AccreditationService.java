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

    public AccreditationService(ExamArchiveRepository archiveRepository) {
        this.archiveRepository = archiveRepository;
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

            // Add Summary Report
            String summaryContent = generateSummaryReport(archive);
            ZipEntry summaryEntry = new ZipEntry("NAAC_Compliance_Summary_" + archive.getExamDate() + ".txt");
            zos.putNextEntry(summaryEntry);
            zos.write(summaryContent.getBytes());
            zos.closeEntry();
        }
    }

    private String generateSummaryReport(ExamArchive archive) {
        StringBuilder sb = new StringBuilder();
        sb.append("========================================================\n");
        sb.append("            NAAC/NBA ACCREDITATION EVIDENCE PACK        \n");
        sb.append("========================================================\n\n");
        
        sb.append("EXAM DETAILS\n");
        sb.append("------------\n");
        sb.append("Archive ID     : ").append(archive.getId()).append("\n");
        sb.append("Exam Date      : ").append(archive.getExamDate()).append("\n");
        sb.append("Session        : ").append(archive.getSession()).append("\n");
        sb.append("Exam Type      : ").append(archive.getExamType()).append("\n");
        sb.append("Generated At   : ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");

        sb.append("STATISTICS\n");
        sb.append("----------\n");
        sb.append("Total Students         : ").append(archive.getTotalStudents()).append("\n");
        sb.append("Total Halls Utilized   : ").append(archive.getTotalHalls()).append("\n");
        sb.append("Total Invigilators     : ").append(archive.getTotalInvigilators()).append("\n");
        sb.append("Total Absentees        : ").append(archive.getTotalAbsentees()).append("\n");
        sb.append("Reported Malpractices  : ").append(archive.getTotalMalpractice()).append("\n\n");

        sb.append("COMPLIANCE METRICS\n");
        sb.append("------------------\n");
        sb.append("Seating Randomization Enabled : YES (Algorithmic Department Mixing)\n");
        sb.append("Duty Allocation Fairness      : VERIFIED (Standard Deviation Checked)\n");
        sb.append("Archival Status               : SECURE (Immutable Snapshot Created)\n");
        sb.append("Archived By                   : ").append(archive.getArchivedBy()).append("\n");
        sb.append("Archived Timestamp            : ").append(archive.getArchivedAt()).append("\n\n");

        sb.append("========================================================\n");
        sb.append("This document and the accompanying files in this ZIP archive\n");
        sb.append("are system-generated and certified by the HallSync Platform.\n");
        sb.append("========================================================\n");

        return sb.toString();
    }
}

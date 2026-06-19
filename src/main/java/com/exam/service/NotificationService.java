package com.exam.service;

import com.exam.entity.*;
import com.exam.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationLogRepository notificationLogRepository;
    private final InvigilatorDutyRepository dutyRepository;
    private final FacultyRepository facultyRepository;
    private final AllocationBatchRepository batchRepository;
    private final JavaMailSender mailSender;

    public NotificationService(NotificationLogRepository notificationLogRepository,
                               InvigilatorDutyRepository dutyRepository,
                               FacultyRepository facultyRepository,
                               AllocationBatchRepository batchRepository,
                               JavaMailSender mailSender) {
        this.notificationLogRepository = notificationLogRepository;
        this.dutyRepository = dutyRepository;
        this.facultyRepository = facultyRepository;
        this.batchRepository = batchRepository;
        this.mailSender = mailSender;
    }

    /**
     * Sends duty allocation emails to all faculty assigned to a batch.
     */
    public Map<String, Object> sendDutyAllocationEmails(UUID batchId) {
        AllocationBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + batchId));

        List<InvigilatorDuty> duties = dutyRepository.findByBatchIdOrderByHallIdAsc(batchId);
        int sent = 0, failed = 0, skipped = 0;

        for (InvigilatorDuty duty : duties) {
            Faculty faculty = duty.getFaculty();
            if (faculty == null || faculty.getEmail() == null || faculty.getEmail().isBlank()) {
                skipped++;
                continue;
            }

            String subject = "HallSync — Duty Allocation: " + duty.getDutyDate();
            String body = String.format(
                "Dear %s,\n\n" +
                "You have been assigned invigilation duty.\n\n" +
                "Details:\n" +
                "  Date    : %s\n" +
                "  Shift   : %s\n" +
                "  Hall    : %s\n" +
                "  Role    : %s\n\n" +
                "Please report to the exam hall 15 minutes before the exam begins.\n\n" +
                "Regards,\nHallSync Exam Management System",
                faculty.getName(),
                duty.getDutyDate(),
                duty.getShift(),
                duty.getHall() != null ? duty.getHall().getName() : "TBD",
                duty.getDutyType()
            );

            NotificationLog logEntry = new NotificationLog(
                faculty.getEmail(), faculty.getName(), subject, body,
                "DUTY_ALLOCATION", batchId
            );

            try {
                sendEmail(faculty.getEmail(), subject, body);
                logEntry.setStatus("SENT");
                logEntry.setSentAt(LocalDateTime.now());
                sent++;
            } catch (Exception e) {
                logEntry.setStatus("FAILED");
                logEntry.setErrorMessage(e.getMessage());
                failed++;
                log.warn("Failed to send duty email to {}: {}", faculty.getEmail(), e.getMessage());
            }
            notificationLogRepository.save(logEntry);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalDuties", duties.size());
        result.put("sent", sent);
        result.put("failed", failed);
        result.put("skipped", skipped);
        return result;
    }

    /**
     * Sends a broadcast notification to all faculty assigned to a batch.
     */
    public Map<String, Object> sendExamBroadcast(UUID batchId, String customMessage) {
        List<InvigilatorDuty> duties = dutyRepository.findByBatchIdOrderByHallIdAsc(batchId);

        // Collect unique faculty
        Set<UUID> seen = new HashSet<>();
        List<Faculty> uniqueFaculty = new ArrayList<>();
        for (InvigilatorDuty duty : duties) {
            if (duty.getFaculty() != null && seen.add(duty.getFaculty().getId())) {
                uniqueFaculty.add(duty.getFaculty());
            }
        }

        int sent = 0, failed = 0, skipped = 0;

        for (Faculty faculty : uniqueFaculty) {
            if (faculty.getEmail() == null || faculty.getEmail().isBlank()) {
                skipped++;
                continue;
            }

            String subject = "HallSync — Exam Notification";
            String body = String.format(
                "Dear %s,\n\n%s\n\nRegards,\nHallSync Exam Management System",
                faculty.getName(), customMessage
            );

            NotificationLog logEntry = new NotificationLog(
                faculty.getEmail(), faculty.getName(), subject, body,
                "EXAM_NOTIFICATION", batchId
            );

            try {
                sendEmail(faculty.getEmail(), subject, body);
                logEntry.setStatus("SENT");
                logEntry.setSentAt(LocalDateTime.now());
                sent++;
            } catch (Exception e) {
                logEntry.setStatus("FAILED");
                logEntry.setErrorMessage(e.getMessage());
                failed++;
            }
            notificationLogRepository.save(logEntry);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalRecipients", uniqueFaculty.size());
        result.put("sent", sent);
        result.put("failed", failed);
        result.put("skipped", skipped);
        return result;
    }

    /**
     * Returns recent notification history.
     */
    @Transactional(readOnly = true)
    public List<NotificationLog> getRecentNotifications() {
        return notificationLogRepository.findTop50ByOrderByCreatedAtDesc();
    }

    /**
     * Returns notification stats for a batch.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getNotificationStats(UUID batchId) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("sent", notificationLogRepository.countByBatchIdAndStatus(batchId, "SENT"));
        stats.put("failed", notificationLogRepository.countByBatchIdAndStatus(batchId, "FAILED"));
        stats.put("pending", notificationLogRepository.countByBatchIdAndStatus(batchId, "PENDING"));
        return stats;
    }

    private void sendEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}

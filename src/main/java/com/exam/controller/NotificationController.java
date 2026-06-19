package com.exam.controller;

import com.exam.entity.NotificationLog;
import com.exam.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/notifications")

public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/duty-emails/{batchId}")
    public ResponseEntity<Map<String, Object>> sendDutyEmails(@PathVariable UUID batchId) {
        return ResponseEntity.ok(notificationService.sendDutyAllocationEmails(batchId));
    }

    @PostMapping("/exam-broadcast/{batchId}")
    public ResponseEntity<Map<String, Object>> sendExamBroadcast(
            @PathVariable UUID batchId,
            @RequestBody Map<String, String> body) {
        String message = body.getOrDefault("message", "Important exam notification from HallSync.");
        return ResponseEntity.ok(notificationService.sendExamBroadcast(batchId, message));
    }

    @GetMapping("/log")
    public ResponseEntity<List<Map<String, Object>>> getNotificationLog() {
        List<NotificationLog> logs = notificationService.getRecentNotifications();
        List<Map<String, Object>> result = new ArrayList<>();
        for (NotificationLog log : logs) {
            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("id", log.getId());
            dto.put("recipientEmail", log.getRecipientEmail());
            dto.put("recipientName", log.getRecipientName());
            dto.put("subject", log.getSubject());
            dto.put("notificationType", log.getNotificationType());
            dto.put("status", log.getStatus());
            dto.put("errorMessage", log.getErrorMessage());
            dto.put("sentAt", log.getSentAt() != null ? log.getSentAt().toString() : null);
            dto.put("createdAt", log.getCreatedAt().toString());
            result.add(dto);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/stats/{batchId}")
    public ResponseEntity<Map<String, Object>> getStats(@PathVariable UUID batchId) {
        return ResponseEntity.ok(notificationService.getNotificationStats(batchId));
    }
}

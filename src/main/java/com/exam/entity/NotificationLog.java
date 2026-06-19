package com.exam.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification_log")
public class NotificationLog {

    @Id
    private UUID id;

    @Column(name = "recipient_email")
    private String recipientEmail;

    @Column(name = "recipient_name")
    private String recipientName;

    @Column(nullable = false, length = 500)
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(name = "notification_type", nullable = false, length = 50)
    private String notificationType;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "batch_id")
    private UUID batchId;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected NotificationLog() {}

    public NotificationLog(String recipientEmail, String recipientName, String subject,
                           String body, String notificationType, UUID batchId) {
        this.id = UUID.randomUUID();
        this.recipientEmail = recipientEmail;
        this.recipientName = recipientName;
        this.subject = subject;
        this.body = body;
        this.notificationType = notificationType;
        this.status = "PENDING";
        this.batchId = batchId;
        this.createdAt = LocalDateTime.now();
    }

    // Getters
    public UUID getId() { return id; }
    public String getRecipientEmail() { return recipientEmail; }
    public String getRecipientName() { return recipientName; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public String getNotificationType() { return notificationType; }
    public String getStatus() { return status; }
    public String getErrorMessage() { return errorMessage; }
    public UUID getBatchId() { return batchId; }
    public LocalDateTime getSentAt() { return sentAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // Setters
    public void setStatus(String status) { this.status = status; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
}

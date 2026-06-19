package com.exam.repository;

import com.exam.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {

    List<NotificationLog> findByBatchIdOrderByCreatedAtDesc(UUID batchId);

    List<NotificationLog> findTop50ByOrderByCreatedAtDesc();

    List<NotificationLog> findByNotificationTypeOrderByCreatedAtDesc(String notificationType);

    long countByBatchIdAndStatus(UUID batchId, String status);
}

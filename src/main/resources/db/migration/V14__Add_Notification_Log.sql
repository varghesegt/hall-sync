-- V14__Add_Notification_Log.sql
-- Communication Center: logs all outgoing notifications

CREATE TABLE IF NOT EXISTS notification_log (
    id UUID PRIMARY KEY,
    recipient_email VARCHAR(255),
    recipient_name VARCHAR(255),
    subject VARCHAR(500) NOT NULL,
    body TEXT,
    notification_type VARCHAR(50) NOT NULL DEFAULT 'GENERAL',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    batch_id UUID REFERENCES allocation_batches(id),
    sent_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notification_type ON notification_log(notification_type);
CREATE INDEX IF NOT EXISTS idx_notification_status ON notification_log(status);
CREATE INDEX IF NOT EXISTS idx_notification_batch ON notification_log(batch_id);
CREATE INDEX IF NOT EXISTS idx_notification_created ON notification_log(created_at);

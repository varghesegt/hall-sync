-- ══════════════════════════════════════════════════════════════════════════════
-- V3__Drop_Audit_Log_Constraint.sql
-- Drop Hibernate-generated check constraint on audit_log.event_type
-- to allow new enum values like PATTERN_DEVIATION
-- ══════════════════════════════════════════════════════════════════════════════

ALTER TABLE audit_log DROP CONSTRAINT IF EXISTS audit_log_event_type_check;

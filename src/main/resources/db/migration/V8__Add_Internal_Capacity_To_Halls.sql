-- ══════════════════════════════════════════════════════════════════════════════
-- V8__Add_Internal_Capacity_To_Halls.sql
-- Add internal_capacity to halls to support dual capacities per hall.
-- ══════════════════════════════════════════════════════════════════════════════

ALTER TABLE halls ADD COLUMN IF NOT EXISTS internal_capacity INT NOT NULL DEFAULT 40;

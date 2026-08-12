-- V19: Add is_revaluation column to claim_records table for Revaluation Mode support

ALTER TABLE claim_records ADD COLUMN IF NOT EXISTS is_revaluation BOOLEAN DEFAULT FALSE;

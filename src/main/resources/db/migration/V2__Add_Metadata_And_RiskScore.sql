-- ══════════════════════════════════════════════════════════════════════════════
-- V2__Add_Metadata_And_RiskScore.sql
-- Add semester, regulation to students and risk_score to allocations
-- ══════════════════════════════════════════════════════════════════════════════

-- 1. Add metadata to students
ALTER TABLE students ADD COLUMN IF NOT EXISTS semester VARCHAR(20);
ALTER TABLE students ADD COLUMN IF NOT EXISTS regulation VARCHAR(20);

-- 2. Add risk_score to allocations
ALTER TABLE allocations ADD COLUMN IF NOT EXISTS risk_score INTEGER;

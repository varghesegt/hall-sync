-- ══════════════════════════════════════════════════════════════════════════════
-- V7__Internal_Exam_Support.sql
-- Add exam_type discriminator for dual-mode (Semester / Internal) support
-- All existing data defaults to 'SEMESTER' — zero disruption
-- ══════════════════════════════════════════════════════════════════════════════

-- 1. Add exam_type to exam_sessions
ALTER TABLE exam_sessions ADD COLUMN IF NOT EXISTS exam_type VARCHAR(20) NOT NULL DEFAULT 'SEMESTER';

-- 2. Add exam_type to uploaded_files
ALTER TABLE uploaded_files ADD COLUMN IF NOT EXISTS exam_type VARCHAR(20) NOT NULL DEFAULT 'SEMESTER';

-- 3. Index for fast filtering by exam_type
CREATE INDEX IF NOT EXISTS idx_exam_sessions_type ON exam_sessions(exam_type);
CREATE INDEX IF NOT EXISTS idx_uploaded_files_type ON uploaded_files(exam_type);

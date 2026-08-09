-- Add grid dimension columns to halls table.
-- These allow each college to configure their own physical seating grid layout.
-- Defaults match the current hardcoded behavior: 5×5 for semester, 7×6 for internal.

ALTER TABLE halls ADD COLUMN IF NOT EXISTS sem_rows INT NOT NULL DEFAULT 5;
ALTER TABLE halls ADD COLUMN IF NOT EXISTS sem_cols INT NOT NULL DEFAULT 5;
ALTER TABLE halls ADD COLUMN IF NOT EXISTS internal_rows INT NOT NULL DEFAULT 7;
ALTER TABLE halls ADD COLUMN IF NOT EXISTS internal_cols INT NOT NULL DEFAULT 6;

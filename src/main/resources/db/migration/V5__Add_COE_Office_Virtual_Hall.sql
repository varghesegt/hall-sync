-- Add COE Office Virtual Hall for catchment of overflow students
-- This ensures zero students are dropped during allocation
INSERT INTO halls (id, name, capacity) VALUES ('COE-OFFICE', 'COE OFFICE', 1000) ON CONFLICT (id) DO NOTHING;

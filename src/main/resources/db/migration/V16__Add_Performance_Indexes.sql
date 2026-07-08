-- ══════════════════════════════════════════════════════════════════════════════
-- V16__Add_Performance_Indexes.sql
-- Production performance indexes for 50+ tenant scale
-- Without these indexes, every query does a full table scan.
-- At 50 colleges with thousands of students each, queries would take 10-30 seconds.
-- ══════════════════════════════════════════════════════════════════════════════

-- Students: Most queries filter by exam_session_id and department
CREATE INDEX IF NOT EXISTS idx_students_session ON students(exam_session_id);
CREATE INDEX IF NOT EXISTS idx_students_dept ON students(department);
CREATE INDEX IF NOT EXISTS idx_students_register ON students(register_number);

-- Allocations: Always queried by batch_id, and joined on student_id / hall_id
CREATE INDEX IF NOT EXISTS idx_allocations_batch ON allocations(batch_id);
CREATE INDEX IF NOT EXISTS idx_allocations_student ON allocations(student_id);
CREATE INDEX IF NOT EXISTS idx_allocations_hall ON allocations(hall_id);

-- Allocation Batches: Queried by session and status
CREATE INDEX IF NOT EXISTS idx_batches_session ON allocation_batches(exam_session_id);
CREATE INDEX IF NOT EXISTS idx_batches_status ON allocation_batches(status);
CREATE INDEX IF NOT EXISTS idx_batches_created ON allocation_batches(created_at DESC);

-- Audit Log: Queried by session + time for dashboards
CREATE INDEX IF NOT EXISTS idx_audit_session ON audit_log(exam_session_id);
CREATE INDEX IF NOT EXISTS idx_audit_created ON audit_log(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_event_type ON audit_log(event_type);

-- Exam Sessions: Queried by season and date
CREATE INDEX IF NOT EXISTS idx_sessions_season ON exam_sessions(season_id);
CREATE INDEX IF NOT EXISTS idx_sessions_date ON exam_sessions(exam_date DESC);

-- Uploaded Files: Queried by hash for deduplication
CREATE INDEX IF NOT EXISTS idx_uploads_created ON uploaded_files(created_at DESC);

-- Faculty: Queried by department
CREATE INDEX IF NOT EXISTS idx_faculty_dept ON faculty(department);

-- Malpractice Cases: Queried by batch
CREATE INDEX IF NOT EXISTS idx_malpractice_batch_16 ON malpractice_cases(batch_id);

-- Student Attendance: Queried by batch
CREATE INDEX IF NOT EXISTS idx_attendance_batch_16 ON student_attendance(batch_id);
CREATE INDEX IF NOT EXISTS idx_attendance_student ON student_attendance(student_id);

-- Claims: Queried by batch
CREATE INDEX IF NOT EXISTS idx_claims_batch ON claim_records(batch_id);
CREATE INDEX IF NOT EXISTS idx_claims_created ON claim_records(created_at DESC);

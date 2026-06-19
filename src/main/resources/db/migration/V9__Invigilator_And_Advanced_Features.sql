-- V9__Invigilator_And_Advanced_Features.sql
-- Adds Faculty, Invigilator Duties, Malpractice, Archives, and Attendance tables

-- 1. Faculty (Invigilators)
CREATE TABLE IF NOT EXISTS faculty (
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    employee_id VARCHAR(50) NOT NULL UNIQUE,
    department VARCHAR(100) NOT NULL,
    designation VARCHAR(100),
    phone VARCHAR(20),
    email VARCHAR(150),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Invigilator Duties
CREATE TABLE IF NOT EXISTS invigilator_duties (
    id UUID PRIMARY KEY,
    batch_id UUID NOT NULL REFERENCES allocation_batches(id) ON DELETE CASCADE,
    faculty_id UUID NOT NULL REFERENCES faculty(id),
    hall_id VARCHAR(50) NOT NULL REFERENCES halls(id),
    duty_type VARCHAR(30) NOT NULL DEFAULT 'INVIGILATOR',
    shift VARCHAR(10) NOT NULL,
    duty_date DATE NOT NULL,
    is_present BOOLEAN,
    marked_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_duty_batch_faculty_hall UNIQUE(batch_id, faculty_id, hall_id)
);

-- 3. Malpractice Cases (evidence stored as BYTEA in DB)
CREATE TABLE IF NOT EXISTS malpractice_cases (
    id UUID PRIMARY KEY,
    batch_id UUID REFERENCES allocation_batches(id),
    student_id UUID REFERENCES students(id),
    hall_id VARCHAR(50) REFERENCES halls(id),
    reported_by UUID REFERENCES faculty(id),
    case_type VARCHAR(50) NOT NULL,
    description TEXT,
    evidence_data BYTEA,
    evidence_filename VARCHAR(255),
    evidence_content_type VARCHAR(100),
    severity VARCHAR(20) NOT NULL DEFAULT 'MODERATE',
    action_taken VARCHAR(500),
    status VARCHAR(30) NOT NULL DEFAULT 'REPORTED',
    reported_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP
);

-- 4. Exam Archives
CREATE TABLE IF NOT EXISTS exam_archives (
    id UUID PRIMARY KEY,
    exam_session_id UUID NOT NULL REFERENCES exam_sessions(id),
    batch_id UUID NOT NULL REFERENCES allocation_batches(id),
    exam_type VARCHAR(20) NOT NULL,
    exam_date DATE NOT NULL,
    session VARCHAR(10) NOT NULL,
    total_students INTEGER NOT NULL,
    total_halls INTEGER NOT NULL,
    total_invigilators INTEGER NOT NULL DEFAULT 0,
    total_absentees INTEGER NOT NULL DEFAULT 0,
    total_malpractice INTEGER NOT NULL DEFAULT 0,
    seating_plan_snapshot BYTEA,
    attendance_snapshot BYTEA,
    duty_sheet_snapshot BYTEA,
    archived_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    archived_by VARCHAR(150)
);

-- 5. Student Attendance
CREATE TABLE IF NOT EXISTS student_attendance (
    id UUID PRIMARY KEY,
    batch_id UUID NOT NULL REFERENCES allocation_batches(id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    hall_id VARCHAR(50) NOT NULL REFERENCES halls(id),
    is_present BOOLEAN,
    marked_by UUID REFERENCES faculty(id),
    marked_at TIMESTAMP,
    CONSTRAINT uq_attendance_batch_student UNIQUE(batch_id, student_id)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_faculty_dept ON faculty(department);
CREATE INDEX IF NOT EXISTS idx_faculty_active ON faculty(is_active);
CREATE INDEX IF NOT EXISTS idx_duties_batch ON invigilator_duties(batch_id);
CREATE INDEX IF NOT EXISTS idx_duties_faculty ON invigilator_duties(faculty_id);
CREATE INDEX IF NOT EXISTS idx_duties_date ON invigilator_duties(duty_date);
CREATE INDEX IF NOT EXISTS idx_malpractice_batch ON malpractice_cases(batch_id);
CREATE INDEX IF NOT EXISTS idx_malpractice_status ON malpractice_cases(status);
CREATE INDEX IF NOT EXISTS idx_archives_session ON exam_archives(exam_session_id);
CREATE INDEX IF NOT EXISTS idx_archives_date ON exam_archives(exam_date);
CREATE INDEX IF NOT EXISTS idx_attendance_batch ON student_attendance(batch_id);

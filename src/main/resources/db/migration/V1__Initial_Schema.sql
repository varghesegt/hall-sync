-- ══════════════════════════════════════════════════════════════════════════════
-- V1__Initial_Schema.sql
-- Baseline schema for Exam Seat Allocation System
-- ══════════════════════════════════════════════════════════════════════════════

-- 1. Halls Table
CREATE TABLE IF NOT EXISTS halls (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    capacity INTEGER NOT NULL
);

-- 2. Exam Sessions Table
CREATE TABLE IF NOT EXISTS exam_sessions (
    id UUID PRIMARY KEY,
    season_id VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    exam_date DATE NOT NULL,
    session VARCHAR(10) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. Students Table
CREATE TABLE IF NOT EXISTS students (
    id UUID PRIMARY KEY,
    exam_session_id UUID NOT NULL,
    register_number VARCHAR(100) NOT NULL,
    name VARCHAR(100),
    department VARCHAR(100) NOT NULL,
    class_name VARCHAR(100),
    subject_name VARCHAR(255),
    subject_code VARCHAR(100),
    CONSTRAINT fk_student_session FOREIGN KEY (exam_session_id) REFERENCES exam_sessions(id) ON DELETE CASCADE,
    CONSTRAINT uq_student_session UNIQUE (register_number, exam_session_id)
);

-- 4. Allocation Batches Table
CREATE TABLE IF NOT EXISTS allocation_batches (
    id UUID PRIMARY KEY,
    exam_session_id UUID NOT NULL,
    allocation_request_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    version INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_batch_session FOREIGN KEY (exam_session_id) REFERENCES exam_sessions(id) ON DELETE CASCADE
);

-- 5. Allocations Table
CREATE TABLE IF NOT EXISTS allocations (
    id UUID PRIMARY KEY,
    batch_id UUID NOT NULL,
    student_id UUID NOT NULL,
    hall_id VARCHAR(50) NOT NULL,
    seat_row INTEGER NOT NULL,
    seat_col VARCHAR(5) NOT NULL,
    CONSTRAINT fk_alloc_batch FOREIGN KEY (batch_id) REFERENCES allocation_batches(id) ON DELETE CASCADE,
    CONSTRAINT fk_alloc_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    CONSTRAINT fk_alloc_hall FOREIGN KEY (hall_id) REFERENCES halls(id) ON DELETE CASCADE,
    CONSTRAINT uq_hall_seat_batch UNIQUE (hall_id, seat_row, seat_col, batch_id),
    CONSTRAINT uq_student_per_batch UNIQUE (student_id, batch_id)
);

-- 6. Audit Log Table
CREATE TABLE IF NOT EXISTS audit_log (
    id UUID PRIMARY KEY,
    exam_session_id UUID NOT NULL,
    batch_id UUID,
    allocation_request_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    severity VARCHAR(50) NOT NULL,
    message VARCHAR(4000) NOT NULL,
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_session FOREIGN KEY (exam_session_id) REFERENCES exam_sessions(id) ON DELETE CASCADE,
    CONSTRAINT fk_audit_batch FOREIGN KEY (batch_id) REFERENCES allocation_batches(id) ON DELETE SET NULL
);

-- 7. Uploaded Files Table
CREATE TABLE IF NOT EXISTS uploaded_files (
    id UUID PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    sha256_hash VARCHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 8. Allocation Locks Table (Native Locking)
CREATE TABLE IF NOT EXISTS allocation_locks (
    exam_session_id UUID PRIMARY KEY,
    locked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    locked_by VARCHAR(255) NOT NULL,
    CONSTRAINT fk_lock_session FOREIGN KEY (exam_session_id) REFERENCES exam_sessions(id) ON DELETE CASCADE
);

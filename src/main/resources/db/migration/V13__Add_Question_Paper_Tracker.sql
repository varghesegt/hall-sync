CREATE TABLE IF NOT EXISTS question_paper_trackers (
    id UUID PRIMARY KEY,
    exam_session_id UUID NOT NULL REFERENCES exam_sessions(id),
    subject_code VARCHAR(255) NOT NULL,
    total_received INT NOT NULL,
    stored_location VARCHAR(255),
    distributed_time TIMESTAMP,
    returned_time TIMESTAMP,
    status VARCHAR(50) NOT NULL DEFAULT 'RECEIVED',
    remarks TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

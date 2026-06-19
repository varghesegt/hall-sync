package com.exam.entity;

/**
 * Lifecycle states for AllocationBatch.
 *
 * RUNNING  → allocation in progress (async job executing)
 * ACTIVE   → allocation completed successfully (current active batch)
 * SUPERSEDED → replaced by a newer ACTIVE batch
 * FAILED   → allocation failed (error captured in audit)
 */
public enum BatchStatus {
    RUNNING,
    ACTIVE,
    SUPERSEDED,
    FAILED
}

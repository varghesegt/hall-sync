package com.exam.engine.model;

import java.util.Objects;

public record Hall(String id, int capacity, int rows, int cols) implements Comparable<Hall> {
    public Hall {
        Objects.requireNonNull(id, "Hall ID cannot be null");
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        if (rows <= 0 || cols <= 0) {
            throw new IllegalArgumentException("Rows and cols must be positive");
        }
    }

    /** Backward-compatible constructor: defaults to 5×5 grid */
    public Hall(String id, int capacity) {
        this(id, capacity, 5, 5);
    }

    @Override
    public int compareTo(Hall o) {
        return this.id.compareTo(o.id);
    }
}


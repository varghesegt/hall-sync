package com.exam.engine.model;

import java.util.Objects;

public record Hall(String id, int capacity) implements Comparable<Hall> {
    public Hall {
        Objects.requireNonNull(id, "Hall ID cannot be null");
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
    }

    @Override
    public int compareTo(Hall o) {
        return this.id.compareTo(o.id);
    }
}

package com.exam.claims.enums;

public enum PostHeld {
    EXAMINER("EXAMINER"),
    ASSISTANT_EXAMINER("ASSISTANT EXAMINER"),
    CHIEF_EXAMINER("CHIEF EXAMINER");

    private final String displayName;

    PostHeld(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static PostHeld fromString(String value) {
        if (value == null) return EXAMINER;
        String normalized = value.trim().toUpperCase();
        for (PostHeld p : values()) {
            if (p.displayName.equalsIgnoreCase(normalized) || p.name().equalsIgnoreCase(normalized)) {
                return p;
            }
        }
        // Partial matching
        if (normalized.contains("CHIEF")) return CHIEF_EXAMINER;
        if (normalized.contains("ASSISTANT")) return ASSISTANT_EXAMINER;
        return EXAMINER;
    }
}

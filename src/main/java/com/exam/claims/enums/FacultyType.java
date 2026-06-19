package com.exam.claims.enums;

public enum FacultyType {
    INTERNAL("INT"),
    EXTERNAL("EXT");

    private final String code;

    FacultyType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static FacultyType fromString(String value) {
        if (value == null) return EXTERNAL;
        String normalized = value.trim().toUpperCase();
        if (normalized.contains("INT") || normalized.contains("INTERNAL")) return INTERNAL;
        return EXTERNAL;
    }
}

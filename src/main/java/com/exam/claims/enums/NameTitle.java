package com.exam.claims.enums;

public enum NameTitle {
    MR("Mr."),
    MRS("Mrs."),
    MS("Ms."),
    DR("Dr.");

    private final String display;

    NameTitle(String display) {
        this.display = display;
    }

    public String getDisplay() {
        return display;
    }

    public static NameTitle fromString(String value) {
        if (value == null) return MR;
        String normalized = value.trim().toUpperCase().replace(".", "");
        if (normalized.contains("MRS")) return MRS;
        if (normalized.contains("MS")) return MS;
        if (normalized.contains("DR")) return DR;
        return MR;
    }
}

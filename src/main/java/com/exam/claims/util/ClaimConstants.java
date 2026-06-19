package com.exam.claims.util;

import java.math.BigDecimal;

/**
 * Constants for claim calculations.
 * All rates are configurable here for easy updates.
 */
public final class ClaimConstants {

    private ClaimConstants() {
        // Utility class
    }

    // ==================== SCRIPT AMOUNT ====================
    /** Rate per script for UG and PG valuation */
    public static final BigDecimal RATE_PER_SCRIPT = new BigDecimal("30.00");

    // ==================== TRAVELLING ALLOWANCE (TA) ====================
    /** Rate per KM for TA calculation */
    public static final BigDecimal TA_RATE_PER_KM = new BigDecimal("8.00");

    /** Flat TA amount when round-trip distance is <= threshold */
    public static final BigDecimal TA_FLAT_AMOUNT = new BigDecimal("150.00");

    /** Distance threshold in KM for flat TA (round-trip) */
    public static final BigDecimal TA_DISTANCE_THRESHOLD_KM = new BigDecimal("35.00");

    // ==================== DEARNESS ALLOWANCE (DA) ====================
    /** DA per day when attending both FN and AN sessions */
    public static final BigDecimal DA_PER_DAY = new BigDecimal("300.00");

    /** DA per session when attending single session (FN or AN) */
    public static final BigDecimal DA_PER_SESSION = new BigDecimal("250.00");

    // ==================== CHIEF EXAMINER ====================
    /** Bonus percentage for Chief Examiner script amount */
    public static final BigDecimal CHIEF_EXAMINER_BONUS_PERCENT = new BigDecimal("0.10");

    // ==================== REVENUE STAMP ====================
    /** Threshold above which revenue stamp note is shown */
    public static final BigDecimal REVENUE_STAMP_THRESHOLD = new BigDecimal("5000.00");

    // Removed KRCE specific logic for Multi-Tenancy

    // ==================== DOCUMENT HEADER ====================
    public static final String COLLEGE_NAME = "K.RAMAKRISHNAN COLLEGE OF ENGINEERING";
    public static final String COLLEGE_AUTONOMY = "(Autonomous)";
    public static final String OFFICE_TITLE = "Office of the Controller of Examinations";

    public static String getExamTitle(String examSeason) {
        String season = (examSeason != null && !examSeason.trim().isEmpty()) ? examSeason : "MAY 2026";
        return "UG/PG Central Valuation - " + season + " Examinations";
    }


}

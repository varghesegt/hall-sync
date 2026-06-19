package com.exam.claims.service;

import com.exam.claims.entity.ClaimRecord;
import com.exam.claims.entity.ScriptDetail;
import com.exam.claims.util.AmountToWordsConverter;
import com.exam.claims.util.ClaimConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Core business logic for calculating claim amounts.
 * Handles three types: EXAMINER, ASSISTANT EXAMINER, CHIEF EXAMINER
 */
@Service
public class ClaimCalculationService {

    private static final Logger log = LoggerFactory.getLogger(ClaimCalculationService.class);

    /**
     * Calculate all claim amounts for a given record.
     * This method modifies the ClaimRecord in-place.
     */
    public void calculateClaim(ClaimRecord record) {
        log.debug("Calculating claim for: {} ({})", record.getStaffName(), record.getPostHeld());

        // Count total scripts from script details
        calculateScriptTotals(record);

        if (record.isAssistantExaminer()) {
            calculateAssistantExaminerClaim(record);
        } else if (record.isChiefExaminer()) {
            calculateChiefExaminerClaim(record);
        } else {
            calculateExaminerClaim(record);
        }

        // Convert total to words
        record.setAmountInWords(AmountToWordsConverter.convert(record.getTotalAmount()));

        log.debug("Claim calculated: {} = Rs. {}", record.getStaffName(), record.getTotalAmount());
    }

    /**
     * Count FN and AN scripts from the script details list
     */
    private void calculateScriptTotals(ClaimRecord record) {
        List<ScriptDetail> details = record.getScriptDetails();
        int fnTotal = 0;
        int anTotal = 0;

        if (details != null) {
            for (ScriptDetail detail : details) {
                int scripts = detail.getNoOfScripts() != null ? detail.getNoOfScripts() : 0;
                if ("FN".equalsIgnoreCase(detail.getSessionType())) {
                    fnTotal += scripts;
                } else if ("AN".equalsIgnoreCase(detail.getSessionType())) {
                    anTotal += scripts;
                }
            }
        }

        record.setFnScripts(fnTotal);
        record.setAnScripts(anTotal);
        record.setTotalScripts(fnTotal + anTotal);
    }

    // ==================== EXAMINER ====================

    /**
     * Calculate claim for regular EXAMINER.
     * Total = Script Amount + TA + DA
     */
    private void calculateExaminerClaim(ClaimRecord record) {
        BigDecimal scriptAmount = calculateScriptAmount(record.getTotalScripts());
        BigDecimal ta = calculateTA(record);
        BigDecimal da = calculateDA(record);

        record.setScriptAmount(scriptAmount);
        record.setTravellingAllowance(ta);
        record.setDearnessAllowance(da);
        record.setTotalAmount(scriptAmount.add(ta).add(da));
    }

    // ==================== CHIEF EXAMINER ====================

    /**
     * Calculate claim for CHIEF EXAMINER.
     * Script amount gets a 10% bonus.
     * Total = Overall Script Amount + TA + DA
     */
    private void calculateChiefExaminerClaim(ClaimRecord record) {
        int maxScripts = record.getTotalScripts();
        BigDecimal maxScriptsAmount = calculateScriptAmount(maxScripts);
        BigDecimal tenPercent = maxScriptsAmount.multiply(ClaimConstants.CHIEF_EXAMINER_BONUS_PERCENT)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal overallScriptAmount = maxScriptsAmount.add(tenPercent);

        BigDecimal ta = calculateTA(record);
        BigDecimal da = calculateDA(record);

        record.setMaxScriptsValued(maxScripts);
        record.setMaxScriptsAmount(maxScriptsAmount);
        record.setTenPercentAmount(tenPercent);
        record.setOverallScriptAmount(overallScriptAmount);
        record.setScriptAmount(maxScriptsAmount);
        record.setTravellingAllowance(ta);
        record.setDearnessAllowance(da);
        record.setTotalAmount(overallScriptAmount.add(ta).add(da));
    }

    // ==================== ASSISTANT EXAMINER ====================

    /**
     * Calculate claim for ASSISTANT EXAMINER.
     * Always internal staff -> TA = 0
     * Base Remuneration: Rs. 550 (Both Sessions / Full Day) or Rs. 275 (Single Session / Half Day)
     * DA: Rs. 0 on normal working days. Rs. 300 (Both Sessions) or Rs. 250 (Single Session) on Sundays/Holidays.
     * Total = Base Remuneration + DA
     */
    private void calculateAssistantExaminerClaim(ClaimRecord record) {
        // Base Remuneration (stored in scriptAmount for consistency in UI and documents)
        BigDecimal baseRemuneration = BigDecimal.ZERO;
        if (record.hasBothSessions()) {
            baseRemuneration = new BigDecimal("550.00");
        } else if (record.getSessionCount() == 1) {
            baseRemuneration = new BigDecimal("275.00");
        }

        // TA is strictly Rs. 0 for Assistant Examiners
        BigDecimal ta = BigDecimal.ZERO;

        // DA is Rs. 0 on normal working days, standard rates on holidays/Sundays
        BigDecimal da = BigDecimal.ZERO;
        boolean isHoliday = isHolidayOrSunday(record.getValuationDate(), record.isGovernmentHoliday());
        if (isHoliday) {
            if (record.hasBothSessions()) {
                da = ClaimConstants.DA_PER_DAY; // Rs. 300.00
            } else if (record.getSessionCount() == 1) {
                da = ClaimConstants.DA_PER_SESSION; // Rs. 250.00
            }
        }

        BigDecimal total = baseRemuneration.add(da);

        record.setScriptAmount(baseRemuneration);
        record.setTravellingAllowance(ta);
        record.setDearnessAllowance(da);
        record.setTotalAmount(total);
    }

    // ==================== COMMON CALCULATIONS ====================

    /**
     * Script Amount = Total Scripts × Rate Per Script (Rs. 30)
     */
    private BigDecimal calculateScriptAmount(int totalScripts) {
        return ClaimConstants.RATE_PER_SCRIPT
                .multiply(new BigDecimal(totalScripts))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Travelling Allowance calculation.
     * - Internal staff: TA = 0
     * - Round trip distance = one-way distance × 2
     * - If round trip <= 35 km: TA = Rs. 150 (flat)
     * - If round trip > 35 km: TA = round trip × Rs. 8/km
     */
    private BigDecimal calculateTA(ClaimRecord record) {
        // Internal staff: no TA
        if (isInternalStaff(record)) {
            return BigDecimal.ZERO;
        }

        BigDecimal distanceKm = record.getDistanceKm();
        if (distanceKm == null || distanceKm.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // Round trip = one-way × 2
        BigDecimal roundTrip = distanceKm.multiply(new BigDecimal("2"));

        if (roundTrip.compareTo(ClaimConstants.TA_DISTANCE_THRESHOLD_KM) <= 0) {
            return ClaimConstants.TA_FLAT_AMOUNT;
        } else {
            return roundTrip.multiply(ClaimConstants.TA_RATE_PER_KM)
                    .setScale(2, RoundingMode.HALF_UP);
        }
    }

    private boolean isHolidayOrSunday(java.time.LocalDate date, boolean isForcedHoliday) {
        if (date == null) return false;
        boolean isSunday = date.getDayOfWeek() == java.time.DayOfWeek.SUNDAY;
        return isSunday || isForcedHoliday;
    }

    /**
     * Dearness Allowance calculation.
     * - FN and AN (both sessions / full day): Rs. 300 per day
     * - FN or AN (single session): Rs. 250 per session
     */
    private BigDecimal calculateDA(ClaimRecord record) {
        boolean isInternal = isInternalStaff(record);
        boolean isHoliday = isHolidayOrSunday(record.getValuationDate(), record.isGovernmentHoliday());

        // Internal staff on a normal working day gets NO DA
        if (isInternal && !isHoliday) {
            return BigDecimal.ZERO;
        }

        if (record.hasBothSessions()) {
            return ClaimConstants.DA_PER_DAY;
        } else if (record.getSessionCount() == 1) {
            return ClaimConstants.DA_PER_SESSION;
        }
        return BigDecimal.ZERO;
    }

    /**
     * Check if the examiner is internal staff.
     */
    private boolean isInternalStaff(ClaimRecord record) {
        return record.isInternal();
    }
}

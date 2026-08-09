package com.exam.claims.service;

import com.exam.claims.entity.LabClaimRecord;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class LabClaimCalculationService {

    /**
     * Calculate remuneration, TA, DA, and total amount for a LabClaimRecord
     * strictly implementing the rates in Image 2 for all 4 roles:
     * 1. External Examiner
     * 2. Internal Examiner
     * 3. Skilled Assistant
     * 4. Supporting Staff / Lab Technician / Lab Attender
     */
    public void calculateLabClaim(LabClaimRecord record) {
        if (record == null) return;

        int appeared = record.getPresentCount() != null && record.getPresentCount() > 0 ? record.getPresentCount() : 0;
        int registered = record.getRegisteredCount() != null && record.getRegisteredCount() > 0 ? record.getRegisteredCount() : appeared;
        if (appeared == 0 && registered > 0) {
            appeared = registered;
        }

        String rawRole = record.getStaffRole() != null ? record.getStaffRole().toUpperCase().trim() : "";
        String roleCategory = getRoleCategory(rawRole);
        String subjectType = getNormalizedSubjectType(record);

        BigDecimal remAmount = BigDecimal.ZERO;

        switch (subjectType) {
            case "PRACTICAL_UG":
                if ("EXTERNAL_EXAMINER".equals(roleCategory) || "INTERNAL_EXAMINER".equals(roleCategory)) {
                    remAmount = calcMinMax(appeared, 25, 150);
                } else if ("SKILLED_ASSISTANT".equals(roleCategory)) {
                    remAmount = calcMinMax(registered, 12, 100);
                } else if ("SUPPORTING_STAFF".equals(roleCategory)) {
                    remAmount = calcMinMax(registered, 6, 50);
                }
                break;

            case "PRACTICAL_PG":
                if ("EXTERNAL_EXAMINER".equals(roleCategory) || "INTERNAL_EXAMINER".equals(roleCategory)) {
                    remAmount = calcMinMax(appeared, 30, 150);
                } else if ("SKILLED_ASSISTANT".equals(roleCategory)) {
                    remAmount = calcMinMax(registered, 12, 100);
                } else if ("SUPPORTING_STAFF".equals(roleCategory)) {
                    remAmount = calcMinMax(registered, 6, 50);
                }
                break;

            case "TCPL":
                if ("EXTERNAL_EXAMINER".equals(roleCategory) || "INTERNAL_EXAMINER".equals(roleCategory)) {
                    remAmount = calcMinMax(appeared, 20, 100);
                } else if ("SKILLED_ASSISTANT".equals(roleCategory)) {
                    remAmount = calcMinMax(registered, 10, 100);
                } else if ("SUPPORTING_STAFF".equals(roleCategory)) {
                    remAmount = calcMinMax(registered, 6, 50);
                }
                break;

            case "TCPR":
                if ("EXTERNAL_EXAMINER".equals(roleCategory) || "INTERNAL_EXAMINER".equals(roleCategory)) {
                    remAmount = calcMinMax(appeared, 20, 100);
                } else if ("SKILLED_ASSISTANT".equals(roleCategory)) {
                    remAmount = calcMinMax(registered, 10, 100);
                } else if ("SUPPORTING_STAFF".equals(roleCategory)) {
                    remAmount = calcMinMax(registered, 6, 50);
                }
                break;

            case "PROJECT_UG":
                if ("EXTERNAL_EXAMINER".equals(roleCategory) || "INTERNAL_EXAMINER".equals(roleCategory)) {
                    remAmount = calcMinMax(appeared, 25, 150);
                } else if ("SKILLED_ASSISTANT".equals(roleCategory)) {
                    remAmount = calcMinMax(registered, 12, 100);
                } else if ("SUPPORTING_STAFF".equals(roleCategory)) {
                    remAmount = calcMinMax(registered, 6, 50);
                }
                break;

            case "PROJECT_PG":
                if ("EXTERNAL_EXAMINER".equals(roleCategory)) {
                    remAmount = new BigDecimal(appeared * 250);
                } else if ("INTERNAL_EXAMINER".equals(roleCategory)) {
                    remAmount = new BigDecimal(appeared * 75);
                } else if ("SKILLED_ASSISTANT".equals(roleCategory)) {
                    remAmount = calcMinMax(registered, 12, 100);
                } else if ("SUPPORTING_STAFF".equals(roleCategory)) {
                    remAmount = calcMinMax(registered, 6, 50);
                }
                break;

            case "PROJECT_MBA":
                if ("EXTERNAL_EXAMINER".equals(roleCategory)) {
                    remAmount = new BigDecimal(appeared * 200);
                } else if ("INTERNAL_EXAMINER".equals(roleCategory)) {
                    remAmount = new BigDecimal(appeared * 75);
                } else if ("SKILLED_ASSISTANT".equals(roleCategory)) {
                    remAmount = calcMinMax(registered, 12, 100);
                } else if ("SUPPORTING_STAFF".equals(roleCategory)) {
                    remAmount = calcMinMax(registered, 6, 50);
                }
                break;

            default:
                if ("EXTERNAL_EXAMINER".equals(roleCategory) || "INTERNAL_EXAMINER".equals(roleCategory)) {
                    remAmount = calcMinMax(appeared, 25, 150);
                } else if ("SKILLED_ASSISTANT".equals(roleCategory)) {
                    remAmount = calcMinMax(registered, 12, 100);
                } else {
                    remAmount = calcMinMax(registered, 6, 50);
                }
                break;
        }

        record.setRemunerationAmount(remAmount.setScale(2, RoundingMode.HALF_UP));

        // TA & DA Calculation for External Examiner
        BigDecimal ta = BigDecimal.ZERO;
        BigDecimal da = BigDecimal.ZERO;

        if ("EXTERNAL_EXAMINER".equals(roleCategory)) {
            // TA Rule: <= 35 km -> Flat Rs 150; > 35 km -> Rs. 8 per km (To and Fro: km * 2 * 8)
            BigDecimal distance = record.getDistanceKm() != null ? record.getDistanceKm() : BigDecimal.ZERO;
            if (distance.compareTo(BigDecimal.ZERO) == 0 || distance.compareTo(new BigDecimal("35")) <= 0) {
                ta = new BigDecimal("150.00");
            } else {
                ta = distance.multiply(new BigDecimal("2")).multiply(new BigDecimal("8"));
            }

            // DA Rule: Rs. 300 per day (Both sessions) or Rs. 250 per session
            String session = record.getSession();
            if (session != null && session.toUpperCase().contains("BOTH")) {
                da = new BigDecimal("300.00");
            } else {
                da = new BigDecimal("250.00");
            }
        }

        record.setTaAmount(ta.setScale(2, RoundingMode.HALF_UP));
        record.setDaAmount(da.setScale(2, RoundingMode.HALF_UP));

        BigDecimal total = remAmount.add(ta).add(da);
        record.setTotalAmount(total.setScale(2, RoundingMode.HALF_UP));
    }

    public String getRoleCategory(String rawRole) {
        if (rawRole == null) return "SUPPORTING_STAFF";
        String u = rawRole.toUpperCase().trim();
        if (u.contains("EXTERNAL") || u.contains("EXT")) {
            return "EXTERNAL_EXAMINER";
        }
        if (u.contains("INTERNAL") || u.contains("INT")) {
            return "INTERNAL_EXAMINER";
        }
        if (u.contains("SKILLED")) {
            return "SKILLED_ASSISTANT";
        }
        return "SUPPORTING_STAFF";
    }

    private BigDecimal calcMinMax(int count, int rate, int minAmount) {
        int calculated = count * rate;
        return new BigDecimal(Math.max(calculated, minAmount));
    }

    private String getNormalizedSubjectType(LabClaimRecord record) {
        String subName = record.getSubjectName() != null ? record.getSubjectName().toUpperCase() : "";
        String subCode = record.getSubjectCode() != null ? record.getSubjectCode().toUpperCase() : "";
        String sem = record.getSemester() != null ? record.getSemester().toUpperCase() : "";

        if (subName.contains("TCPL") || subCode.contains("TCPL")) return "TCPL";
        if (subName.contains("TCPR") || subCode.contains("TCPR")) return "TCPR";

        if (subName.contains("MBA") || subCode.contains("MBA")) return "PROJECT_MBA";

        if (subName.contains("PROJECT") || subName.contains("MINI PROJECT") || subName.contains("PRBL")) {
            if ("VIII".equals(sem) || "VII".equals(sem) || "VI".equals(sem) || "V".equals(sem) || "IV".equals(sem)) {
                return "PROJECT_UG";
            }
            return "PROJECT_PG";
        }

        if ("PG".equalsIgnoreCase(record.getDepartment()) || "ME".equalsIgnoreCase(record.getDepartment()) || "M.E".equalsIgnoreCase(record.getDepartment()) || "M.TECH".equalsIgnoreCase(record.getDepartment())) {
            return "PRACTICAL_PG";
        }

        return "PRACTICAL_UG";
    }
}

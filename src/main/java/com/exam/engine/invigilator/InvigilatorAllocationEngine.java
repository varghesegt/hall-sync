package com.exam.engine.invigilator;

import com.exam.entity.Faculty;
import com.exam.entity.Hall;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Invigilator Allocation Engine.
 *
 * Assigns faculty members to internal exam halls respecting:
 * 1. Strict Department Isolation (Staff from Dept X cannot invigilate Dept X students).
 * 2. Hall Rotation (Staff members do NOT repeat the same hall/class position across sessions in CIA 1).
 * 3. Duty Tally & Student Strength Proportionality (Departments with more students take proportional duties).
 * 4. Capped Supporting Department Duties (Maths / S&H capped at 2 duties per section).
 * 5. CIA Maximum Duty Cap (Max 3 or 4 duties per faculty per CIA cycle).
 * 6. Year-Wise Duty Tracking & Same-Day Shift Isolation.
 */
public class InvigilatorAllocationEngine {

    public static final int DEFAULT_MAX_CIA_DUTIES = 4;

    public record DutyAssignment(
        Faculty faculty,
        Hall hall,
        String dutyType,
        Set<String> hallDepartments
    ) {}

    public record AllocationResult(
        List<DutyAssignment> assignments,
        List<String> warnings
    ) {}

    /**
     * @param availableFaculty     Active faculty pool
     * @param hallsWithDepts       Map of Hall -> Set of department names that have students in that hall
     * @param existingDutyCount    Map of facultyId -> number of duties already assigned in current CIA cycle
     * @param sameDayAssigned      Set of facultyIds already assigned in the other shift today
     * @param deptStudentCounts    Map of Dept -> Total Student Strength in this exam batch
     * @param facultyPreviousHalls Map of FacultyId -> Set of Hall ID Strings previously assigned in this CIA cycle (for Hall Rotation)
     */
    public AllocationResult allocate(
            List<Faculty> availableFaculty,
            Map<Hall, Set<String>> hallsWithDepts,
            Map<UUID, Long> existingDutyCount,
            Set<UUID> sameDayAssigned,
            Map<String, Integer> deptStudentCounts,
            Map<UUID, Set<String>> facultyPreviousHalls
    ) {
        if (availableFaculty.isEmpty() || hallsWithDepts.isEmpty()) {
            return new AllocationResult(List.of(), List.of("No faculty or halls provided."));
        }

        List<DutyAssignment> assignments = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // Mutable copy of duty counts for fairness tracking
        Map<UUID, Long> dutyTracker = new HashMap<>(existingDutyCount);
        for (Faculty f : availableFaculty) {
            dutyTracker.putIfAbsent(f.getId(), 0L);
        }

        // Track department-level duty counts in this allocation run
        Map<String, Integer> deptDutyTracker = new HashMap<>();

        // Calculate total student strength and target ratio per department
        int totalStudents = (deptStudentCounts != null && !deptStudentCounts.isEmpty())
                ? deptStudentCounts.values().stream().mapToInt(Integer::intValue).sum()
                : 1;

        // Track which faculty have been assigned in this run
        Set<UUID> assignedThisRun = new HashSet<>();

        // Sort halls by number of departments descending (harder multi-dept halls first)
        List<Map.Entry<Hall, Set<String>>> sortedHalls = hallsWithDepts.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
                .collect(Collectors.toList());

        for (Map.Entry<Hall, Set<String>> entry : sortedHalls) {
            Hall hall = entry.getKey();
            Set<String> hallDepts = entry.getValue();

            // Determine required invigilator count based on hall capacity
            int requiredInvigilators = 1;
            if (hall.getCapacity() != null && hall.getCapacity() > 80) requiredInvigilators = 3;
            else if (hall.getCapacity() != null && hall.getCapacity() > 40) requiredInvigilators = 2;

            for (int slot = 1; slot <= requiredInvigilators; slot++) {
                String dutyRole = (requiredInvigilators > 1 && slot == 1) ? "CHIEF_INVIGILATOR" : "INVIGILATOR";

                // Pass 1: Best Candidate (Dept isolation, Hall Rotation, Under CIA Max Cap <= 4, Student Strength Weighted, No Back-to-Back)
                Faculty bestCandidate = findBestCandidate(
                        availableFaculty, hall, hallDepts, dutyTracker, deptDutyTracker,
                        deptStudentCounts, totalStudents, assignedThisRun, sameDayAssigned,
                        facultyPreviousHalls, DEFAULT_MAX_CIA_DUTIES, true);

                // Pass 2: Relax Hall Rotation if necessary to fill duty tally
                if (bestCandidate == null) {
                    bestCandidate = findBestCandidate(
                            availableFaculty, hall, hallDepts, dutyTracker, deptDutyTracker,
                            deptStudentCounts, totalStudents, assignedThisRun, sameDayAssigned,
                            facultyPreviousHalls, DEFAULT_MAX_CIA_DUTIES, false);

                    if (bestCandidate != null) {
                        warnings.add("Hall " + hall.getName() + " (Slot " + slot + "): assigned " +
                                bestCandidate.getName() + " with repeated hall assignment (hall rotation relaxed).");
                    }
                }

                // Pass 3: Relax same-day shift constraint if necessary
                if (bestCandidate == null) {
                    bestCandidate = findBestCandidate(
                            availableFaculty, hall, hallDepts, dutyTracker, deptDutyTracker,
                            deptStudentCounts, totalStudents, assignedThisRun, Collections.emptySet(),
                            facultyPreviousHalls, DEFAULT_MAX_CIA_DUTIES, false);

                    if (bestCandidate != null) {
                        warnings.add("Hall " + hall.getName() + " (Slot " + slot + "): assigned " +
                                bestCandidate.getName() + " with back-to-back shift (same-day shift relaxed).");
                    }
                }

                // Pass 4: Relax CIA Duty Cap (allow > 4 duties) to ensure 100% duty count tally
                if (bestCandidate == null) {
                    bestCandidate = findBestCandidate(
                            availableFaculty, hall, hallDepts, dutyTracker, deptDutyTracker,
                            deptStudentCounts, totalStudents, assignedThisRun, Collections.emptySet(),
                            facultyPreviousHalls, 999, false);

                    if (bestCandidate != null) {
                        warnings.add("Hall " + hall.getName() + " (Slot " + slot + "): assigned " +
                                bestCandidate.getName() + " exceeding standard CIA max cap to complete total duty tally.");
                    }
                }

                // Pass 5: Full relaxation (allow dept overlap as absolute last resort)
                if (bestCandidate == null) {
                    bestCandidate = findLastResortCandidate(availableFaculty, dutyTracker, assignedThisRun);

                    if (bestCandidate != null) {
                        warnings.add("Hall " + hall.getName() + " (Slot " + slot + "): WARNING — assigned " +
                                bestCandidate.getName() + " (" + bestCandidate.getDepartment() +
                                ") despite department overlap due to insufficient faculty.");
                    }
                }

                if (bestCandidate != null) {
                    assignments.add(new DutyAssignment(bestCandidate, hall, dutyRole, hallDepts));
                    assignedThisRun.add(bestCandidate.getId());
                    dutyTracker.merge(bestCandidate.getId(), 1L, Long::sum);

                    String normDept = normalizeDept(bestCandidate.getDepartment());
                    deptDutyTracker.merge(normDept, 1, Integer::sum);
                } else {
                    warnings.add("Hall " + hall.getName() + " (Slot " + slot + "): UNASSIGNED — no available faculty.");
                }
            }
        }

        return new AllocationResult(assignments, warnings);
    }

    private Faculty findBestCandidate(
            List<Faculty> pool,
            Hall hall,
            Set<String> hallDepts,
            Map<UUID, Long> dutyTracker,
            Map<String, Integer> deptDutyTracker,
            Map<String, Integer> deptStudentCounts,
            int totalStudents,
            Set<UUID> assignedThisRun,
            Set<UUID> sameDayAssigned,
            Map<UUID, Set<String>> facultyPreviousHalls,
            int maxCiaCap,
            boolean enforceHallRotation
    ) {
        return pool.stream()
                .filter(f -> !assignedThisRun.contains(f.getId()))
                .filter(f -> !sameDayAssigned.contains(f.getId()))
                .filter(f -> !hasDeptConflict(f, hallDepts))
                .filter(f -> dutyTracker.getOrDefault(f.getId(), 0L) < maxCiaCap)
                .filter(f -> {
                    if (!enforceHallRotation || facultyPreviousHalls == null) return true;
                    Set<String> prevHalls = facultyPreviousHalls.getOrDefault(f.getId(), Collections.emptySet());
                    return !prevHalls.contains(hall.getId());
                })
                .min(Comparator
                        // 1. Supporting Dept Cap & Student Strength Proportionality:
                        .comparingDouble((Faculty f) -> calculateDeptOverQuotaScore(f, deptDutyTracker, deptStudentCounts, totalStudents))
                        // 2. Individual Workload Fairness:
                        .thenComparingLong(f -> dutyTracker.getOrDefault(f.getId(), 0L))
                )
                .orElse(null);
    }

    /**
     * Calculates department over-quota score to balance duty load according to student strength.
     * Lower score = higher priority for assignment.
     * Supporting departments (S&H, Maths, English) are capped at 2 duties per section.
     */
    private double calculateDeptOverQuotaScore(
            Faculty f,
            Map<String, Integer> deptDutyTracker,
            Map<String, Integer> deptStudentCounts,
            int totalStudents
    ) {
        String deptNorm = normalizeDept(f.getDepartment());
        int currentDeptDuties = deptDutyTracker.getOrDefault(deptNorm, 0);

        // Supporting / Other Dept (Maths, Physics, Chemistry, English, S&H)
        if (isSupportingDept(deptNorm)) {
            int totalSupportingStudents = deptStudentCounts != null ? deptStudentCounts.getOrDefault(deptNorm, 60) : 60;
            int sections = Math.max(1, (int) Math.ceil(totalSupportingStudents / 60.0));
            int maxSupportingQuota = sections * 2; // e.g. Maths - 2 duties per section

            if (currentDeptDuties >= maxSupportingQuota) {
                return 100.0 + currentDeptDuties;
            }
            return (double) currentDeptDuties / maxSupportingQuota;
        }

        // Core Engineering Dept (CSE, ECE, EEE, MECH, CIVIL, IT, AIDS)
        int deptStudents = (deptStudentCounts != null && deptStudentCounts.containsKey(deptNorm))
                ? deptStudentCounts.get(deptNorm)
                : 30;

        double expectedRatio = (double) deptStudents / totalStudents;
        return currentDeptDuties - (expectedRatio * 10.0);
    }

    private boolean isSupportingDept(String deptNorm) {
        if (deptNorm == null) return false;
        return deptNorm.contains("MATH") || deptNorm.contains("SH") || deptNorm.contains("ENGLISH")
                || deptNorm.contains("PHYSIC") || deptNorm.contains("CHEMISTR") || deptNorm.contains("SCIENCE");
    }

    private Faculty findLastResortCandidate(List<Faculty> pool,
                                             Map<UUID, Long> dutyTracker, Set<UUID> assigned) {
        return pool.stream()
                .filter(f -> !assigned.contains(f.getId()))
                .min(Comparator.comparingLong(f -> dutyTracker.getOrDefault(f.getId(), 0L)))
                .orElse(null);
    }

    public static boolean hasDeptConflict(Faculty faculty, Set<String> hallDepts) {
        if (faculty == null || faculty.getDepartment() == null || hallDepts == null || hallDepts.isEmpty()) {
            return false;
        }
        String facultyDeptNorm = normalizeDept(faculty.getDepartment());
        return hallDepts.stream()
                .anyMatch(d -> normalizeDept(d).equals(facultyDeptNorm));
    }

    public static String normalizeDept(String dept) {
        if (dept == null) return "";
        String d = dept.toUpperCase().replaceAll("[^A-Z]", "");
        if (d.contains("COMPUTER") || d.contains("CSE")) return "CSE";
        if (d.contains("ELECTRICAL") || d.contains("EEE")) return "EEE";
        if (d.contains("ELECTRONIC") || d.contains("ECE")) return "ECE";
        if (d.contains("MECHANICAL") || d.contains("MECH")) return "MECH";
        if (d.contains("INFORMATION") || d.contains("IT")) return "IT";
        if (d.contains("CIVIL")) return "CIVIL";
        if (d.contains("ARTIFICIAL") || d.contains("AIDS") || d.contains("AIML")) return "AIDS";
        if (d.contains("MATH")) return "MATHS";
        if (d.contains("SH") || d.contains("SCIENCE")) return "SH";
        return d;
    }
}

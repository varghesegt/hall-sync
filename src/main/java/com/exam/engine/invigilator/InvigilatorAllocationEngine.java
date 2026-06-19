package com.exam.engine.invigilator;

import com.exam.entity.Faculty;
import com.exam.entity.Hall;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Invigilator Allocation Engine.
 *
 * Assigns faculty members to exam halls while respecting constraints:
 * 1. A faculty member from department X should not invigilate a room
 *    where department X students are seated.
 * 2. Workload is distributed as fairly as possible across all faculty.
 * 3. Back-to-back shifts (FN + AN on the same day) are minimized.
 */
public class InvigilatorAllocationEngine {

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
     * @param availableFaculty  Active faculty pool
     * @param hallsWithDepts    Map of Hall -> Set of department names that have students in that hall
     * @param existingDutyCount Map of facultyId -> number of duties already assigned (for fairness)
     * @param sameDayAssigned   Set of facultyIds already assigned in the other shift today
     */
    public AllocationResult allocate(
            List<Faculty> availableFaculty,
            Map<Hall, Set<String>> hallsWithDepts,
            Map<UUID, Long> existingDutyCount,
            Set<UUID> sameDayAssigned
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

        // Track which faculty have been assigned in this run
        Set<UUID> assignedThisRun = new HashSet<>();

        // Sort halls by number of departments descending (harder halls first)
        List<Map.Entry<Hall, Set<String>>> sortedHalls = hallsWithDepts.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
                .collect(Collectors.toList());

        for (Map.Entry<Hall, Set<String>> entry : sortedHalls) {
            Hall hall = entry.getKey();
            Set<String> hallDepts = entry.getValue();

            // Find the best candidate: no dept conflict, lowest workload, not back-to-back
            Faculty bestCandidate = findBestCandidate(
                    availableFaculty, hallDepts, dutyTracker, assignedThisRun, sameDayAssigned);

            if (bestCandidate == null) {
                // Relax: allow back-to-back but still enforce dept constraint
                bestCandidate = findRelaxedCandidate(
                        availableFaculty, hallDepts, dutyTracker, assignedThisRun);

                if (bestCandidate != null) {
                    warnings.add("Hall " + hall.getName() + ": assigned " +
                            bestCandidate.getName() + " with back-to-back shift (relaxed).");
                }
            }

            if (bestCandidate == null) {
                // Full relaxation: allow dept overlap as last resort
                bestCandidate = findLastResortCandidate(availableFaculty, dutyTracker, assignedThisRun);

                if (bestCandidate != null) {
                    warnings.add("Hall " + hall.getName() + ": WARNING — assigned " +
                            bestCandidate.getName() + " (" + bestCandidate.getDepartment() +
                            ") despite department overlap due to insufficient faculty.");
                }
            }

            if (bestCandidate != null) {
                assignments.add(new DutyAssignment(bestCandidate, hall, "INVIGILATOR", hallDepts));
                assignedThisRun.add(bestCandidate.getId());
                dutyTracker.merge(bestCandidate.getId(), 1L, Long::sum);
            } else {
                warnings.add("Hall " + hall.getName() + ": UNASSIGNED — no available faculty.");
            }
        }

        return new AllocationResult(assignments, warnings);
    }

    private Faculty findBestCandidate(List<Faculty> pool, Set<String> hallDepts,
                                       Map<UUID, Long> dutyTracker, Set<UUID> assigned,
                                       Set<UUID> sameDayAssigned) {
        return pool.stream()
                .filter(f -> !assigned.contains(f.getId()))
                .filter(f -> !sameDayAssigned.contains(f.getId()))
                .filter(f -> !hasDeptConflict(f, hallDepts))
                .min(Comparator.comparingLong(f -> dutyTracker.getOrDefault(f.getId(), 0L)))
                .orElse(null);
    }

    private Faculty findRelaxedCandidate(List<Faculty> pool, Set<String> hallDepts,
                                          Map<UUID, Long> dutyTracker, Set<UUID> assigned) {
        return pool.stream()
                .filter(f -> !assigned.contains(f.getId()))
                .filter(f -> !hasDeptConflict(f, hallDepts))
                .min(Comparator.comparingLong(f -> dutyTracker.getOrDefault(f.getId(), 0L)))
                .orElse(null);
    }

    private Faculty findLastResortCandidate(List<Faculty> pool,
                                             Map<UUID, Long> dutyTracker, Set<UUID> assigned) {
        return pool.stream()
                .filter(f -> !assigned.contains(f.getId()))
                .min(Comparator.comparingLong(f -> dutyTracker.getOrDefault(f.getId(), 0L)))
                .orElse(null);
    }

    private boolean hasDeptConflict(Faculty faculty, Set<String> hallDepts) {
        String facultyDept = faculty.getDepartment().toUpperCase().trim();
        return hallDepts.stream()
                .anyMatch(d -> d.toUpperCase().trim().equals(facultyDept));
    }
}

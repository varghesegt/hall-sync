package com.exam.engine;

import com.exam.engine.model.Hall;
import com.exam.engine.model.Student;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.stream.Collectors;

/**
 * PRODUCTION-GRADE GLOBAL OPTIMIZER (V4 - DEPARTMENT PACKING + SUBJECT AUDIT)
 *
 * Rules:
 * 1. One Column = One Department (STRICT)
 * 2. No same department horizontally adjacent (guaranteed by ABABA interleaving)
 * 3. No same subject code horizontally adjacent (best-effort, audited)
 * 4. Minimize total hall count (pack 25 per hall in Phase A)
 * 5. Remainder halls use SPREAD mode (columns I, III, V only) with even splits
 * 6. Strict hall rotation via season history
 */
public class GlobalOptimizer {

    private static final Logger logger = LoggerFactory.getLogger(GlobalOptimizer.class);

    private final HallScoringSystem scorer = new HallScoringSystem();

    public record OptimizedAllocation(
        Map<String, List<Student>> hallAssignments,
        Map<String, String[]> hallPatterns,
        Map<String, String> reasoning,
        List<String> rotationWarnings
    ) {}

    public OptimizedAllocation optimize(List<Student> allStudents, List<Hall> allHalls, Map<String, Set<String>> seasonHistory) {
        Map<String, List<Student>> assignments = new LinkedHashMap<>();
        Map<String, String[]> patterns = new HashMap<>();
        Map<String, String> reasoning = new HashMap<>();
        List<String> rotationWarnings = new ArrayList<>();

        // 1. Group students by DEPARTMENT, sorted by size (largest first)
        Map<String, Queue<Student>> deptQueues = buildSortedDeptQueues(allStudents);

        List<Hall> sortedHalls = new ArrayList<>(allHalls);
        Collections.sort(sortedHalls);

        int hallIndex = 0;

        // =============================================
        // PHASE A: Fill halls AGGRESSIVELY with full column blocks
        // MINIMUM 3 columns (15 students) to justify a hall
        // Always try to fill all 5 columns (25 students)
        // =============================================
        while (hallIndex < sortedHalls.size()) {
            List<DeptBudget> fullDepts = getDeptsWithFullColumns(deptQueues);

            // CRITICAL: Reorder so that top1 and top2 have DIFFERENT subject codes
            // This prevents AIML(CGA1101) | AIDS(CGA1101) adjacency
            fullDepts = reorderForSubjectDiversity(fullDepts, deptQueues);

            // Stop Phase A if we can't fill at least 3 columns from big departments
            int totalAvailableColumns = fullDepts.stream().mapToInt(d -> d.fullColumns).sum();
            if (totalAvailableColumns < 3) break;

            // Strict Rotation: peek at students and find best non-conflicting hall
            List<Student> peekStudents = peekPotentialStudents(deptQueues, fullDepts);
            int bestHallIdx = findBestNonConflictingHall(hallIndex, sortedHalls, peekStudents, seasonHistory);
            if (bestHallIdx != hallIndex) {
                Hall chosen = sortedHalls.remove(bestHallIdx);
                sortedHalls.add(hallIndex, chosen);
                rotationWarnings.add("Rotated hall " + chosen.id() + " to avoid repeat assignment.");
            }

            Hall hall = sortedHalls.get(hallIndex);
            List<Student> hallStudents = new ArrayList<>();
            List<String> patternList = new ArrayList<>();
            StringBuilder rationale = new StringBuilder();

            if (fullDepts.size() >= 2) {
                DeptBudget top1 = fullDepts.get(0);
                DeptBudget top2 = fullDepts.get(1);

                if (top1.fullColumns >= 3 && top2.fullColumns >= 2) {
                    // ABABA (15:10 = 25) — BEST CASE
                    fillColumns(deptQueues, top1.dept, 3, hallStudents, patternList);
                    fillColumns(deptQueues, top2.dept, 2, hallStudents, patternList);
                    rationale.append("15:10 ABABA [").append(top1.dept).append("/").append(top2.dept).append("]");

                } else if (top1.fullColumns >= 2 && top2.fullColumns >= 2) {
                    // ABAB (10:10 = 20) + try to add a 5th column
                    fillColumns(deptQueues, top1.dept, 2, hallStudents, patternList);
                    fillColumns(deptQueues, top2.dept, 2, hallStudents, patternList);
                    rationale.append("10:10 ABAB [").append(top1.dept).append("/").append(top2.dept).append("]");

                    DeptBudget extra = findExtraColumn(deptQueues);
                    if (extra != null) {
                        fillColumns(deptQueues, extra.dept, 1, hallStudents, patternList);
                        rationale.append(" +5 [").append(extra.dept).append("]");
                    }

                } else if (top1.fullColumns >= 2 && top2.fullColumns >= 1) {
                    // ABA (10:5 = 15) + try to add more columns
                    fillColumns(deptQueues, top1.dept, 2, hallStudents, patternList);
                    fillColumns(deptQueues, top2.dept, 1, hallStudents, patternList);
                    rationale.append("10:5 ABA [").append(top1.dept).append("/").append(top2.dept).append("]");

                    for (int extra = 0; extra < 2; extra++) {
                        DeptBudget e = findExtraColumn(deptQueues);
                        if (e != null) {
                            fillColumns(deptQueues, e.dept, 1, hallStudents, patternList);
                            rationale.append(" +5 [").append(e.dept).append("]");
                        }
                    }

                } else {
                    // Both have only 1 full column each — need a 3rd dept to justify a hall
                    DeptBudget top3 = fullDepts.size() >= 3 ? fullDepts.get(2) : null;
                    if (top3 != null) {
                        fillColumns(deptQueues, top1.dept, 1, hallStudents, patternList);
                        fillColumns(deptQueues, top2.dept, 1, hallStudents, patternList);
                        fillColumns(deptQueues, top3.dept, 1, hallStudents, patternList);
                        rationale.append("5:5:5 ABC [").append(top1.dept).append("/").append(top2.dept).append("/").append(top3.dept).append("]");

                        for (int extra = 0; extra < 2; extra++) {
                            DeptBudget e = findExtraColumn(deptQueues);
                            if (e != null) {
                                fillColumns(deptQueues, e.dept, 1, hallStudents, patternList);
                                rationale.append(" +5 [").append(e.dept).append("]");
                            }
                        }
                    } else {
                        break;
                    }
                }
            } else if (fullDepts.size() == 1) {
                DeptBudget top1 = fullDepts.get(0);
                int cols = Math.min(5, top1.fullColumns);
                if (cols < 3) break;
                fillColumns(deptQueues, top1.dept, cols, hallStudents, patternList);
                rationale.append("Single-dept ").append(cols).append(" cols [").append(top1.dept).append("]");
            } else {
                break;
            }

            // Interleave the pattern for proper ABABA ordering
            String[] finalPattern = interleavePattern(patternList);

            if (!hallStudents.isEmpty()) {
                List<Student> reorderedStudents = reorderStudents(hallStudents, patternList, finalPattern);
                assignments.put(hall.id(), reorderedStudents);
                patterns.put(hall.id(), finalPattern);
                reasoning.put(hall.id(), rationale + "; Students: " + hallStudents.size());
                hallIndex++;
            } else {
                break;
            }
        }

        // =============================================
        // PHASE B: Pack ALL remaining students into minimal halls
        // SPREAD mode: columns I, III, V only (max 15 per hall)
        // Split EVENLY: 27 → 14 + 13, not 25 + 2
        // =============================================
        List<Student> remainderPool = new ArrayList<>();
        for (Queue<Student> q : deptQueues.values()) {
            remainderPool.addAll(q);
            q.clear();
        }

        int offset = 0;
        if (!remainderPool.isEmpty() && hallIndex < sortedHalls.size()) {
            // Interleave by subject code so adjacent columns have different subjects
            remainderPool = interleaveRemainderBySubject(remainderPool);

            int remainderCount = remainderPool.size();
            // Use max 25 per hall, but split evenly across needed halls
            int hallsNeeded = (int) Math.ceil((double) remainderCount / 25);
            int availableHalls = sortedHalls.size() - hallIndex;
            hallsNeeded = Math.min(hallsNeeded, availableHalls);
            if (hallsNeeded == 0) hallsNeeded = 1;

            // Calculate EVEN split
            int perHall = (int) Math.ceil((double) remainderCount / hallsNeeded);

            for (int h = 0; h < hallsNeeded && offset < remainderPool.size() && hallIndex < sortedHalls.size(); h++) {
                // Strict Rotation for remainder halls
                List<Student> sample = remainderPool.subList(offset, Math.min(offset + perHall, remainderPool.size()));
                int bestHallIdx = findBestNonConflictingHall(hallIndex, sortedHalls, sample, seasonHistory);
                if (bestHallIdx != hallIndex) {
                    Hall chosen = sortedHalls.remove(bestHallIdx);
                    sortedHalls.add(hallIndex, chosen);
                    rotationWarnings.add("Rotated remainder hall " + chosen.id() + " to avoid repeat assignment.");
                }

                Hall hall = sortedHalls.get(hallIndex);
                int end = Math.min(offset + perHall, remainderPool.size());
                List<Student> hallStudents = new ArrayList<>(remainderPool.subList(offset, end));

                String[] autoPattern = generatePatternFromStudents(hallStudents);

                assignments.put(hall.id(), hallStudents);
                patterns.put(hall.id(), autoPattern);
                reasoning.put(hall.id(), "Remainder (spread); Students: " + hallStudents.size() + "/" + remainderCount);

                offset = end;
                hallIndex++;
            }
        }

        // =============================================
        // PHASE C: GLOBAL CONSOLIDATION
        // Merge under-filled halls to minimize classroom usage
        // =============================================
        List<String> hallIds = new ArrayList<>(assignments.keySet());
        for (int i = hallIds.size() - 1; i >= 0; i--) {
            String currentHallId = hallIds.get(i);
            List<Student> currentStudents = assignments.get(currentHallId);
            String[] currentPattern = patterns.get(currentHallId);

            if (currentStudents == null) continue;

            for (int j = 0; j < i; j++) {
                String targetHallId = hallIds.get(j);
                List<Student> targetStudents = assignments.get(targetHallId);
                String[] targetPattern = patterns.get(targetHallId);

                if (targetStudents == null) continue;

                int combinedCount = targetStudents.size() + currentStudents.size();
                int combinedCols = targetPattern.length + currentPattern.length;

                if (combinedCount <= 25 && combinedCols <= 5) {
                    loggerDebug("Consolidating " + currentHallId + " into " + targetHallId);

                    List<String> combinedPatternList = new ArrayList<>();
                    combinedPatternList.addAll(Arrays.asList(targetPattern));
                    combinedPatternList.addAll(Arrays.asList(currentPattern));

                    List<Student> combinedStudents = new ArrayList<>();
                    combinedStudents.addAll(targetStudents);
                    combinedStudents.addAll(currentStudents);

                    String[] newInterleavedPattern = interleavePattern(combinedPatternList);
                    List<Student> reorderedStudents = reorderStudents(combinedStudents, combinedPatternList, newInterleavedPattern);

                    assignments.put(targetHallId, reorderedStudents);
                    patterns.put(targetHallId, newInterleavedPattern);
                    reasoning.put(targetHallId, reasoning.get(targetHallId) + " + [Consolidated " + currentHallId + "]; Students: " + combinedCount);

                    assignments.remove(currentHallId);
                    patterns.remove(currentHallId);
                    reasoning.remove(currentHallId);

                    break;
                }
            }
        }

        return new OptimizedAllocation(assignments, patterns, reasoning, rotationWarnings);
    }

    // ===================== ROTATION HELPERS =====================

    private int countHistoryConflicts(List<Student> students, String hallId, Map<String, Set<String>> history) {
        int conflicts = 0;
        for (Student s : students) {
            if (history.getOrDefault(s.registerNumber(), Collections.emptySet()).contains(hallId)) {
                conflicts++;
            }
        }
        return conflicts;
    }

    private int findBestNonConflictingHall(int startIndex, List<Hall> halls, List<Student> students, Map<String, Set<String>> history) {
        if (history.isEmpty()) return startIndex;
        int minConflicts = Integer.MAX_VALUE;
        int bestIdx = startIndex;
        for (int i = startIndex; i < halls.size(); i++) {
            int conflicts = countHistoryConflicts(students, halls.get(i).id(), history);
            if (conflicts == 0) return i;
            if (conflicts < minConflicts) {
                minConflicts = conflicts;
                bestIdx = i;
            }
        }
        return bestIdx;
    }

    private List<Student> peekPotentialStudents(Map<String, Queue<Student>> queues, List<DeptBudget> fullDepts) {
        List<Student> peek = new ArrayList<>();
        if (fullDepts.size() >= 2) {
            DeptBudget top1 = fullDepts.get(0);
            DeptBudget top2 = fullDepts.get(1);
            Queue<Student> q1 = queues.get(top1.dept);
            Queue<Student> q2 = queues.get(top2.dept);
            if (q1 != null) peek.addAll(q1.stream().limit(15).toList());
            if (q2 != null) peek.addAll(q2.stream().limit(10).toList());
        } else if (fullDepts.size() == 1) {
            Queue<Student> q = queues.get(fullDepts.get(0).dept);
            if (q != null) peek.addAll(q.stream().limit(25).toList());
        }
        return peek;
    }

    // ===================== COLUMN HELPERS =====================

    private void loggerDebug(String msg) {
        logger.debug("[OPTIMIZER] {}", msg);
    }

    /** Pull exactly N columns (5 students each) from the given department */
    private void fillColumns(Map<String, Queue<Student>> queues, String dept,
                             int numColumns, List<Student> target, List<String> pattern) {
        Queue<Student> q = queues.get(dept);
        if (q == null) return;
        for (int col = 0; col < numColumns; col++) {
            for (int i = 0; i < 5 && !q.isEmpty(); i++) {
                target.add(q.poll());
            }
            pattern.add(dept);
        }
    }

    /** Find ANY department that has a full column (5+ students) available */
    private DeptBudget findExtraColumn(Map<String, Queue<Student>> queues) {
        return queues.entrySet().stream()
                .filter(e -> e.getValue().size() >= 5)
                .max(Comparator.comparingInt(e -> e.getValue().size()))
                .map(e -> new DeptBudget(e.getKey(), e.getValue().size() / 5))
                .orElse(null);
    }

    /** Get departments that can fill at least one full column (5+ students) */
    private List<DeptBudget> getDeptsWithFullColumns(Map<String, Queue<Student>> queues) {
        return queues.entrySet().stream()
                .filter(e -> e.getValue().size() >= 5)
                .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
                .map(e -> new DeptBudget(e.getKey(), e.getValue().size() / 5))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /** Get the dominant subject code for a department's student queue */
    private String getDeptSubjectCode(String dept, Map<String, Queue<Student>> queues) {
        Queue<Student> q = queues.get(dept);
        if (q == null || q.isEmpty()) return "N/A";
        Student first = q.peek();
        return (first.subjectCode() != null && !first.subjectCode().isBlank())
                ? first.subjectCode().trim().toUpperCase() : "N/A";
    }

    /**
     * Reorder fullDepts so that top1 and top2 have DIFFERENT subject codes.
     * If AIML(CGA1101) and AIDS(CGA1101) are top1/top2, this will swap top2 with
     * the first department that has a different subject code (e.g., ME(CSE) with PCSMC14).
     * If no such department exists, the original order is preserved (unavoidable).
     */
    private List<DeptBudget> reorderForSubjectDiversity(List<DeptBudget> fullDepts, Map<String, Queue<Student>> queues) {
        if (fullDepts.size() <= 1) return fullDepts;

        String top1Subject = getDeptSubjectCode(fullDepts.get(0).dept, queues);
        String top2Subject = getDeptSubjectCode(fullDepts.get(1).dept, queues);

        // If top1 and top2 already have different subjects, no change needed
        if (!top1Subject.equals(top2Subject)) return fullDepts;

        // Find the first department with a DIFFERENT subject code
        for (int i = 2; i < fullDepts.size(); i++) {
            String candidateSubject = getDeptSubjectCode(fullDepts.get(i).dept, queues);
            if (!candidateSubject.equals(top1Subject)) {
                // Promote this department to position 1 (top2 slot)
                List<DeptBudget> reordered = new ArrayList<>(fullDepts);
                DeptBudget better = reordered.remove(i);
                reordered.add(1, better);
                return reordered;
            }
        }

        // No department with a different subject exists — unavoidable
        return fullDepts;
    }

    // ===================== PATTERN HELPERS =====================

    /**
     * Interleave the pattern for proper ABACA ordering.
     * The LARGEST group gets EVEN positions (0, 2, 4) and
     * all other groups fill ODD positions (1, 3).
     * 
     * Example: [ECE, ECE, ECE, EEE, MECH] → [ECE, EEE, ECE, MECH, ECE]
     * This GUARANTEES no same-department adjacency when majority ≤ ceil(n/2).
     */
    private String[] interleavePattern(List<String> patternList) {
        if (patternList.size() <= 1) return patternList.toArray(new String[0]);

        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String dept : patternList) {
            counts.merge(dept, 1, Integer::sum);
        }

        List<Map.Entry<String, Integer>> sorted = counts.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .collect(Collectors.toCollection(ArrayList::new));

        String[] result = new String[patternList.size()];

        if (sorted.size() >= 2) {
            String majorDept = sorted.get(0).getKey();
            int majorCount = sorted.get(0).getValue();

            // Collect all minor departments in order (largest minor first)
            List<String> minors = new ArrayList<>();
            for (int i = 1; i < sorted.size(); i++) {
                for (int j = 0; j < sorted.get(i).getValue(); j++) {
                    minors.add(sorted.get(i).getKey());
                }
            }

            // Place major at even positions (0, 2, 4), minors at odd (1, 3)
            int majorIdx = 0;
            int minorIdx = 0;
            for (int i = 0; i < result.length; i++) {
                if (i % 2 == 0 && majorIdx < majorCount) {
                    result[i] = majorDept;
                    majorIdx++;
                } else if (minorIdx < minors.size()) {
                    result[i] = minors.get(minorIdx);
                    minorIdx++;
                } else if (majorIdx < majorCount) {
                    result[i] = majorDept;
                    majorIdx++;
                }
            }
        } else {
            Arrays.fill(result, sorted.get(0).getKey());
        }

        return result;
    }

    /**
     * Reorder students to match the interleaved pattern.
     * Students come in as [A,A,A,B,B] blocks, reorder to match [A,B,A,B,A].
     * Groups by DEPARTMENT (not subject code).
     */
    private List<Student> reorderStudents(List<Student> students, List<String> originalPattern, String[] interleavedPattern) {
        Map<String, Queue<Student>> byDept = new LinkedHashMap<>();
        for (Student s : students) {
            byDept.computeIfAbsent(s.department(), k -> new LinkedList<>()).add(s);
        }

        List<Student> reordered = new ArrayList<>();
        for (String dept : interleavedPattern) {
            Queue<Student> q = byDept.get(dept);
            if (q != null) {
                for (int i = 0; i < 5 && !q.isEmpty(); i++) {
                    reordered.add(q.poll());
                }
            }
        }

        // Safety net: add any remaining students
        for (Queue<Student> q : byDept.values()) {
            while (!q.isEmpty()) reordered.add(q.poll());
        }

        return reordered;
    }

    /** Generate a column pattern from the actual students in a remainder hall (by DEPARTMENT) */
    private String[] generatePatternFromStudents(List<Student> students) {
        Map<String, Long> counts = students.stream()
                .collect(Collectors.groupingBy(s -> s.department(), Collectors.counting()));
        List<String> depts = counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(ArrayList::new));

        List<String> pattern = new ArrayList<>();
        for (String dept : depts) {
            int cols = (int) Math.ceil((double) counts.get(dept) / 5);
            for (int i = 0; i < cols; i++) pattern.add(dept);
        }

        if (pattern.size() > 5) pattern = pattern.subList(0, 5);
        if (pattern.isEmpty()) pattern.add("UNKNOWN");

        // Apply subject-diversity: reorder so adjacent entries don't share a subject code
        pattern = applySubjectDiversityToPattern(pattern, students);

        return interleavePattern(pattern);
    }

    /**
     * Reorder a pattern list so that adjacent entries don't share a subject code.
     * E.g., if AIDS(ADI1221) and AIML(ADI1221) are adjacent, swap AIML with a
     * department that has a different subject code.
     */
    private List<String> applySubjectDiversityToPattern(List<String> pattern, List<Student> students) {
        if (pattern.size() <= 1) return pattern;

        // Build dept -> dominant subject map
        Map<String, String> deptSubject = new HashMap<>();
        for (Student s : students) {
            deptSubject.putIfAbsent(s.department(),
                (s.subjectCode() != null && !s.subjectCode().isBlank())
                    ? s.subjectCode().trim().toUpperCase() : "N/A");
        }

        List<String> result = new ArrayList<>(pattern);
        for (int i = 0; i < result.size() - 1; i++) {
            String subA = deptSubject.getOrDefault(result.get(i), "N/A");
            String subB = deptSubject.getOrDefault(result.get(i + 1), "N/A");
            if (subA.equals(subB)) {
                // Find a later entry with a different subject to swap
                for (int j = i + 2; j < result.size(); j++) {
                    String subC = deptSubject.getOrDefault(result.get(j), "N/A");
                    if (!subC.equals(subA)) {
                        Collections.swap(result, i + 1, j);
                        break;
                    }
                }
            }
        }
        return result;
    }

    /**
     * Interleave remainder students so adjacent column-blocks (groups of 5)
     * have different subject codes. Prevents same-subject horizontal adjacency.
     */
    private List<Student> interleaveRemainderBySubject(List<Student> students) {
        if (students.size() <= 5) return students;

        // Group by subject code
        Map<String, List<Student>> bySubject = new LinkedHashMap<>();
        for (Student s : students) {
            String sub = (s.subjectCode() != null && !s.subjectCode().isBlank())
                         ? s.subjectCode().trim().toUpperCase() : "N/A";
            bySubject.computeIfAbsent(sub, k -> new ArrayList<>()).add(s);
        }

        if (bySubject.size() <= 1) {
            // Only one subject — sort by department for best column purity
            students.sort(Comparator.comparing((Student s) -> s.department())
                    .thenComparing(s -> s.registerNumber()));
            return students;
        }

        // Sort each subject group internally by department for column purity
        for (List<Student> group : bySubject.values()) {
            group.sort(Comparator.comparing((Student s) -> s.department())
                    .thenComparing(s -> s.registerNumber()));
        }

        // Sort subject groups by size descending
        List<Map.Entry<String, List<Student>>> sorted = bySubject.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
                .collect(Collectors.toList());

        // Interleave: take column-sized chunks (up to 5) alternating subjects
        // Major subject at even positions, minors at odd positions
        List<Student> result = new ArrayList<>();
        List<Queue<Student>> queues = new ArrayList<>();
        for (var entry : sorted) {
            queues.add(new LinkedList<>(entry.getValue()));
        }

        int qIdx = 0;
        while (result.size() < students.size()) {
            boolean added = false;
            for (int attempt = 0; attempt < queues.size(); attempt++) {
                int idx = (qIdx + attempt) % queues.size();
                if (!queues.get(idx).isEmpty()) {
                    for (int i = 0; i < 5 && !queues.get(idx).isEmpty(); i++) {
                        result.add(queues.get(idx).poll());
                    }
                    qIdx = (idx + 1) % queues.size();
                    added = true;
                    break;
                }
            }
            if (!added) break;
        }
        return result;
    }

    // ===================== QUEUE BUILDER =====================

    /** Group students by DEPARTMENT, sorted by department size (largest first) */
    private Map<String, Queue<Student>> buildSortedDeptQueues(List<Student> students) {
        Map<String, Queue<Student>> queues = new LinkedHashMap<>();
        students.stream()
                .collect(Collectors.groupingBy(s -> s.department(), LinkedHashMap::new, Collectors.toList()))
                .entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
                .forEach(e -> queues.put(e.getKey(), new LinkedList<>(e.getValue())));
        return queues;
    }

    private record DeptBudget(String dept, int fullColumns) {}
}

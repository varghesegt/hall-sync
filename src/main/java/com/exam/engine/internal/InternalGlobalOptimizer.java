package com.exam.engine.internal;

import com.exam.engine.model.Hall;
import com.exam.engine.model.Student;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.stream.Collectors;

/**
 * INTERNAL EXAM GLOBAL OPTIMIZER (7×6 GRID)
 *
 * Adapted from semester GlobalOptimizer for 7 rows × 6 columns.
 * Max 40 students per hall, 7 students per column, 6 columns per hall.
 *
 * Rules:
 * 1. One Column = One Department (STRICT)
 * 2. No same department horizontally adjacent (guaranteed by interleaving)
 * 3. No same subject code horizontally adjacent (best-effort, audited)
 * 4. Minimize total hall count (pack 40 per hall in Phase A, strict 20:20 ABABAB)
 * 5. Remainder halls use SPREAD mode or mixed
 * 6. Strict hall rotation via season history
 */
public class InternalGlobalOptimizer {

    private static final Logger logger = LoggerFactory.getLogger(InternalGlobalOptimizer.class);
    private static final int ROWS = 7;
    private static final int COLS = 6;
    private static final int MAX_STUDENTS = 40;
    
    // The exact capacities of each of the 6 columns in a 40-seat hall: 7, 7, 7, 7, 6, 6
    private static final int[] COL_CAPACITIES = {7, 7, 7, 7, 6, 6};

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
        // PHASE A: Fill halls AGGRESSIVELY with 40 students
        // 6 columns per hall = 40 seats (7,7,7,7,6,6)
        // =============================================
        while (hallIndex < sortedHalls.size()) {
            Hall hall = sortedHalls.get(hallIndex);
            int maxCols = hall.cols();
            int maxRows = hall.rows();
            int maxCapacity = hall.capacity();

            List<DeptBudget> fullDepts = getDeptsWithFullColumns(deptQueues, maxRows - 1); // internal usually has slightly uneven columns
            fullDepts = reorderForSubjectDiversity(fullDepts, deptQueues);

            int minColsNeeded = (int) Math.ceil((double) maxCols / 2.0);
            int totalAvailable = fullDepts.stream().mapToInt(d -> d.students).sum();
            if (totalAvailable < (minColsNeeded * maxRows - 2)) break;

            // Strict Rotation
            List<Student> peekStudents = peekPotentialStudents(deptQueues, fullDepts, maxRows);
            int bestHallIdx = findBestNonConflictingHall(hallIndex, sortedHalls, peekStudents, seasonHistory);
            if (bestHallIdx != hallIndex) {
                Hall chosen = sortedHalls.remove(bestHallIdx);
                sortedHalls.add(hallIndex, chosen);
                hall = chosen;
                maxCols = hall.cols();
                maxRows = hall.rows();
                maxCapacity = hall.capacity();
                rotationWarnings.add("Rotated hall " + chosen.id() + " to avoid repeat assignment.");
            }

            List<Student> hallStudents = new ArrayList<>();
            List<String> patternList = new ArrayList<>();
            StringBuilder rationale = new StringBuilder();

            if (fullDepts.size() >= 2) {
                DeptBudget top1 = fullDepts.get(0);
                DeptBudget top2 = fullDepts.get(1);

                int halfCapacity = maxCapacity / 2;
                int cols1 = (int) Math.ceil((double) maxCols / 2.0);
                int cols2 = maxCols - cols1;

                if (top1.students >= halfCapacity && top2.students >= halfCapacity) {
                    // ABABAB 
                    fillExact(deptQueues, top1.dept, halfCapacity, hallStudents, patternList, cols1);
                    fillExact(deptQueues, top2.dept, halfCapacity, hallStudents, patternList, cols2);
                    rationale.append(halfCapacity).append(":").append(halfCapacity).append(" ABABAB [").append(top1.dept).append("/").append(top2.dept).append("]");
                } else if (top1.students >= halfCapacity && top2.students >= (halfCapacity / 2) && fullDepts.size() >= 3 && fullDepts.get(2).students >= (halfCapacity / 2)) {
                    // ABACBA 
                    DeptBudget top3 = fullDepts.get(2);
                    int third = halfCapacity / 2;
                    fillExact(deptQueues, top1.dept, halfCapacity, hallStudents, patternList, cols1);
                    fillExact(deptQueues, top2.dept, third, hallStudents, patternList, cols2 / 2);
                    fillExact(deptQueues, top3.dept, third, hallStudents, patternList, cols2 - (cols2 / 2));
                    rationale.append(halfCapacity).append(":").append(third).append(":").append(third).append(" ABACBA [").append(top1.dept).append("/").append(top2.dept).append("/").append(top3.dept).append("]");
                } else if (top1.students >= (halfCapacity / 2 + 5) && top2.students >= (halfCapacity / 2 + 5)) {
                    int amount = halfCapacity / 2 + 5;
                    fillExact(deptQueues, top1.dept, amount, hallStudents, patternList, cols1 - 1);
                    fillExact(deptQueues, top2.dept, amount, hallStudents, patternList, cols2);
                    rationale.append(amount).append(":").append(amount).append(" ABAB [").append(top1.dept).append("/").append(top2.dept).append("]");
                } else {
                    break;
                }
            } else if (fullDepts.size() == 1) {
                DeptBudget top1 = fullDepts.get(0);
                int take = Math.min(maxCapacity, top1.students);
                int cols = (int) Math.ceil((double) take / maxRows);
                fillExact(deptQueues, top1.dept, take, hallStudents, patternList, cols);
                rationale.append("Single-dept ").append(take).append(" students [").append(top1.dept).append("]");
            } else {
                break;
            }

            String[] finalPattern = interleavePattern(patternList);

            if (!hallStudents.isEmpty()) {
                List<Student> reorderedStudents = reorderStudents(hallStudents, patternList, finalPattern, maxRows);
                assignments.put(hall.id(), reorderedStudents);
                patterns.put(hall.id(), finalPattern);
                reasoning.put(hall.id(), rationale + "; Students: " + hallStudents.size());
                hallIndex++;
            } else {
                break;
            }
        }

        // =============================================
        // PHASE B: Pack remaining students into minimal halls
        // =============================================
        List<Student> remainderPool = new ArrayList<>();
        for (Queue<Student> q : deptQueues.values()) {
            remainderPool.addAll(q);
            q.clear();
        }

        int offset = 0;
        if (!remainderPool.isEmpty() && hallIndex < sortedHalls.size()) {
            remainderPool = interleaveRemainderBySubject(remainderPool);

            int remainderCount = remainderPool.size();
            
            // Estimate halls needed based on full capacity
            int hallsNeeded = 1;
            int currentEstim = 0;
            for (int i = hallIndex; i < sortedHalls.size(); i++) {
                currentEstim += sortedHalls.get(i).capacity();
                if (currentEstim >= remainderCount) {
                    hallsNeeded = i - hallIndex + 1;
                    break;
                }
            }
            int availableHalls = sortedHalls.size() - hallIndex;
            hallsNeeded = Math.min(hallsNeeded, availableHalls);
            if (hallsNeeded == 0) hallsNeeded = 1;

            int perHall = (int) Math.ceil((double) remainderCount / hallsNeeded);

            for (int h = 0; h < hallsNeeded && offset < remainderPool.size() && hallIndex < sortedHalls.size(); h++) {
                List<Student> sample = remainderPool.subList(offset, Math.min(offset + perHall, remainderPool.size()));
                int bestHallIdx = findBestNonConflictingHall(hallIndex, sortedHalls, sample, seasonHistory);
                if (bestHallIdx != hallIndex) {
                    Hall chosen = sortedHalls.remove(bestHallIdx);
                    sortedHalls.add(hallIndex, chosen);
                    rotationWarnings.add("Rotated remainder hall " + chosen.id() + " to avoid repeat assignment.");
                }

                Hall hall = sortedHalls.get(hallIndex);
                int end = Math.min(offset + perHall, remainderPool.size());
                
                if (end - offset > hall.capacity()) {
                    end = offset + hall.capacity();
                }

                List<Student> hallStudents = new ArrayList<>(remainderPool.subList(offset, end));

                String[] autoPattern = generatePatternFromStudents(hallStudents, hall.cols(), hall.rows());
                List<Student> reorderedStudents = reorderStudents(hallStudents, null, autoPattern, hall.rows());

                assignments.put(hall.id(), reorderedStudents);
                patterns.put(hall.id(), autoPattern);
                reasoning.put(hall.id(), "Remainder (spread); Students: " + hallStudents.size() + "/" + remainderCount);

                offset = end;
                hallIndex++;
            }
        }

        // =============================================
        // PHASE C: GLOBAL CONSOLIDATION
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
                
                Hall targetHall = sortedHalls.stream().filter(h -> h.id().equals(targetHallId)).findFirst().orElse(new Hall(targetHallId, 40, 7, 6));

                int combinedCount = targetStudents.size() + currentStudents.size();
                int combinedCols = targetPattern.length + currentPattern.length;

                if (combinedCount <= targetHall.capacity() && combinedCols <= targetHall.cols()) {
                    logger.debug("[INTERNAL-OPTIMIZER] Consolidating {} into {}", currentHallId, targetHallId);

                    List<String> combinedPatternList = new ArrayList<>();
                    combinedPatternList.addAll(Arrays.asList(targetPattern));
                    combinedPatternList.addAll(Arrays.asList(currentPattern));

                    List<Student> combinedStudents = new ArrayList<>();
                    combinedStudents.addAll(targetStudents);
                    combinedStudents.addAll(currentStudents);

                    String[] newInterleavedPattern = interleavePattern(combinedPatternList);
                    List<Student> reorderedStudents = reorderStudents(combinedStudents, combinedPatternList, newInterleavedPattern, targetHall.rows());

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

    private List<Student> peekPotentialStudents(Map<String, Queue<Student>> queues, List<DeptBudget> fullDepts, int maxRows) {
        List<Student> peek = new ArrayList<>();
        if (fullDepts.size() >= 2) {
            DeptBudget top1 = fullDepts.get(0);
            DeptBudget top2 = fullDepts.get(1);
            Queue<Student> q1 = queues.get(top1.dept);
            Queue<Student> q2 = queues.get(top2.dept);
            if (q1 != null) peek.addAll(q1.stream().limit(maxRows * 3).toList());
            if (q2 != null) peek.addAll(q2.stream().limit(maxRows * 3).toList());
        } else if (fullDepts.size() == 1) {
            Queue<Student> q = queues.get(fullDepts.get(0).dept);
            if (q != null) peek.addAll(q.stream().limit(maxRows * 6).toList());
        }
        return peek;
    }

    // ===================== COLUMN HELPERS =====================

    private void fillExact(Map<String, Queue<Student>> queues, String dept,
                             int count, List<Student> target, List<String> pattern, int numCols) {
        Queue<Student> q = queues.get(dept);
        if (q == null) return;
        for (int i = 0; i < count && !q.isEmpty(); i++) {
            target.add(q.poll());
        }
        for (int i = 0; i < numCols; i++) {
            pattern.add(dept);
        }
    }

    private DeptBudget findExtraColumn(Map<String, Queue<Student>> queues) {
        return queues.entrySet().stream()
                .filter(e -> e.getValue().size() >= 7)
                .max(Comparator.comparingInt(e -> e.getValue().size()))
                .map(e -> new DeptBudget(e.getKey(), e.getValue().size() / 7))
                .orElse(null);
    }

    private List<DeptBudget> getDeptsWithFullColumns(Map<String, Queue<Student>> queues, int maxRowsForInternalCol) {
        return queues.entrySet().stream()
                .filter(e -> e.getValue().size() >= maxRowsForInternalCol) 
                .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
                .map(e -> new DeptBudget(e.getKey(), e.getValue().size()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private String getDeptSubjectCode(String dept, Map<String, Queue<Student>> queues) {
        Queue<Student> q = queues.get(dept);
        if (q == null || q.isEmpty()) return "N/A";
        Student first = q.peek();
        return (first.subjectCode() != null && !first.subjectCode().isBlank())
                ? first.subjectCode().trim().toUpperCase() : "N/A";
    }

    private List<DeptBudget> reorderForSubjectDiversity(List<DeptBudget> fullDepts, Map<String, Queue<Student>> queues) {
        if (fullDepts.size() <= 1) return fullDepts;

        String top1Subject = getDeptSubjectCode(fullDepts.get(0).dept, queues);
        String top2Subject = getDeptSubjectCode(fullDepts.get(1).dept, queues);

        if (!top1Subject.equals(top2Subject)) return fullDepts;

        for (int i = 2; i < fullDepts.size(); i++) {
            String candidateSubject = getDeptSubjectCode(fullDepts.get(i).dept, queues);
            if (!candidateSubject.equals(top1Subject)) {
                List<DeptBudget> reordered = new ArrayList<>(fullDepts);
                DeptBudget better = reordered.remove(i);
                reordered.add(1, better);
                return reordered;
            }
        }

        return fullDepts;
    }

    // ===================== PATTERN HELPERS =====================

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

            List<String> minors = new ArrayList<>();
            for (int i = 1; i < sorted.size(); i++) {
                for (int j = 0; j < sorted.get(i).getValue(); j++) {
                    minors.add(sorted.get(i).getKey());
                }
            }

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

    private List<Student> reorderStudents(List<Student> students, List<String> originalPattern, String[] interleavedPattern, int maxRows) {
        Map<String, Queue<Student>> byDept = new LinkedHashMap<>();
        for (Student s : students) {
            byDept.computeIfAbsent(s.department(), k -> new LinkedList<>()).add(s);
        }

        List<Student> reordered = new ArrayList<>();
        
        for (int i = 0; i < interleavedPattern.length; i++) {
            String dept = interleavedPattern[i];
            Queue<Student> q = byDept.get(dept);
            int capacity = maxRows;
            
            if (q != null) {
                for (int j = 0; j < capacity && !q.isEmpty(); j++) {
                    reordered.add(q.poll());
                }
            }
        }

        for (Queue<Student> q : byDept.values()) {
            while (!q.isEmpty()) reordered.add(q.poll());
        }

        return reordered;
    }

    private String[] generatePatternFromStudents(List<Student> students, int maxCols, int maxRows) {
        Map<String, Long> counts = students.stream()
                .collect(Collectors.groupingBy(Student::department, Collectors.counting()));
        List<String> depts = counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(ArrayList::new));

        List<String> pattern = new ArrayList<>();
        for (String dept : depts) {
            int cols = (int) Math.ceil((double) counts.get(dept) / maxRows);
            for (int i = 0; i < cols; i++) pattern.add(dept);
        }

        if (pattern.size() > maxCols) pattern = pattern.subList(0, maxCols);
        if (pattern.isEmpty()) pattern.add("UNKNOWN");

        pattern = applySubjectDiversityToPattern(pattern, students);

        return interleavePattern(pattern);
    }

    private List<String> applySubjectDiversityToPattern(List<String> pattern, List<Student> students) {
        if (pattern.size() <= 1) return pattern;

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

    private List<Student> interleaveRemainderBySubject(List<Student> students) {
        if (students.size() <= 7) return students;

        Map<String, List<Student>> bySubject = new LinkedHashMap<>();
        for (Student s : students) {
            String sub = (s.subjectCode() != null && !s.subjectCode().isBlank())
                         ? s.subjectCode().trim().toUpperCase() : "N/A";
            bySubject.computeIfAbsent(sub, k -> new ArrayList<>()).add(s);
        }

        if (bySubject.size() <= 1) {
            students.sort(Comparator.comparing((Student s) -> s.department())
                    .thenComparing(Student::registerNumber));
            return students;
        }

        for (List<Student> group : bySubject.values()) {
            group.sort(Comparator.comparing((Student s) -> s.department())
                    .thenComparing(Student::registerNumber));
        }

        List<Map.Entry<String, List<Student>>> sorted = bySubject.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
                .collect(Collectors.toList());

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
                    for (int i = 0; i < 7 && !queues.get(idx).isEmpty(); i++) {
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

    private Map<String, Queue<Student>> buildSortedDeptQueues(List<Student> students) {
        Map<String, Queue<Student>> queues = new LinkedHashMap<>();
        students.stream()
                .collect(Collectors.groupingBy(Student::department, LinkedHashMap::new, Collectors.toList()))
                .entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
                .forEach(e -> queues.put(e.getKey(), new LinkedList<>(e.getValue())));
        return queues;
    }

    private record DeptBudget(String dept, int students) {}
}

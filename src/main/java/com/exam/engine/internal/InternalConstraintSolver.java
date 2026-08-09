package com.exam.engine.internal;

import com.exam.engine.AllocationConstraintSolver;
import com.exam.engine.model.AllocationViolation;
import com.exam.engine.model.SeatAssignment;
import com.exam.engine.model.Student;
import com.exam.engine.model.ViolationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * INTERNAL EXAM CONSTRAINT SOLVER (DYNAMIC GRID)
 *
 * Adapted from semester AllocationConstraintSolver for internal exams.
 * Grid dimensions (rows × cols) are now per-hall parameters.
 * Default: 7×6 (42 seats, 40 max students).
 *
 * HARD CONSTRAINTS:
 *   1. One Column = One Department (STRICT)
 *   2. No same department horizontally adjacent (guaranteed by pattern)
 *   3. No same subject code horizontally adjacent (audited, best-effort swap)
 *
 * SOFT CONSTRAINTS:
 *   4. Register numbers sorted ascending within each column
 *   5. ROW ROTATION: Students shift rows across exam sessions
 *
 * SPREAD MODE (for remainder halls with ≤ ceil(capacity/2) students):
 *   Fill Column I, Skip II, Fill III, Skip IV, Fill V, Skip VI
 */
public class InternalConstraintSolver {

    private static final Logger logger = LoggerFactory.getLogger(InternalConstraintSolver.class);

    public record SolverResult(List<SeatAssignment> assignments, List<AllocationViolation> violations) {}

    public SolverResult allocateHall(String hallId, List<Student> students, String[] injectedPattern,
                                     Map<String, Set<String>> positionHistory, int seasonSessionIndex,
                                     int rows, int cols) {
        int maxCapacity = rows * cols;
        // Allow a small buffer for constraint overflow (original behavior: 40 max on 42 grid)
        int maxStudents = Math.max(maxCapacity - 2, maxCapacity);
        if (students.size() > maxStudents) {
            throw new IllegalArgumentException("Hall " + hallId + " capacity exceeded (" + maxStudents + " max for internal)");
        }
        List<String> colNames = AllocationConstraintSolver.generateColNames(cols);
        List<Integer> spreadCols = AllocationConstraintSolver.generateSpreadCols(cols);
        List<Integer> fullCols = AllocationConstraintSolver.generateFullCols(cols);

        List<SeatAssignment> assignments = new ArrayList<>();
        List<AllocationViolation> violations = new ArrayList<>();

        // 1. Organize students into COLUMN BLOCKS by DEPARTMENT & ELECTIVE SUBJECTS
        List<List<Student>> columnBlocks = buildColumnBlocks(students, injectedPattern, rows, cols);

        // EDGE CASE 1: Open Electives - Re-order column blocks to separate adjacent columns with same subject
        columnBlocks = reorderColumnBlocksForElectiveSeparation(columnBlocks, cols);

        // 2. Determine mode: SPREAD or FULL
        // EDGE CASE 3: Single-Department Hall Overflow — force Spread Mode (Spacer Columns) if only 1 dept
        Set<String> uniqueDeptsInHall = students.stream().map(Student::department).collect(Collectors.toSet());
        boolean isSingleDeptHall = uniqueDeptsInHall.size() == 1 && columnBlocks.size() > 1;

        int spreadThreshold = (int) Math.ceil((double) maxCapacity / 2.0);
        boolean spreadMode = isSingleDeptHall || (students.size() <= spreadThreshold && columnBlocks.size() <= spreadCols.size());
        List<Integer> physicalCols = spreadMode ? spreadCols : fullCols;

        // 3. Compute row rotation offset (DISABLED FOR INTERNAL EXAMS)
        int rowOffset = 0;

        logger.info("Hall {}: {} students, {} blocks, Grid: {}×{}, Mode: {}, SingleDept: {}, Pattern: {}",
                hallId, students.size(), columnBlocks.size(), rows, cols,
                spreadMode ? "SPREAD" : "FULL", isSingleDeptHall,
                injectedPattern != null ? Arrays.toString(injectedPattern) : "auto");

        Student[][] grid = new Student[rows + 1][cols]; // grid[row 1-rows][col 0-(cols-1)]

        // 4. Fill each column block into its physical column
        for (int blockIdx = 0; blockIdx < columnBlocks.size() && blockIdx < physicalCols.size(); blockIdx++) {
            int c = physicalCols.get(blockIdx);
            List<Student> columnStudents = new ArrayList<>(columnBlocks.get(blockIdx));

            // EDGE CASE 2: Professional Electives — Interleave subjects vertically inside the column
            columnStudents = sortColumnStudents(columnStudents);

            for (int r = 0; r < columnStudents.size() && r < rows; r++) {
                int row = r + 1;
                Student candidate = columnStudents.get(r);

                // HORIZONTAL ADJACENCY CHECK (subject code & department)
                boolean hasConflict = false;
                if (c > 0 && grid[row][c - 1] != null && (sameSubject(candidate, grid[row][c - 1]) || (isSingleDeptHall && sameDept(candidate, grid[row][c - 1])))) {
                    hasConflict = true;
                }
                if (c < cols - 1 && grid[row][c + 1] != null && (sameSubject(candidate, grid[row][c + 1]) || (isSingleDeptHall && sameDept(candidate, grid[row][c + 1])))) {
                    hasConflict = true;
                }

                if (hasConflict) {
                    Student swapped = findSafeSwap(columnStudents, r, grid, row, c, cols);
                    if (swapped != null) {
                        candidate = swapped;
                    } else {
                        violations.add(new AllocationViolation(ViolationType.SUBJECT_ADJACENCY_VIOLATION,
                                "Horizontal adjacency at Hall " + hallId + " R" + row + "C" + colNames.get(c)
                                + " [" + candidate.subjectCode() + "]"));
                    }
                }

                grid[row][c] = candidate;
            }
        }

        // EDGE CASE 4: 2D Multi-Pass Swap Optimization to eliminate lingering subject/dept adjacencies
        optimize2DGridAdjacency(grid, rows, cols);

        // 5. POST-ALLOCATION AUDIT
        int adjacencyViolations = auditHorizontalAdjacency(grid, hallId, rows, cols, colNames);
        if (adjacencyViolations > 0) {
            logger.warn("Hall {}: AUDIT — {} subject adjacency warnings", hallId, adjacencyViolations);
        } else {
            logger.info("Hall {}: AUDIT PASSED — Zero adjacency violations", hallId);
        }

        // 6. BUILD FINAL ASSIGNMENTS with ROW ROTATION
        for (int c = 0; c < cols; c++) {
            for (int logicalRow = 1; logicalRow <= rows; logicalRow++) {
                if (grid[logicalRow][c] != null) {
                    Student s = grid[logicalRow][c];
                    int physicalRow = ((logicalRow - 1 + rowOffset) % rows) + 1;
                    assignments.add(new SeatAssignment(
                            s.registerNumber(), hallId, physicalRow, colNames.get(c),
                            s.subjectCode(), s.department(),
                            s.semester(), s.regulation(),
                            calculateRisk(grid, logicalRow, c, rows, cols)
                    ));
                }
            }
        }

        if (rowOffset > 0) {
            logger.info("Hall {}: ROW ROTATION applied — offset {} (session {}). " +
                    "Row 1 students now at physical row {}.",
                    hallId, rowOffset, seasonSessionIndex, ((0 + rowOffset) % rows) + 1);
        }

        return new SolverResult(assignments, violations);
    }

    /** Backward-compatible overload: defaults to 7×6 grid */
    public SolverResult allocateHall(String hallId, List<Student> students, String[] injectedPattern,
                                     Map<String, Set<String>> positionHistory, int seasonSessionIndex) {
        return allocateHall(hallId, students, injectedPattern, positionHistory, seasonSessionIndex, 7, 6);
    }

    // ===================== RISK CALCULATOR =====================

    private int calculateRisk(Student[][] grid, int row, int col, int rows, int cols) {
        int risk = 0;
        if (col > 0 && grid[row][col - 1] != null && sameSubject(grid[row][col], grid[row][col - 1])) {
            risk += 30;
        }
        if (col < cols - 1 && grid[row][col + 1] != null && sameSubject(grid[row][col], grid[row][col + 1])) {
            risk += 30;
        }
        return risk;
    }

    // ===================== SUBJECT HELPERS =====================

    private boolean sameSubject(Student a, Student b) {
        if (a == null || b == null) return false;
        if (a.subjectCode() == null || b.subjectCode() == null) return false;
        String sa = a.subjectCode().trim();
        String sb = b.subjectCode().trim();
        if (sa.isEmpty() || sa.equalsIgnoreCase("N/A") || sa.equals("-")) return false;
        return sa.equalsIgnoreCase(sb);
    }

    private boolean sameDept(Student a, Student b) {
        if (a == null || b == null) return false;
        if (a.department() == null || b.department() == null) return false;
        return a.department().equalsIgnoreCase(b.department());
    }

    private List<List<Student>> reorderColumnBlocksForElectiveSeparation(List<List<Student>> blocks, int cols) {
        if (blocks.size() <= 1) return blocks;

        // Group dominant subject for each block
        List<List<Student>> reordered = new ArrayList<>();
        List<List<Student>> remaining = new ArrayList<>(blocks);

        reordered.add(remaining.remove(0));

        while (!remaining.isEmpty()) {
            List<Student> lastPlaced = reordered.get(reordered.size() - 1);
            String lastSub = getDominantSubject(lastPlaced);

            int bestCandidateIdx = -1;
            for (int i = 0; i < remaining.size(); i++) {
                String candidateSub = getDominantSubject(remaining.get(i));
                if (!candidateSub.equalsIgnoreCase(lastSub)) {
                    bestCandidateIdx = i;
                    break;
                }
            }

            if (bestCandidateIdx != -1) {
                reordered.add(remaining.remove(bestCandidateIdx));
            } else {
                reordered.add(remaining.remove(0));
            }
        }
        return reordered;
    }

    private String getDominantSubject(List<Student> block) {
        if (block == null || block.isEmpty()) return "";
        Map<String, Long> freq = block.stream()
                .filter(s -> s.subjectCode() != null)
                .collect(Collectors.groupingBy(Student::subjectCode, Collectors.counting()));
        return freq.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("");
    }

    private void optimize2DGridAdjacency(Student[][] grid, int rows, int cols) {
        for (int pass = 0; pass < 3; pass++) {
            boolean swappedInPass = false;
            for (int r = 1; r <= rows; r++) {
                for (int c = 0; c < cols - 1; c++) {
                    if (grid[r][c] != null && grid[r][c + 1] != null && sameSubject(grid[r][c], grid[r][c + 1])) {
                        // Try to swap grid[r][c] with another row in same column c
                        for (int targetRow = 1; targetRow <= rows; targetRow++) {
                            if (targetRow == r || grid[targetRow][c] == null) continue;

                            Student candidate = grid[targetRow][c];
                            boolean leftOk = (c == 0 || !sameSubject(candidate, grid[r][c - 1]));
                            boolean rightOk = (!sameSubject(candidate, grid[r][c + 1]));

                            if (leftOk && rightOk) {
                                Student temp = grid[r][c];
                                grid[r][c] = grid[targetRow][c];
                                grid[targetRow][c] = temp;
                                swappedInPass = true;
                                break;
                            }
                        }
                    }
                }
            }
            if (!swappedInPass) break;
        }
    }

    private int auditHorizontalAdjacency(Student[][] grid, String hallId, int rows, int cols, List<String> colNames) {
        int violations = 0;
        for (int r = 1; r <= rows; r++) {
            for (int c = 0; c < cols - 1; c++) {
                if (grid[r][c] != null && grid[r][c + 1] != null) {
                    if (sameSubject(grid[r][c], grid[r][c + 1])) {
                        violations++;
                        logger.warn("AUDIT: Hall {} R{}C{}-C{} same subject [{}]",
                                hallId, r, colNames.get(c), colNames.get(c + 1), grid[r][c].subjectCode());
                    }
                }
            }
        }
        return violations;
    }

    // ===================== COLUMN BUILDING =====================

    private List<List<Student>> buildColumnBlocks(List<Student> students, String[] pattern, int rows, int cols) {
        List<List<Student>> blocks = new ArrayList<>();

        if (pattern != null && pattern.length > 0) {
            int offset = 0;
            for (int i = 0; i < pattern.length && offset < students.size(); i++) {
                String targetDept = pattern[i];
                List<Student> block = new ArrayList<>();
                int maxRowsForCol = rows;

                while (block.size() < maxRowsForCol && offset < students.size()) {
                    Student s = students.get(offset);
                    if (s.department().equals(targetDept)) {
                        block.add(s);
                        offset++;
                    } else {
                        break;
                    }
                }

                if (block.isEmpty() && offset < students.size()) {
                    while (block.size() < maxRowsForCol && offset < students.size()) {
                        block.add(students.get(offset));
                        offset++;
                    }
                }

                if (!block.isEmpty()) blocks.add(block);
            }

            while (offset < students.size()) {
                List<Student> block = new ArrayList<>();
                int maxRowsForCol = rows;
                while (block.size() < maxRowsForCol && offset < students.size()) {
                    block.add(students.get(offset));
                    offset++;
                }
                if (!block.isEmpty()) blocks.add(block);
            }
        } else {
            for (int i = 0; i < students.size(); ) {
                int maxRowsForCol = rows;
                int end = Math.min(i + maxRowsForCol, students.size());
                blocks.add(new ArrayList<>(students.subList(i, end)));
                i = end;
            }
        }

        // PRODUCTION SAFETY: Fold overflow blocks
        if (blocks.size() > cols) {
            logger.warn("buildColumnBlocks produced {} blocks (>{} cols). Folding overflow.", blocks.size(), cols);
            List<Student> lastBlock = blocks.get(cols - 1);
            for (int i = cols; i < blocks.size(); i++) {
                lastBlock.addAll(blocks.get(i));
            }
            blocks = new ArrayList<>(blocks.subList(0, cols));
        }

        return blocks;
    }

    // ===================== SORTING =====================

    private List<Student> sortColumnStudents(List<Student> column) {
        if (column.size() <= 1) return column;

        Map<String, List<Student>> bySubject = column.stream()
                .collect(Collectors.groupingBy(
                    s -> s.subjectCode() != null ? s.subjectCode() : "N/A",
                    LinkedHashMap::new, Collectors.toList()));

        for (List<Student> group : bySubject.values()) {
            group.sort(Comparator.comparing(Student::registerNumber));
        }

        if (bySubject.size() <= 1) {
            List<Student> sorted = new ArrayList<>(column);
            sorted.sort(Comparator.comparing(Student::registerNumber));
            return sorted;
        }

        List<Student> result = new ArrayList<>();
        List<Iterator<Student>> iterators = bySubject.values().stream()
                .sorted((a, b) -> Integer.compare(b.size(), a.size()))
                .map(List::iterator)
                .collect(Collectors.toList());

        while (result.size() < column.size()) {
            for (Iterator<Student> it : iterators) {
                if (it.hasNext() && result.size() < column.size()) {
                    result.add(it.next());
                }
            }
        }
        return result;
    }

    // ===================== SWAP HELPERS =====================

    private Student findSafeSwap(List<Student> columnStudents, int currentIdx,
                                  Student[][] grid, int row, int col, int cols) {
        Set<String> forbidden = new HashSet<>();
        if (col > 0 && grid[row][col - 1] != null && grid[row][col - 1].subjectCode() != null) {
            forbidden.add(grid[row][col - 1].subjectCode().trim().toUpperCase());
        }
        if (col < cols - 1 && grid[row][col + 1] != null && grid[row][col + 1].subjectCode() != null) {
            forbidden.add(grid[row][col + 1].subjectCode().trim().toUpperCase());
        }

        if (forbidden.isEmpty()) return null;

        for (int i = currentIdx + 1; i < columnStudents.size(); i++) {
            String candidateSub = columnStudents.get(i).subjectCode();
            String normalized = (candidateSub != null) ? candidateSub.trim().toUpperCase() : "";
            if (!forbidden.contains(normalized)) {
                Student safe = columnStudents.get(i);
                columnStudents.set(i, columnStudents.get(currentIdx));
                columnStudents.set(currentIdx, safe);
                return safe;
            }
        }
        return null;
    }
}

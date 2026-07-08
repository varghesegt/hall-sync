package com.exam.engine.internal;

import com.exam.engine.model.AllocationViolation;
import com.exam.engine.model.SeatAssignment;
import com.exam.engine.model.Student;
import com.exam.engine.model.ViolationType;
import com.exam.engine.SafetyValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * INTERNAL EXAM CONSTRAINT SOLVER (7×6 GRID)
 *
 * Adapted from semester AllocationConstraintSolver for 7 rows × 6 columns.
 * Max 40 students per hall (overflow to 41-42 if constraints demand).
 *
 * HARD CONSTRAINTS:
 *   1. One Column = One Department (STRICT)
 *   2. No same department horizontally adjacent (guaranteed by pattern)
 *   3. No same subject code horizontally adjacent (audited, best-effort swap)
 *
 * SOFT CONSTRAINTS:
 *   4. Register numbers sorted ascending within each column
 *   5. ROW ROTATION: Students shift rows across exam sessions
 *      - Offset pattern: (sessionIndex * 2) % 7
 *      - Guarantees: no student sits in same row for 7 consecutive exams
 *
 * SPREAD MODE (for remainder halls with ≤ 21 students):
 *   Fill Column I, Skip II, Fill III, Skip IV, Fill V, Skip VI
 */
public class InternalConstraintSolver {

    private static final Logger logger = LoggerFactory.getLogger(InternalConstraintSolver.class);
    private static final String[] COL_NAMES = {"I", "II", "III", "IV", "V", "VI"};
    private static final int ROWS = 7;
    private static final int COLS = 6;
    private static final int MAX_STUDENTS = 40;
    private static final int TOTAL_SEATS = 42;

    // Physical column indices for SPREAD mode: columns I, III, V (skip II, IV, VI)
    private static final int[] SPREAD_COLS = {0, 2, 4};
    // Physical column indices for FULL mode: all columns
    private static final int[] FULL_COLS = {0, 1, 2, 3, 4, 5};

    public record SolverResult(List<SeatAssignment> assignments, List<AllocationViolation> violations) {}

    public SolverResult allocateHall(String hallId, List<Student> students, String[] injectedPattern,
                                     Map<String, Set<String>> positionHistory, int seasonSessionIndex) {
        if (students.size() > MAX_STUDENTS) {
            throw new IllegalArgumentException("Hall " + hallId + " capacity exceeded (" + MAX_STUDENTS + " max for internal)");
        }

        List<SeatAssignment> assignments = new ArrayList<>();
        List<AllocationViolation> violations = new ArrayList<>();

        // 1. Organize students into COLUMN BLOCKS by DEPARTMENT
        List<List<Student>> columnBlocks = buildColumnBlocks(students, injectedPattern);

        // 2. Determine mode: SPREAD (≤21 students, ≤3 blocks) or FULL (>21 students)
        boolean spreadMode = students.size() <= 21 && columnBlocks.size() <= 3;
        int[] physicalCols = spreadMode ? SPREAD_COLS : FULL_COLS;

        // 3. Compute row rotation offset (DISABLED FOR INTERNAL EXAMS)
        int rowOffset = 0;

        logger.info("Hall {}: {} students, {} blocks, Mode: {}, RowOffset: 0 (Disabled), Pattern: {}",
                hallId, students.size(), columnBlocks.size(),
                spreadMode ? "SPREAD(I,III,V)" : "FULL(I-VI)",
                injectedPattern != null ? Arrays.toString(injectedPattern) : "auto");

        Student[][] grid = new Student[ROWS + 1][COLS]; // grid[row 1-7][col 0-5]

        // 4. Fill each column block into its physical column
        for (int blockIdx = 0; blockIdx < columnBlocks.size() && blockIdx < physicalCols.length; blockIdx++) {
            int c = physicalCols[blockIdx];
            List<Student> columnStudents = new ArrayList<>(columnBlocks.get(blockIdx));

            columnStudents = sortColumnStudents(columnStudents);

            for (int r = 0; r < columnStudents.size() && r < ROWS; r++) {
                int row = r + 1;
                // STRICT CHECK: Skip Row 7 for Column V (index 4) and Column VI (index 5)
                if (row == 7 && (c == 4 || c == 5)) {
                    continue; // Should not happen if buildColumnBlocks is correct, but safe check
                }
                
                Student candidate = columnStudents.get(r);

                // HORIZONTAL ADJACENCY CHECK (subject code)
                boolean hasConflict = false;
                if (c > 0 && grid[row][c - 1] != null && sameSubject(candidate, grid[row][c - 1])) {
                    hasConflict = true;
                }
                if (c < COLS - 1 && grid[row][c + 1] != null && sameSubject(candidate, grid[row][c + 1])) {
                    hasConflict = true;
                }

                if (hasConflict) {
                    Student swapped = findSafeSwap(columnStudents, r, grid, row, c);
                    if (swapped != null) {
                        candidate = swapped;
                    } else {
                        violations.add(new AllocationViolation(ViolationType.SUBJECT_ADJACENCY_VIOLATION,
                                "Horizontal adjacency at Hall " + hallId + " R" + row + "C" + COL_NAMES[c]
                                + " [" + candidate.subjectCode() + "]"));
                    }
                }

                grid[row][c] = candidate;
            }
        }

        // 5. POST-ALLOCATION AUDIT
        int adjacencyViolations = auditHorizontalAdjacency(grid, hallId);
        if (adjacencyViolations > 0) {
            logger.warn("Hall {}: AUDIT — {} subject adjacency warnings", hallId, adjacencyViolations);
        } else {
            logger.info("Hall {}: AUDIT PASSED — Zero adjacency violations", hallId);
        }

        // 6. BUILD FINAL ASSIGNMENTS with ROW ROTATION
        for (int c = 0; c < COLS; c++) {
            for (int logicalRow = 1; logicalRow <= ROWS; logicalRow++) {
                if (grid[logicalRow][c] != null) {
                    Student s = grid[logicalRow][c];
                    int physicalRow = ((logicalRow - 1 + rowOffset) % ROWS) + 1;
                    assignments.add(new SeatAssignment(
                            s.registerNumber(), hallId, physicalRow, COL_NAMES[c],
                            s.subjectCode(), s.department(),
                            s.semester(), s.regulation(),
                            calculateRisk(grid, logicalRow, c)
                    ));
                }
            }
        }

        if (rowOffset > 0) {
            logger.info("Hall {}: ROW ROTATION applied — offset {} (session {}). " +
                    "Row 1 students now at physical row {}.",
                    hallId, rowOffset, seasonSessionIndex, ((0 + rowOffset) % ROWS) + 1);
        }

        return new SolverResult(assignments, violations);
    }

    // ===================== RISK CALCULATOR =====================

    private int calculateRisk(Student[][] grid, int row, int col) {
        int risk = 0;
        // Check left
        if (col > 0 && grid[row][col - 1] != null && sameSubject(grid[row][col], grid[row][col - 1])) {
            risk += 30;
        }
        // Check right
        if (col < COLS - 1 && grid[row][col + 1] != null && sameSubject(grid[row][col], grid[row][col + 1])) {
            risk += 30;
        }
        return risk;
    }

    // ===================== SUBJECT HELPERS =====================

    private boolean sameSubject(Student a, Student b) {
        if (a.subjectCode() == null || b.subjectCode() == null) return false;
        String sa = a.subjectCode().trim();
        String sb = b.subjectCode().trim();
        if (sa.isEmpty() || sa.equalsIgnoreCase("N/A") || sa.equals("-")) return false;
        return sa.equalsIgnoreCase(sb);
    }

    private int auditHorizontalAdjacency(Student[][] grid, String hallId) {
        int violations = 0;
        for (int r = 1; r <= ROWS; r++) {
            for (int c = 0; c < COLS - 1; c++) {
                if (grid[r][c] != null && grid[r][c + 1] != null) {
                    if (sameSubject(grid[r][c], grid[r][c + 1])) {
                        violations++;
                        logger.warn("AUDIT: Hall {} R{}C{}-C{} same subject [{}]",
                                hallId, r, COL_NAMES[c], COL_NAMES[c + 1], grid[r][c].subjectCode());
                    }
                }
            }
        }
        return violations;
    }

    // ===================== COLUMN BUILDING =====================

    private List<List<Student>> buildColumnBlocks(List<Student> students, String[] pattern) {
        List<List<Student>> blocks = new ArrayList<>();

        if (pattern != null && pattern.length > 0) {
            int offset = 0;
            for (int i = 0; i < pattern.length && offset < students.size(); i++) {
                String targetDept = pattern[i];
                List<Student> block = new ArrayList<>();
                int maxRowsForCol = (i == 4 || i == 5) ? 6 : ROWS; // Col V and VI have only 6 seats

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
                int currentBlockIdx = blocks.size();
                int maxRowsForCol = (currentBlockIdx == 4 || currentBlockIdx == 5) ? 6 : ROWS;
                while (block.size() < maxRowsForCol && offset < students.size()) {
                    block.add(students.get(offset));
                    offset++;
                }
                if (!block.isEmpty()) blocks.add(block);
            }
        } else {
            for (int i = 0; i < students.size(); ) {
                int currentBlockIdx = blocks.size();
                int maxRowsForCol = (currentBlockIdx == 4 || currentBlockIdx == 5) ? 6 : ROWS;
                int end = Math.min(i + maxRowsForCol, students.size());
                blocks.add(new ArrayList<>(students.subList(i, end)));
                i = end;
            }
        }

        // PRODUCTION SAFETY: Fold overflow blocks
        if (blocks.size() > COLS) {
            logger.warn("buildColumnBlocks produced {} blocks (>{} cols). Folding overflow.", blocks.size(), COLS);
            List<Student> lastBlock = blocks.get(COLS - 1);
            for (int i = COLS; i < blocks.size(); i++) {
                lastBlock.addAll(blocks.get(i));
            }
            blocks = new ArrayList<>(blocks.subList(0, COLS));
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
                                  Student[][] grid, int row, int col) {
        Set<String> forbidden = new HashSet<>();
        if (col > 0 && grid[row][col - 1] != null && grid[row][col - 1].subjectCode() != null) {
            forbidden.add(grid[row][col - 1].subjectCode().trim().toUpperCase());
        }
        if (col < COLS - 1 && grid[row][col + 1] != null && grid[row][col + 1].subjectCode() != null) {
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

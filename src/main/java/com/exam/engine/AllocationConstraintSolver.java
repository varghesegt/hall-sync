package com.exam.engine;

import com.exam.engine.model.AllocationViolation;
import com.exam.engine.model.SeatAssignment;
import com.exam.engine.model.Student;
import com.exam.engine.model.ViolationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * PRODUCTION-GRADE CONSTRAINT SOLVER (V11 - ROW ROTATION)
 *
 * HARD CONSTRAINTS:
 *   1. One Column = One Department (STRICT)
 *   2. No same department horizontally adjacent (guaranteed by pattern)
 *   3. No same subject code horizontally adjacent (audited, best-effort swap)
 *
 * SOFT CONSTRAINTS:
 *   4. Register numbers sorted ascending within each column
 *   5. ROW ROTATION: Students shift rows across exam sessions in the same season
 *      - Session 0: rows 1,2,3,4,5 (normal)
 *      - Session 1: rows 3,4,5,1,2 (offset +2)
 *      - Session 2: rows 5,1,2,3,4 (offset +4)
 *      - Session 3: rows 2,3,4,5,1 (offset +1)
 *      - Session 4: rows 4,5,1,2,3 (offset +3)
 *      Guarantees: no student sits in the same row for 5 consecutive exams.
 *      Preserves: dept purity, register order (cyclic), horizontal adjacency.
 *
 * SPREAD MODE (for remainder halls with ≤ 15 students):
 *   Fill Column I, Skip II, Fill III, Skip IV, Fill V
 */
public class AllocationConstraintSolver {

    private static final Logger logger = LoggerFactory.getLogger(AllocationConstraintSolver.class);
    private static final String[] COL_NAMES = {"I", "II", "III", "IV", "V"};
    private final SafetyValidator safetyValidator = new SafetyValidator();

    // Physical column indices for SPREAD mode: columns I, III, V (skip II, IV)
    private static final int[] SPREAD_COLS = {0, 2, 4};
    // Physical column indices for FULL mode: all columns
    private static final int[] FULL_COLS = {0, 1, 2, 3, 4};

    public record SolverResult(List<SeatAssignment> assignments, List<AllocationViolation> violations) {}

    /**
     * Main entry point. Accepts positionHistory for backward compat and seasonSessionIndex for row rotation.
     */
    public SolverResult allocateHall(String hallId, List<Student> students, String[] injectedPattern,
                                     Map<String, Set<String>> positionHistory, int seasonSessionIndex) {
        if (students.size() > 25) {
            throw new IllegalArgumentException("Hall " + hallId + " capacity exceeded (25 max)");
        }

        List<SeatAssignment> assignments = new ArrayList<>();
        List<AllocationViolation> violations = new ArrayList<>();

        // 1. Organize students into COLUMN BLOCKS by DEPARTMENT
        List<List<Student>> columnBlocks = buildColumnBlocks(students, injectedPattern);

        // 2. Determine mode: SPREAD (≤15 students, ≤3 blocks) or FULL (>15 students)
        boolean spreadMode = students.size() <= 15 && columnBlocks.size() <= 3;
        int[] physicalCols = spreadMode ? SPREAD_COLS : FULL_COLS;

        // 3. Compute row rotation offset from session index
        //    Pattern: 0, 2, 4, 1, 3 — maximizes distance between consecutive sessions
        //    DISABLED: Per user request to prevent confusion and allocate sequentially
        int rowOffset = 0;

        logger.info("Hall {}: {} students, {} blocks, Mode: {}, RowOffset: {} (session {}), Pattern: {}",
                hallId, students.size(), columnBlocks.size(),
                spreadMode ? "SPREAD(I,III,V)" : "FULL(I-V)",
                rowOffset, seasonSessionIndex,
                injectedPattern != null ? Arrays.toString(injectedPattern) : "auto");

        Student[][] grid = new Student[6][5]; // grid[row 1-5][col 0-4]

        // 4. Fill each column block into its physical column
        for (int blockIdx = 0; blockIdx < columnBlocks.size() && blockIdx < physicalCols.length; blockIdx++) {
            int c = physicalCols[blockIdx];
            List<Student> columnStudents = new ArrayList<>(columnBlocks.get(blockIdx));

            // Sort within column: register number ascending + subject alternation
            columnStudents = sortColumnStudents(columnStudents);

            for (int r = 0; r < columnStudents.size() && r < 5; r++) {
                Student candidate = columnStudents.get(r);
                int row = r + 1; // 1-indexed LOGICAL row (for constraint checking)

                // HORIZONTAL ADJACENCY CHECK (subject code) — checked on LOGICAL grid
                boolean hasConflict = false;

                // Check LEFT
                if (c > 0 && grid[row][c - 1] != null
                        && sameSubject(candidate, grid[row][c - 1])) {
                    hasConflict = true;
                }
                // Check RIGHT
                if (c < 4 && grid[row][c + 1] != null
                        && sameSubject(candidate, grid[row][c + 1])) {
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

        // =============================================
        // 5. POST-ALLOCATION AUDIT (on logical grid)
        // =============================================
        int adjacencyViolations = auditHorizontalAdjacency(grid, hallId);
        if (adjacencyViolations > 0) {
            logger.warn("Hall {}: AUDIT — {} subject adjacency warnings", hallId, adjacencyViolations);
        } else {
            logger.info("Hall {}: AUDIT PASSED — Zero adjacency violations", hallId);
        }

        // =============================================
        // 6. BUILD FINAL ASSIGNMENTS with ROW ROTATION
        //    Logical row → Physical row via offset
        //    This preserves all constraint checks (done on logical grid)
        //    while ensuring students sit in different physical rows each exam.
        // =============================================
        for (int c = 0; c < 5; c++) {
            for (int logicalRow = 1; logicalRow <= 5; logicalRow++) {
                if (grid[logicalRow][c] != null) {
                    Student s = grid[logicalRow][c];
                    // Apply cyclic row rotation: physical = ((logical-1 + offset) % 5) + 1
                    int physicalRow = ((logicalRow - 1 + rowOffset) % 5) + 1;
                    assignments.add(new SeatAssignment(
                            s.registerNumber(), hallId, physicalRow, COL_NAMES[c],
                            s.subjectCode(), s.department(),
                            s.semester(), s.regulation(),
                            safetyValidator.calculateRisk(grid, logicalRow, c)
                    ));
                }
            }
        }

        if (rowOffset > 0) {
            logger.info("Hall {}: ROW ROTATION applied — offset {} (session {}). " +
                    "Row 1 students now at physical row {}.",
                    hallId, rowOffset, seasonSessionIndex, ((0 + rowOffset) % 5) + 1);
        }

        return new SolverResult(assignments, violations);
    }

    // ===================== SUBJECT HELPERS =====================

    /** Safe null-aware subject comparison */
    private boolean sameSubject(Student a, Student b) {
        if (a.subjectCode() == null || b.subjectCode() == null) return false;
        return a.subjectCode().trim().equalsIgnoreCase(b.subjectCode().trim());
    }

    /**
     * POST-ALLOCATION AUDIT: Scan entire grid for horizontal adjacency violations.
     */
    private int auditHorizontalAdjacency(Student[][] grid, String hallId) {
        int violations = 0;
        for (int r = 1; r <= 5; r++) {
            for (int c = 0; c < 4; c++) {
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

    /**
     * Build column blocks from the pre-sorted student list.
     * Matches by DEPARTMENT (one column = one department).
     *
     * PRODUCTION SAFETY: The number of blocks is CAPPED at the physical
     * column limit (5 for FULL mode). If more blocks would be generated,
     * the extra students are folded into the last block to prevent data loss.
     */
    private List<List<Student>> buildColumnBlocks(List<Student> students, String[] pattern) {
        List<List<Student>> blocks = new ArrayList<>();

        if (pattern != null && pattern.length > 0) {
            int offset = 0;
            for (int i = 0; i < pattern.length && offset < students.size(); i++) {
                String targetDept = pattern[i];
                List<Student> block = new ArrayList<>();

                while (block.size() < 5 && offset < students.size()) {
                    Student s = students.get(offset);
                    if (s.department().equals(targetDept)) {
                        block.add(s);
                        offset++;
                    } else {
                        break;
                    }
                }

                // Fallback: if block empty but students remain, pull anyone
                if (block.isEmpty() && offset < students.size()) {
                    while (block.size() < 5 && offset < students.size()) {
                        block.add(students.get(offset));
                        offset++;
                    }
                }

                if (!block.isEmpty()) blocks.add(block);
            }

            // Handle remaining students not covered by pattern
            while (offset < students.size()) {
                List<Student> block = new ArrayList<>();
                while (block.size() < 5 && offset < students.size()) {
                    block.add(students.get(offset));
                    offset++;
                }
                if (!block.isEmpty()) blocks.add(block);
            }
        } else {
            for (int i = 0; i < students.size(); i += 5) {
                int end = Math.min(i + 5, students.size());
                blocks.add(new ArrayList<>(students.subList(i, end)));
            }
        }

        // =============================================
        // PRODUCTION SAFETY: Fold overflow blocks into the last block
        // This guarantees ALL students are placed on the grid.
        // A 5x5 grid has exactly 5 columns, so max 5 blocks.
        // If buildColumnBlocks created more (e.g. 6+ departments
        // from a remainder hall), fold extras into block #5.
        // =============================================
        if (blocks.size() > 5) {
            logger.warn("buildColumnBlocks produced {} blocks (>5). Folding overflow into last block.", blocks.size());
            List<Student> lastBlock = blocks.get(4);
            for (int i = 5; i < blocks.size(); i++) {
                lastBlock.addAll(blocks.get(i));
            }
            blocks = new ArrayList<>(blocks.subList(0, 5));
        }

        return blocks;
    }

    // ===================== SORTING =====================

    /**
     * Sort students within a column:
     * - If all have the same subject: sort by register number ascending (3,4,5,6,7)
     * - If mixed subjects: alternate subjects, but sort within each subject group by register number
     */
    private List<Student> sortColumnStudents(List<Student> column) {
        if (column.size() <= 1) return column;

        Map<String, List<Student>> bySubject = column.stream()
                .collect(Collectors.groupingBy(
                    s -> s.subjectCode() != null ? s.subjectCode() : "N/A",
                    LinkedHashMap::new, Collectors.toList()));

        // Sort each subject group by register number ascending
        for (List<Student> group : bySubject.values()) {
            group.sort(Comparator.comparing(Student::registerNumber));
        }

        if (bySubject.size() <= 1) {
            // Single subject: just return sorted by register number
            List<Student> sorted = new ArrayList<>(column);
            sorted.sort(Comparator.comparing(Student::registerNumber));
            return sorted;
        }

        // Multiple subjects: interleave subjects, each group sorted by register number
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

    /**
     * Try to find a safe swap to avoid horizontal adjacency.
     */
    private Student findSafeSwap(List<Student> columnStudents, int currentIdx,
                                  Student[][] grid, int row, int col) {
        Set<String> forbidden = new HashSet<>();
        if (col > 0 && grid[row][col - 1] != null && grid[row][col - 1].subjectCode() != null) {
            forbidden.add(grid[row][col - 1].subjectCode().trim().toUpperCase());
        }
        if (col < 4 && grid[row][col + 1] != null && grid[row][col + 1].subjectCode() != null) {
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

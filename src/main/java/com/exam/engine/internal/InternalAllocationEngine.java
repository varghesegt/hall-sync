package com.exam.engine.internal;

import com.exam.engine.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.stream.Collectors;

/**
 * INTERNAL EXAM ALLOCATION ENGINE (7×6 GRID)
 * 
 * Adapted from the semester AllocationEngine for internal examination seating.
 * Grid: 7 rows × 6 columns = 42 physical seats, MAX 40 students per hall.
 * If constraints cannot be satisfied, overflow to 41 or 42 is allowed.
 *
 * Same algorithm logic as semester engine:
 * - Global Optimizer distributes students across halls
 * - Constraint Solver places students in grid with dept/subject separation
 * - Reconciliation layer ensures zero data loss
 */
public class InternalAllocationEngine {

    private static final Logger logger = LoggerFactory.getLogger(InternalAllocationEngine.class);

    private final InternalGlobalOptimizer optimizer = new InternalGlobalOptimizer();
    private final InternalConstraintSolver solver = new InternalConstraintSolver();

    private static final String[] COL_NAMES = {"I", "II", "III", "IV", "V", "VI"};
    private static final int MAX_STUDENTS_PER_HALL = 40;
    private static final int TOTAL_SEATS = 42; // 7×6
    private static final int ROWS = 7;
    private static final int COLS = 6;

    public AllocationResult allocate(AllocationRequest request) {
        if (request.students() == null || request.students().isEmpty()) {
            return new AllocationResult(List.of(), List.of(), new EngineStats(0, 0, 0), Collections.emptyMap());
        }

        // 1. GLOBAL OPTIMIZATION
        InternalGlobalOptimizer.OptimizedAllocation optimized = optimizer.optimize(
                request.students(), request.halls(), request.seasonHistory());

        List<SeatAssignment> allAssignments = new ArrayList<>();
        List<AllocationViolation> allViolations = new ArrayList<>();
        
        int totalCapacity = 0;
        int utilizedHalls = 0;

        // 2. HALL EXECUTION
        for (Map.Entry<String, List<Student>> entry : optimized.hallAssignments().entrySet()) {
            String hallId = entry.getKey();
            List<Student> hallStudents = entry.getValue();
            String[] pattern = optimized.hallPatterns().get(hallId);

            if (hallStudents.isEmpty()) continue;

            // Retrieve the full hall object to get rows and cols
            Hall currentHall = request.halls().stream()
                .filter(h -> h.id().equals(hallId))
                .findFirst().orElse(new Hall(hallId, MAX_STUDENTS_PER_HALL, ROWS, COLS));

            InternalConstraintSolver.SolverResult solverResult = solver.allocateHall(
                    hallId, hallStudents, pattern, request.positionHistory(), request.seasonSessionIndex(),
                    currentHall.rows(), currentHall.cols());
            
            allAssignments.addAll(solverResult.assignments());
            allViolations.addAll(solverResult.violations());
            
            utilizedHalls++;
            totalCapacity += currentHall.capacity(); // Uses the full row*col capacity now

            String reason = optimized.reasoning().get(hallId);
            logger.debug("[INTERNAL-ENGINE] {}: {}", hallId, reason);
        }

        // =====================================================================
        // RECONCILIATION LAYER — ZERO-LOSS SAFETY NET (Dynamic Grid)
        // =====================================================================
        Set<String> assignedRegNos = allAssignments.stream()
                .map(SeatAssignment::registerNumber)
                .collect(Collectors.toSet());

        List<Student> unassigned = request.students().stream()
                .filter(s -> !assignedRegNos.contains(s.registerNumber()))
                .collect(Collectors.toList());

        if (!unassigned.isEmpty()) {
            logger.info("[INTERNAL-RECONCILIATION] {} students need placement. Filling empty seats in existing halls...", unassigned.size());

            Map<String, Set<String>> occupiedSeats = new LinkedHashMap<>();
            for (SeatAssignment a : allAssignments) {
                occupiedSeats.computeIfAbsent(a.hallId(), k -> new HashSet<>())
                        .add(a.row() + ":" + a.col());
            }

            int placedCount = 0;

            for (Student s : unassigned) {
                boolean placed = false;

                String bestHallId = null;
                int maxEmpty = 0;
                Hall bestHallObj = null;

                for (Map.Entry<String, Set<String>> e : occupiedSeats.entrySet()) {
                    Hall hallObj = request.halls().stream()
                        .filter(h -> h.id().equals(e.getKey()))
                        .findFirst().orElse(new Hall(e.getKey(), MAX_STUDENTS_PER_HALL, ROWS, COLS));
                        
                    int empty = hallObj.capacity() - e.getValue().size();
                    if (empty > maxEmpty) {
                        maxEmpty = empty;
                        bestHallId = e.getKey();
                        bestHallObj = hallObj;
                    }
                }

                if (bestHallId != null && maxEmpty > 0 && bestHallObj != null) {
                    Set<String> seats = occupiedSeats.get(bestHallId);
                    
                    // Generate column names dynamically based on the hall's configured columns
                    String[] ROMAN = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
                    String[] colNames = new String[bestHallObj.cols()];
                    for (int i = 0; i < bestHallObj.cols(); i++) {
                        colNames[i] = i < ROMAN.length ? ROMAN[i] : String.valueOf(i + 1);
                    }

                    for (int row = 1; row <= bestHallObj.rows() && !placed; row++) {
                        for (String col : colNames) {
                            String seatKey = row + ":" + col;
                            if (!seats.contains(seatKey)) {
                                allAssignments.add(new SeatAssignment(
                                        s.registerNumber(), bestHallId, row, col,
                                        s.subjectCode(), s.department(),
                                        s.semester(), s.regulation(), 0
                                ));
                                seats.add(seatKey);
                                placed = true;
                                placedCount++;
                                break;
                            }
                        }
                    }
                }

                if (!placed) {
                    logger.error("[INTERNAL-RECONCILIATION] FATAL: No empty seats for {}", s.registerNumber());
                }
            }

            int finalCount = allAssignments.size();
            int inputCount = request.students().size();
            logger.info("[INTERNAL-RECONCILIATION] Placed {} students into existing hall empty seats.", placedCount);
            if (finalCount == inputCount) {
                logger.info("[INTERNAL-RECONCILIATION] SUCCESS: All {} students assigned.", inputCount);
            } else {
                logger.warn("[INTERNAL-RECONCILIATION] ALERT: {}/{} assigned. Gap: {}", finalCount, inputCount, (inputCount - finalCount));
            }
        }

        EngineStats stats = new EngineStats(request.students().size(), totalCapacity, utilizedHalls);
        
        return new AllocationResult(allAssignments, allViolations, stats, optimized.reasoning());
    }
}

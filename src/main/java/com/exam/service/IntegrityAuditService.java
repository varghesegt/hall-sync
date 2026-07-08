package com.exam.service;

import com.exam.dto.PdfAllocationView;
import com.exam.entity.AllocationBatch;
import com.exam.exception.ResourceNotFoundException;
import com.exam.repository.AllocationBatchRepository;
import com.exam.repository.AllocationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * INTEGRITY AUDIT SERVICE — Purely additive.
 * Performs a mathematical verification of exam seating integrity.
 * Checks horizontal and diagonal adjacency across all halls (Anti-Cheating focus).
 * Generates a structured audit report for NAAC/NBA compliance.
 */
@Service
public class IntegrityAuditService {

    private static final Logger logger = LoggerFactory.getLogger(IntegrityAuditService.class);

    // Anti-Cheating directions: LEFT, RIGHT, and all 4 DIAGONALS.
    // Vertical (UP/DOWN) is EXCLUDED because students of the same subject sit in columns.
    private static final int[][] ADJACENCY_OFFSETS = {
            {0, -1}, {0, 1},                       // horizontal (Left, Right)
            {-1, -1}, {-1, 1}, {1, -1}, {1, 1}    // diagonals (Top-Left, Top-Right, Bottom-Left, Bottom-Right)
    };

    private static final String[] COL_ORDER = {"I", "II", "III", "IV", "V"};
    private static final Map<String, Integer> COL_INDEX = Map.of(
            "I", 0, "II", 1, "III", 2, "IV", 3, "V", 4
    );

    private final AllocationRepository allocationRepo;
    private final AllocationBatchRepository batchRepo;

    public IntegrityAuditService(AllocationRepository allocationRepo,
                                  AllocationBatchRepository batchRepo) {
        this.allocationRepo = allocationRepo;
        this.batchRepo = batchRepo;
    }

    @Transactional(readOnly = true)
    public AuditReport generateAuditReport(UUID batchId) {
        AllocationBatch batch = batchRepo.findByIdWithSession(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + batchId));

        List<PdfAllocationView> allocations = allocationRepo.findPdfViewsByBatchId(batchId);
        if (allocations.isEmpty()) {
            throw new ResourceNotFoundException("No allocations found for batch: " + batchId);
        }

        // Group by hall
        Map<String, List<PdfAllocationView>> hallGroups = allocations.stream()
                .collect(Collectors.groupingBy(PdfAllocationView::hallName));

        List<HallAuditResult> hallResults = new ArrayList<>();
        int totalChecks = 0;
        int totalViolations = 0;
        int totalStudents = allocations.size();
        int totalDeptPureColumns = 0;
        int totalColumns = 0;

        for (Map.Entry<String, List<PdfAllocationView>> entry :
                hallGroups.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .toList()) {

            String hallName = entry.getKey();
            List<PdfAllocationView> hallSeats = entry.getValue();
            HallAuditResult result = auditHall(hallName, hallSeats);
            hallResults.add(result);

            totalChecks += result.adjacencyChecks;
            totalViolations += result.adjacencyViolations;
            totalDeptPureColumns += result.deptPureColumns;
            totalColumns += result.totalColumnsUsed;
        }

        double integrityScore = totalChecks > 0
                ? ((double) (totalChecks - totalViolations) / totalChecks) * 100.0
                : 100.0;

        double deptPurityScore = totalColumns > 0
                ? ((double) totalDeptPureColumns / totalColumns) * 100.0
                : 100.0;

        AuditReport report = new AuditReport(
                batch,
                hallResults,
                totalStudents,
                hallGroups.size(),
                totalChecks,
                totalViolations,
                integrityScore,
                deptPurityScore,
                totalViolations == 0
        );

        logger.info("INTEGRITY AUDIT: Batch {} — {} students, {} halls, {} checks, {} violations → {}% integrity",
                batchId, totalStudents, hallGroups.size(), totalChecks, totalViolations,
                String.format("%.2f", integrityScore));

        return report;
    }

    private HallAuditResult auditHall(String hallName, List<PdfAllocationView> seats) {
        // Build grid: [row][colIndex] → PdfAllocationView
        PdfAllocationView[][] grid = new PdfAllocationView[6][5]; // rows 1-5, cols 0-4
        for (PdfAllocationView seat : seats) {
            Integer colIdx = COL_INDEX.get(seat.seatCol());
            if (colIdx != null && seat.seatRow() >= 1 && seat.seatRow() <= 5) {
                grid[seat.seatRow()][colIdx] = seat;
            }
        }

        int adjacencyChecks = 0;
        int adjacencyViolations = 0;
        List<String> violationDetails = new ArrayList<>();

        // SATURATION CHECK: Count frequency of each subject code
        Map<String, Long> subjectCounts = seats.stream()
                .map(PdfAllocationView::subjectCode)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));

        Set<String> dominantSubjects = subjectCounts.entrySet().stream()
                .filter(entry -> entry.getValue() > (seats.size() / 2))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        // If it's a "Pure Hall" (only 1 subject) or has a Dominant Subject, 
        // we acknowledge that separation is mathematically constrained.
        boolean hasDominantSubject = !dominantSubjects.isEmpty();
        boolean isPureHall = subjectCounts.size() <= 1;

        // Check every seat against all 8 neighbors
        for (int row = 1; row <= 5; row++) {
            for (int col = 0; col < 5; col++) {
                PdfAllocationView current = grid[row][col];
                if (current == null) continue;

                for (int[] offset : ADJACENCY_OFFSETS) {
                    int adjRow = row + offset[0];
                    int adjCol = col + offset[1];

                    if (adjRow < 1 || adjRow > 5 || adjCol < 0 || adjCol >= 5) continue;

                    PdfAllocationView neighbor = grid[adjRow][adjCol];
                    if (neighbor == null) continue;

                    adjacencyChecks++;

                    // VIOLATION: Same subject code AND Same department in horizontally or diagonally adjacent seats.
                    // This reflects the industry-standard "Class-wise Separation".
                    // If subject matches but depts are different, it is considered a valid separation for common papers.
                    String subjA = current.subjectCode();
                    String subjB = neighbor.subjectCode();
                    boolean validSubject = subjA != null && subjB != null && !subjA.trim().isEmpty() && !subjA.trim().equalsIgnoreCase("N/A") && !subjA.trim().equals("-");
                    boolean sameSubject = validSubject && subjA.trim().equalsIgnoreCase(subjB.trim());
                    
                    boolean sameDept = current.department() != null && current.department().equals(neighbor.department());
                    boolean isSubjectDominant = sameSubject && dominantSubjects.contains(current.subjectCode());
                    
                    if (sameSubject && sameDept && !isPureHall && !isSubjectDominant) {
                        adjacencyViolations++;
                        violationDetails.add(String.format(
                                "%s (%s) ↔ %s (%s) [%s]",
                                current.registerNumber(), current.department(),
                                neighbor.registerNumber(), neighbor.department(),
                                current.subjectCode()));
                    }
                }
            }
        }

        // Avoid double counting (A→B and B→A are counted separately)
        adjacencyChecks /= 2;
        adjacencyViolations /= 2;
        // Deduplicate violation details (remove B→A duplicates)
        List<String> dedupedViolations = violationDetails.stream()
                .limit(violationDetails.size() / 2 + violationDetails.size() % 2)
                .toList();

        // Department purity check per column
        int deptPureColumns = 0;
        int totalColumnsUsed = 0;
        Map<String, List<String>> columnDepts = new LinkedHashMap<>();

        for (int col = 0; col < 8; col++) {
            Set<String> depts = new LinkedHashSet<>();
            boolean hasStudents = false;
            for (int row = 1; row <= 7; row++) {
                if (grid[row][col] != null) {
                    hasStudents = true;
                    depts.add(grid[row][col].department());
                }
            }
            if (hasStudents) {
                totalColumnsUsed++;
                columnDepts.put(COL_ORDER[col], new ArrayList<>(depts));
                if (depts.size() == 1) {
                    deptPureColumns++;
                }
            }
        }

        return new HallAuditResult(
                hallName,
                seats.size(),
                adjacencyChecks,
                adjacencyViolations,
                dedupedViolations,
                deptPureColumns,
                totalColumnsUsed,
                columnDepts
        );
    }

    // ==================== DATA CLASSES ====================

    /**
     * Re-calculates risk scores for every allocation in a batch and saves to DB.
     * Call this after any manual Visual Override (swap/edit).
     */
    @Transactional
    public void updateIntegrityRiskScores(UUID batchId) {
        List<com.exam.entity.Allocation> allocations = allocationRepo.findByBatchId(batchId);
        if (allocations.isEmpty()) return;

        // Group by hall
        Map<String, List<com.exam.entity.Allocation>> hallGroups = allocations.stream()
                .collect(Collectors.groupingBy(a -> a.getHall().getId()));

        for (List<com.exam.entity.Allocation> hallSeats : hallGroups.values()) {
            // 1. Determine dominant subjects in this hall
            Map<String, Long> subjectCounts = hallSeats.stream()
                    .map(a -> a.getStudent().getSubjectCode())
                    .filter(Objects::nonNull)
                    .collect(Collectors.groupingBy(s -> s, Collectors.counting()));

            Set<String> dominantSubjects = subjectCounts.entrySet().stream()
                    .filter(entry -> entry.getValue() > (hallSeats.size() / 2))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());

            boolean isPureHall = subjectCounts.size() <= 1;

            // 2. Build grid for adjacency check (up to 7 rows, 6 columns)
            com.exam.entity.Allocation[][] grid = new com.exam.entity.Allocation[8][6];
            for (com.exam.entity.Allocation a : hallSeats) {
                Integer colIdx = COL_INDEX.get(a.getSeatCol());
                if (colIdx != null && a.getSeatRow() >= 1 && a.getSeatRow() <= 7) {
                    grid[a.getSeatRow()][colIdx] = a;
                }
            }

            // 3. Reset all to 0 first, then flag violations
            hallSeats.forEach(a -> a.setRiskScore(0));

            for (int row = 1; row <= 7; row++) {
                for (int col = 0; col < 6; col++) {
                    com.exam.entity.Allocation current = grid[row][col];
                    if (current == null) continue;

                    for (int[] offset : ADJACENCY_OFFSETS) {
                        int adjRow = row + offset[0];
                        int adjCol = col + offset[1];
                        if (adjRow < 1 || adjRow > 7 || adjCol < 0 || adjCol >= 8) continue;

                        com.exam.entity.Allocation neighbor = grid[adjRow][adjCol];
                        if (neighbor == null) continue;

                        String subjA = current.getStudent().getSubjectCode();
                        String subjB = neighbor.getStudent().getSubjectCode();
                        boolean validSubject = subjA != null && subjB != null && !subjA.trim().isEmpty() && !subjA.trim().equalsIgnoreCase("N/A") && !subjA.trim().equals("-");
                        boolean sameSubject = validSubject && subjA.trim().equalsIgnoreCase(subjB.trim());
                        
                        boolean sameDept = current.getStudent().getDepartment() != null && 
                                         current.getStudent().getDepartment().equals(neighbor.getStudent().getDepartment());
                        boolean isSubjectDominant = sameSubject && dominantSubjects.contains(subjA);

                        if (sameSubject && sameDept && !isPureHall && !isSubjectDominant) {
                            current.setRiskScore(1);
                            neighbor.setRiskScore(1);
                        }
                    }
                }
            }
        }
        allocationRepo.saveAll(allocations);
    }

    public record AuditReport(
            AllocationBatch batch,
            List<HallAuditResult> hallResults,
            int totalStudents,
            int totalHalls,
            int totalAdjacencyChecks,
            int totalAdjacencyViolations,
            double integrityScorePercent,
            double deptPurityScorePercent,
            boolean certified
    ) {}

    public record HallAuditResult(
            String hallName,
            int studentCount,
            int adjacencyChecks,
            int adjacencyViolations,
            List<String> violationDetails,
            int deptPureColumns,
            int totalColumnsUsed,
            Map<String, List<String>> columnDepartments
    ) {}
}

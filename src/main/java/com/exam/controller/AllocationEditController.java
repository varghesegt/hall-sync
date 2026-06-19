package com.exam.controller;

import com.exam.dto.PdfAllocationView;
import com.exam.dto.SwapRequest;
import com.exam.dto.UpdateRegisterNumberRequest;
import com.exam.entity.Allocation;
import com.exam.entity.Hall;
import com.exam.exception.ResourceNotFoundException;
import com.exam.repository.AllocationRepository;
import com.exam.repository.HallRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * VISUAL OVERRIDE CONTROLLER — Purely additive.
 * Provides seat-swap and register-number-edit endpoints.
 * 
 * These endpoints directly modify the allocations table rows.
 * Since PDF/Excel/Summary generators read from the SAME table,
 * all downloads automatically reflect manual edits.
 */
@RestController
@RequestMapping("/api/v1/allocation-batches/{batchId}")
public class AllocationEditController {

    private static final Logger logger = LoggerFactory.getLogger(AllocationEditController.class);

    private final AllocationRepository allocationRepo;
    private final HallRepository hallRepo;
    private final com.exam.service.IntegrityAuditService integrityAuditService;

    public AllocationEditController(AllocationRepository allocationRepo, 
                                   HallRepository hallRepo,
                                   com.exam.service.IntegrityAuditService integrityAuditService) {
        this.allocationRepo = allocationRepo;
        this.hallRepo = hallRepo;
        this.integrityAuditService = integrityAuditService;
    }

    /**
     * SWAP two students' seat positions.
     * Both students swap their (hall, row, col) — essentially trading desks.
     * If seat B is empty, student A simply moves there.
     */
    @PutMapping("/swap")
    @Transactional
    public ResponseEntity<List<PdfAllocationView>> swapSeats(
            @PathVariable UUID batchId,
            @Valid @RequestBody SwapRequest request) {

        logger.info("VISUAL OVERRIDE: Swap requested in batch {} — ({} R{}:{}) ↔ ({} R{}:{})",
                batchId,
                request.hallIdA(), request.seatRowA(), request.seatColA(),
                request.hallIdB(), request.seatRowB(), request.seatColB());

        Allocation allocA = allocationRepo.findBySeatPosition(
                batchId, request.hallIdA(), request.seatRowA(), request.seatColA())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No student found at " + request.hallIdA() + " R" + request.seatRowA() + ":" + request.seatColA()));

        // Seat B might be empty (move to empty seat) or occupied (swap)
        var allocBOpt = allocationRepo.findBySeatPosition(
                batchId, request.hallIdB(), request.seatRowB(), request.seatColB());

        if (allocBOpt.isPresent()) {
            // SWAP: Both seats are occupied. 
            Allocation allocB = allocBOpt.get();

            String hallA = request.hallIdA();
            int rowA = request.seatRowA();
            String colA = request.seatColA();

            String hallB = request.hallIdB();
            int rowB = request.seatRowB();
            String colB = request.seatColB();

            // Step 1: Move A to buffer
            allocationRepo.moveToTempState(allocA.getId());
            
            // Step 2: Move B to A's old spot
            allocationRepo.moveToFinalState(allocB.getId(), hallA, rowA, colA);

            // Step 3: Move A to B's old spot
            allocationRepo.moveToFinalState(allocA.getId(), hallB, rowB, colB);

            logger.info("VISUAL OVERRIDE: Safe Swapped {} ↔ {} at positions ({} R{}:{}) ↔ ({} R{}:{})",
                    allocA.getStudent().getRegisterNumber(),
                    allocB.getStudent().getRegisterNumber(),
                    hallA, rowA, colA, hallB, rowB, colB);
        } else {
            // MOVE: Student A moves to empty seat B.
            // Using direct repository call to bypass Hibernate session issues
            allocationRepo.moveToFinalState(allocA.getId(), request.hallIdB(), request.seatRowB(), request.seatColB());

            logger.info("VISUAL OVERRIDE: Moved {} to {} R{}:{} in batch {}",
                    allocA.getStudent().getRegisterNumber(),
                    request.hallIdB(), request.seatRowB(), request.seatColB(),
                    batchId);
        }

        // RE-CALCULATE INTEGRITY
        integrityAuditService.updateIntegrityRiskScores(batchId);

        // Return refreshed preview data
        List<PdfAllocationView> refreshed = allocationRepo.findPdfViewsByBatchId(batchId);
        return ResponseEntity.ok(refreshed);
    }

    /**
     * UPDATE a student's register number at a specific seat.
     */
    @PutMapping("/update-register")
    @Transactional
    public ResponseEntity<List<PdfAllocationView>> updateRegisterNumber(
            @PathVariable UUID batchId,
            @Valid @RequestBody UpdateRegisterNumberRequest request) {

        logger.info("VISUAL OVERRIDE: Register number update in batch {} — {} R{}:{} → '{}'",
                batchId, request.hallId(), request.seatRow(), request.seatCol(),
                request.newRegisterNumber());

        Allocation alloc = allocationRepo.findBySeatPosition(
                batchId, request.hallId(), request.seatRow(), request.seatCol())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No student found at " + request.hallId() + " R" + request.seatRow() + ":" + request.seatCol()));

        // Using direct repository call
        allocationRepo.updateRegisterNumber(alloc.getId(), request.newRegisterNumber().trim());

        logger.info("VISUAL OVERRIDE: Register number changed for alloc {} to '{}' in batch {}",
                alloc.getId(), request.newRegisterNumber().trim(), batchId);

        // RE-CALCULATE INTEGRITY
        integrityAuditService.updateIntegrityRiskScores(batchId);

        // Return refreshed preview data
        List<PdfAllocationView> refreshed = allocationRepo.findPdfViewsByBatchId(batchId);
        return ResponseEntity.ok(refreshed);
    }
}

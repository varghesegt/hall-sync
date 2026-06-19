package com.exam.controller.internal;

import com.exam.dto.AllocationTriggerRequest;
import com.exam.dto.AllocationTriggerResponse;
import com.exam.dto.BatchStatusResponse;
import com.exam.service.internal.InternalAllocationFacade;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@Validated
@RequestMapping("/api/v1/internal/exam-sessions/{examSessionId}")
public class InternalAllocationController {

    private final InternalAllocationFacade allocationFacade;

    public InternalAllocationController(InternalAllocationFacade allocationFacade) {
        this.allocationFacade = allocationFacade;
    }

    @PostMapping(value = "/allocations", consumes = "application/json", produces = "application/json")
    public ResponseEntity<AllocationTriggerResponse> triggerAllocation(
            @PathVariable UUID examSessionId,
            @Valid @RequestBody AllocationTriggerRequest request) {
        AllocationTriggerResponse response = allocationFacade.triggerAllocation(examSessionId, request);
        return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
    }

    @GetMapping(value = "/batches/latest", produces = "application/json")
    public ResponseEntity<BatchStatusResponse> getLatestBatchStatus(@PathVariable UUID examSessionId) {
        BatchStatusResponse response = allocationFacade.getLatestBatchStatus(examSessionId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}

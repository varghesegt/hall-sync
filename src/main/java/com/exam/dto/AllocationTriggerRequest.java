package com.exam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record AllocationTriggerRequest(
        @NotBlank String requestedBy,
        @NotEmpty(message = "At least one room must be selected")
        List<String> selectedRooms
) {}

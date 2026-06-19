package com.exam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request to update a student's register number at a specific seat.
 * Uses composite key (hallId + seatRow + seatCol) to identify the seat.
 */
public record UpdateRegisterNumberRequest(
        @NotBlank String hallId,
        @NotNull Integer seatRow,
        @NotBlank String seatCol,
        @NotBlank String newRegisterNumber
) {}

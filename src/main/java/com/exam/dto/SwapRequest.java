package com.exam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request to swap two students' seat positions within a batch.
 * Uses composite key (hallId + seatRow + seatCol) to identify each seat.
 */
public record SwapRequest(
        @NotBlank String hallIdA,
        @NotNull Integer seatRowA,
        @NotBlank String seatColA,
        @NotBlank String hallIdB,
        @NotNull Integer seatRowB,
        @NotBlank String seatColB
) {}

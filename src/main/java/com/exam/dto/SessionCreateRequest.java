package com.exam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record SessionCreateRequest(
        @NotNull UUID fileId,
        @NotBlank String seasonId,
        @NotBlank String examName,
        @NotNull LocalDate examDate,
        @NotBlank String session
) {}

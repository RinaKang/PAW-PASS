package com.pawpass.trip.dto;

import com.pawpass.global.util.DataSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record TripRequest(
        @NotNull DataSource source,
        @NotBlank String contentId,
        LocalDate visitedAt
) {
}

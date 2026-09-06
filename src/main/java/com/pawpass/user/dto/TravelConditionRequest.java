package com.pawpass.user.dto;

import jakarta.validation.constraints.NotBlank;

public record TravelConditionRequest(
        @NotBlank String regionCode,
        @NotBlank String travelCategory
) {
}

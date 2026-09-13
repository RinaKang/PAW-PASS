package com.pawpass.user.dto;

import jakarta.validation.constraints.NotNull;

public record PrimaryPetRequest(
        @NotNull Long petId
) {
}

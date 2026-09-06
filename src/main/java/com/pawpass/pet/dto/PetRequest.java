package com.pawpass.pet.dto;

import com.pawpass.pet.domain.PetSize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record PetRequest(
        @NotBlank String name,
        @NotBlank String species,
        @NotBlank String breed,
        @Positive Double weight,
        PetSize size,
        Boolean hasCarrier,
        Boolean hasLeash
) {
}

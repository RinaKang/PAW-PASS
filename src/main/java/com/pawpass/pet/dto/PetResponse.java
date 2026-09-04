package com.pawpass.pet.dto;

import com.pawpass.pet.domain.Pet;
import com.pawpass.pet.domain.PetSize;

import java.time.LocalDateTime;

public record PetResponse(
        Long id,
        String name,
        String breed,
        Double weight,
        PetSize size,
        boolean hasCarrier,
        boolean hasStroller,
        LocalDateTime createdAt
) {
    public static PetResponse from(Pet pet) {
        return new PetResponse(
                pet.getId(), pet.getName(), pet.getBreed(), pet.getWeight(),
                pet.getSize(), pet.isHasCarrier(), pet.isHasStroller(), pet.getCreatedAt()
        );
    }
}

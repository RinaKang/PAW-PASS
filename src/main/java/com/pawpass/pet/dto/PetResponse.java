package com.pawpass.pet.dto;

import com.pawpass.pet.domain.Pet;
import com.pawpass.pet.domain.PetSize;

import java.time.LocalDateTime;

public record PetResponse(
        Long id,
        String name,
        String species,
        String breed,
        Double weight,
        PetSize size,
        boolean hasCarrier,
        boolean hasLeash,
        boolean hasMuzzle,
        boolean hasWasteBags,
        boolean hasStroller,
        boolean hasDiaper,
        LocalDateTime createdAt,
        boolean isPrimary
) {
    /** isPrimary 없이 쓰는 기존 호출부 호환용 - 대표 반려동물 여부를 모르는 컨텍스트에선 false로 채운다. */
    public static PetResponse from(Pet pet) {
        return from(pet, false);
    }

    /** isPrimary는 User.primaryPetId와 이 pet의 id가 같은지(2026-09-13 추가) - PetService에서 계산해 넘긴다. */
    public static PetResponse from(Pet pet, boolean isPrimary) {
        return new PetResponse(
                pet.getId(), pet.getName(), pet.getSpecies(), pet.getBreed(), pet.getWeight(),
                pet.getSize(), pet.isHasCarrier(), pet.isHasLeash(), pet.isHasMuzzle(), pet.isHasWasteBags(),
                pet.isHasStroller(), pet.isHasDiaper(), pet.getCreatedAt(), isPrimary
        );
    }
}

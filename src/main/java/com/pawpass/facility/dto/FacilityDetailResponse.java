package com.pawpass.facility.dto;

import com.pawpass.facility.domain.PetFacility;

import java.time.LocalDateTime;

/**
 * README API 명세서의 { title, addr, tel, hours, pet_condition{...}, synced_at }에
 * KCISA 실제 응답으로 새로 확보된 필드(입장 가능 동물 크기/전용 여부/추가 요금)를 pet_condition에 보강해서 반환.
 */
public record FacilityDetailResponse(
        String title,
        String addr,
        String tel,
        String hours,
        PetCondition petCondition,
        LocalDateTime syncedAt
) {
    public record PetCondition(
            String operatingHours,
            String petRestriction,
            Boolean parkingAvailable,
            String allowedPetSize,
            String petExclusive,
            String additionalPetFee
    ) {
    }

    public static FacilityDetailResponse from(PetFacility facility) {
        return new FacilityDetailResponse(
                facility.getTitle(),
                facility.getAddress(),
                facility.getTel(),
                facility.getOperatingHours(),
                new PetCondition(
                        facility.getOperatingHours(),
                        facility.getPetRestriction(),
                        facility.getParkingAvailable(),
                        facility.getAllowedPetSize(),
                        facility.getPetExclusive(),
                        facility.getAdditionalPetFee()
                ),
                facility.getSyncedAt()
        );
    }
}

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
        LocalDateTime syncedAt,
        Double lat,
        Double lng
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

    /**
     * lat/lng(2026-09-13 추가) - FacilitySummaryResponse(목록)엔 있는데 상세엔 없어서 상세 페이지 지도에
     * 핀을 못 찍던 실제 프론트 리포트로 발견됨. KCISA는 외부 호출 불필요 - PetFacility에 이미 저장돼 있는
     * 값을 그대로 실어주면 된다.
     */
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
                facility.getSyncedAt(),
                facility.getLat(),
                facility.getLng()
        );
    }
}

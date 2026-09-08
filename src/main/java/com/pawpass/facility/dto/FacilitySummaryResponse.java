package com.pawpass.facility.dto;

import com.pawpass.facility.domain.PetFacility;

public record FacilitySummaryResponse(
        String id,
        String title,
        String addr,
        String tel,
        Double lat,
        Double lng
) {
    public static FacilitySummaryResponse from(PetFacility facility) {
        return new FacilitySummaryResponse(
                facility.getId(), facility.getTitle(), facility.getAddress(),
                facility.getTel(), facility.getLat(), facility.getLng()
        );
    }
}

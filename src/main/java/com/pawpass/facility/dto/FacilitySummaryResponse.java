package com.pawpass.facility.dto;

import com.pawpass.facility.domain.PetFacility;

public record FacilitySummaryResponse(
        String id,
        String title,
        String addr,
        String tel,
        Double lat,
        Double lng,
        String image
) {
    public static FacilitySummaryResponse from(PetFacility facility) {
        return from(facility, null);
    }

    /** image는 구글 Places 사진 URL 정책상 캐싱 금지라 엔티티가 아니라 매 요청마다 서비스 레이어에서 실시간으로 채워 넣는다. */
    public static FacilitySummaryResponse from(PetFacility facility, String image) {
        return new FacilitySummaryResponse(
                facility.getId(), facility.getTitle(), facility.getAddress(),
                facility.getTel(), facility.getLat(), facility.getLng(), image
        );
    }
}

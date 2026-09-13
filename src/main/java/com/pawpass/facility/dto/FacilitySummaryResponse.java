package com.pawpass.facility.dto;

import com.pawpass.facility.domain.PetFacility;

public record FacilitySummaryResponse(
        String id,
        String title,
        String addr,
        String tel,
        Double lat,
        Double lng,
        String image,
        String imageAttribution,
        String category
) {
    /** category 없이 쓰는 기존 호출부 호환용. */
    public FacilitySummaryResponse(String id, String title, String addr, String tel, Double lat, Double lng,
                                    String image, String imageAttribution) {
        this(id, title, addr, tel, lat, lng, image, imageAttribution, null);
    }

    public static FacilitySummaryResponse from(PetFacility facility) {
        return from(facility, null, null);
    }

    /**
     * image/imageAttribution은 구글 Places 정책상 캐싱 금지라 엔티티가 아니라 매 요청마다 서비스 레이어에서
     * 실시간으로 채워 넣는다. imageAttribution은 사진을 화면에 노출할 때 구글 정책상 같이 표시해야 하는
     * 저작자 표시 문구 - 저작자 정보가 없는 사진이면 null(표시 생략).
     * category는 category3(예: "동물병원", "카페", "펜션") - 프론트가 카테고리별 플레이스홀더 이미지를
     * 고르는 등의 용도로 요청(2026-09-13).
     */
    public static FacilitySummaryResponse from(PetFacility facility, String image, String imageAttribution) {
        return new FacilitySummaryResponse(
                facility.getId(), facility.getTitle(), facility.getAddress(),
                facility.getTel(), facility.getLat(), facility.getLng(), image, imageAttribution,
                facility.getCategory3()
        );
    }
}

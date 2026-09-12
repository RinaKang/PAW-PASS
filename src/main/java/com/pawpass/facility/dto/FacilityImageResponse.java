package com.pawpass.facility.dto;

/**
 * GET /facilities/{id}/image 전용 응답. 시설 본문(GET /facilities/{id})과 사진을 분리한 이유:
 * 구글 Places 사진은 캐싱 금지라 매번 실시간으로 2단계 외부 호출(Place Details -> Photo Media)이
 * 필요한데, 이걸 시설 상세 조회에 얹으면 페이지 진입 자체가 매번 그 지연을 그대로 떠안는다.
 * 본문은 즉시 응답하고, 프론트가 이 엔드포인트를 별도로(병렬/지연) 호출해서 사진만 나중에 채워 넣는
 * 용도 - 프론트 요청(2026-09-12): "상세 페이지 진입 시 구글 API 지연으로 체감 속도 저하"에 대한 대응.
 */
public record FacilityImageResponse(String image, String imageAttribution) {
    public static final FacilityImageResponse EMPTY = new FacilityImageResponse(null, null);
}

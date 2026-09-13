package com.pawpass.explore.dto;

import com.pawpass.facility.dto.FacilitySummaryResponse;
import com.pawpass.tour.dto.TourSummaryResponse;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public record ExploreItem(
        String source,
        String id,
        String title,
        String addr,
        String image,
        String imageAttribution,
        String category,
        Double lat,
        Double lng,
        String dedupKey,
        String matchStatus
) {
    /**
     * category는 소스별 원본 규격 그대로 통과시킨다(공통 규격으로 변환하지 않음) - tourapi는 관광공사
     * contentTypeId(12=관광지 등), kcisa는 category3(예: "동물병원", "카페") 값. 프론트가 카테고리별
     * 플레이스홀더 이미지를 고르는 등의 용도로 요청(2026-09-13) - "동물병원"처럼 /explore의 공통
     * 카테고리(NATURE/CAFE/FOOD/CULTURE/STAY)에 아예 없는 값도 있어서, 공통 규격 대신 원본 그대로 준다.
     */
    public static ExploreItem fromTour(TourSummaryResponse tour, String matchStatus) {
        // TourAPI 좌표 표기 관례: mapX=경도(longitude), mapY=위도(latitude)
        // TourAPI 자체 이미지라 구글 Places 저작자 표시 대상이 아님 - imageAttribution은 항상 null.
        return new ExploreItem("tourapi", tour.contentId(), tour.title(), tour.addr(), tour.image(), null,
                tour.category(), tour.mapY(), tour.mapX(), dedupKey(tour.title()), matchStatus);
    }

    /**
     * KCISA 원본 데이터셋 자체에는 이미지 URL 필드가 없어서(2026-09-11 확인), FacilityService가 구글
     * Places API(New)로 실시간 조회해 채워 넣은 값(과 저작자 표시)을 그대로 통과시킨다. place_id가 아직
     * 없거나(조회 시점에 지연 해소, FacilityService.resolvePlaceId 참고), 페이지당 사진 조회 상한
     * (FacilityService.MAX_ITEMS_TO_FETCH_IMAGE)을 넘긴 항목은 null이라 프론트에서 플레이스홀더 이미지로
     * 처리해야 한다. imageAttribution이 있으면(구글 정책상) 사진과 같이 화면에 표시해야 한다.
     */
    public static ExploreItem fromFacility(FacilitySummaryResponse facility, String matchStatus) {
        return new ExploreItem("kcisa", facility.id(), facility.title(), facility.addr(), facility.image(),
                facility.imageAttribution(), facility.category(), facility.lat(), facility.lng(),
                dedupKey(facility.title()), matchStatus);
    }

    public ExploreItem withMatchStatus(String newMatchStatus) {
        return new ExploreItem(source, id, title, addr, image, imageAttribution, category, lat, lng, dedupKey, newMatchStatus);
    }

    /**
     * 좌표가 둘 다 있으면 ExploreService가 실제로는 이 값 대신 좌표 근접도로 중복을 판단한다 - dedupKey는
     * 좌표가 없는 항목(간혹 TourAPI mapx/mapy가 빈 값으로 오는 경우)에 대한 폴백, 그리고 응답 필드(dedup_key)
     * 자체가 명세에 있어서 항상 채워둔다.
     */
    private static String dedupKey(String title) {
        String normalized = title == null ? "" : title.replaceAll("\\s+", "").toLowerCase();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 찾을 수 없습니다.", e);
        }
    }
}

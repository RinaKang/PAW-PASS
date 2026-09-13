package com.pawpass.tour.dto;

import com.pawpass.tour.dto.external.TourAreaItem;

public record TourSummaryResponse(
        String contentId,
        String title,
        String addr,
        String tel,
        String image,
        Double mapX,
        Double mapY,
        String cat3,
        String category
) {
    /** cat3/category 없이 쓰는 기존 호출부 호환용. */
    public TourSummaryResponse(String contentId, String title, String addr, String tel, String image,
                                Double mapX, Double mapY) {
        this(contentId, title, addr, tel, image, mapX, mapY, null, null);
    }

    /** category(=contentTypeId) 없이 cat3만 쓰는 기존 호출부 호환용 - explore 카페/음식점 구분 필터링 테스트에서 씀. */
    public TourSummaryResponse(String contentId, String title, String addr, String tel, String image,
                                Double mapX, Double mapY, String cat3) {
        this(contentId, title, addr, tel, image, mapX, mapY, cat3, null);
    }

    /**
     * category는 관광공사 contentTypeId(12=관광지, 14=문화시설, 15=행사, 25=여행코스, 28=레포츠, 32=숙박,
     * 38=쇼핑, 39=음식점) 그대로 - 프론트가 카테고리별 플레이스홀더 이미지를 고르는 등의 용도로 요청
     * (2026-09-13).
     */
    public static TourSummaryResponse from(TourAreaItem item) {
        return new TourSummaryResponse(
                item.contentId(),
                item.title(),
                item.addr1(),
                item.tel(),
                item.firstimage(),
                parseDouble(item.mapx()),
                parseDouble(item.mapy()),
                item.cat3(),
                item.contentTypeId()
        );
    }

    private static Double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

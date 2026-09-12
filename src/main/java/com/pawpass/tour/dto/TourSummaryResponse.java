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
        String cat3
) {
    /** cat3(구 분류체계 소분류) 없이 쓰는 기존 호출부 호환용 - explore 쪽 카페/음식점 구분 필터링에만 필요. */
    public TourSummaryResponse(String contentId, String title, String addr, String tel, String image,
                                Double mapX, Double mapY) {
        this(contentId, title, addr, tel, image, mapX, mapY, null);
    }

    public static TourSummaryResponse from(TourAreaItem item) {
        return new TourSummaryResponse(
                item.contentId(),
                item.title(),
                item.addr1(),
                item.tel(),
                item.firstimage(),
                parseDouble(item.mapx()),
                parseDouble(item.mapy()),
                item.cat3()
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

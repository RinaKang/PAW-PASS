package com.pawpass.tour.dto;

import com.pawpass.tour.dto.external.TourAreaItem;

public record TourSummaryResponse(
        String contentId,
        String title,
        String addr,
        String tel,
        String image,
        Double mapX,
        Double mapY
) {
    public static TourSummaryResponse from(TourAreaItem item) {
        return new TourSummaryResponse(
                item.contentId(),
                item.title(),
                item.addr1(),
                item.tel(),
                item.firstimage(),
                parseDouble(item.mapx()),
                parseDouble(item.mapy())
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

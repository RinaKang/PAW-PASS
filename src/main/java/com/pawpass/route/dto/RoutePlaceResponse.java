package com.pawpass.route.dto;

import com.pawpass.global.util.DataSource;
import com.pawpass.route.domain.RoutePlace;

public record RoutePlaceResponse(
        Long id,
        DataSource source,
        String contentId,
        String title,
        Double lat,
        Double lng
) {
    public static RoutePlaceResponse from(RoutePlace place) {
        return new RoutePlaceResponse(
                place.getId(), place.getSource(), place.getContentId(),
                place.getTitle(), place.getLat(), place.getLng()
        );
    }
}

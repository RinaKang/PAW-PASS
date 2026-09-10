package com.pawpass.trip.dto;

import com.pawpass.explore.service.PlaceLookupService;
import com.pawpass.global.util.DataSource;
import com.pawpass.trip.domain.Trip;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TripResponse(
        Long id,
        DataSource source,
        String contentId,
        String title,
        String addr,
        LocalDate visitedAt,
        LocalDateTime createdAt
) {
    public static TripResponse from(Trip trip) {
        return new TripResponse(
                trip.getId(), trip.getSource(), trip.getContentId(), null, null, trip.getVisitedAt(), trip.getCreatedAt()
        );
    }

    public static TripResponse withDetail(Trip trip, PlaceLookupService.PlaceDetail detail) {
        return new TripResponse(
                trip.getId(), trip.getSource(), trip.getContentId(),
                detail == null ? null : detail.title(),
                detail == null ? null : detail.addr(),
                trip.getVisitedAt(), trip.getCreatedAt()
        );
    }
}

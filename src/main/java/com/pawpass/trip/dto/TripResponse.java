package com.pawpass.trip.dto;

import com.pawpass.global.util.DataSource;
import com.pawpass.trip.domain.Trip;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TripResponse(
        Long id,
        DataSource source,
        String contentId,
        LocalDate visitedAt,
        LocalDateTime createdAt
) {
    public static TripResponse from(Trip trip) {
        return new TripResponse(
                trip.getId(), trip.getSource(), trip.getContentId(), trip.getVisitedAt(), trip.getCreatedAt()
        );
    }
}

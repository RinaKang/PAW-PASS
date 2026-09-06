package com.pawpass.user.dto;

import com.pawpass.user.domain.User;

import java.time.LocalDateTime;

public record TravelConditionResponse(
        String regionCode,
        String travelCategory,
        LocalDateTime updatedAt
) {
    public static TravelConditionResponse from(User user) {
        return new TravelConditionResponse(user.getRegionCode(), user.getTravelCategory(), user.getUpdatedAt());
    }
}

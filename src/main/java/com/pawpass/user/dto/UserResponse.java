package com.pawpass.user.dto;

import com.pawpass.user.domain.User;

public record UserResponse(
        Long id,
        String email,
        String name,
        String picture,
        String regionCode,
        String travelCategory
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(), user.getEmail(), user.getName(), user.getPicture(),
                user.getRegionCode(), user.getTravelCategory()
        );
    }
}

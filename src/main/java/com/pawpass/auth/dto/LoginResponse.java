package com.pawpass.auth.dto;

import com.pawpass.user.dto.UserResponse;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        UserResponse user
) {
}

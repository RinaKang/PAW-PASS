package com.pawpass.auth.dto;

public record TokenResponse(String accessToken, String refreshToken) {
}

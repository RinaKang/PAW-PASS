package com.pawpass.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * https://www.googleapis.com/oauth2/v3/userinfo 응답.
 * sub = 구글 계정 고유 식별자 -> User.googleId로 저장.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleUserInfoResponse(
        String sub,
        String email,
        String name,
        String picture
) {
}

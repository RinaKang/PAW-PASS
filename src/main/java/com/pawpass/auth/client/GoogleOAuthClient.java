package com.pawpass.auth.client;

import com.pawpass.auth.dto.GoogleTokenResponse;
import com.pawpass.auth.dto.GoogleUserInfoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 구글 OAuth 인가코드 -> 토큰 교환, 액세스 토큰 -> 사용자 정보 조회.
 * redirect_uri는 프론트엔드가 인가코드를 발급받을 때 사용한 값과 정확히 일치해야 구글이 토큰 교환을 허용함.
 */
@Component
@RequiredArgsConstructor
public class GoogleOAuthClient {

    private static final String TOKEN_URI = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_URI = "https://www.googleapis.com/oauth2/v3/userinfo";

    private final WebClient.Builder webClientBuilder;

    @Value("${oauth.google.client-id}")
    private String clientId;

    @Value("${oauth.google.client-secret}")
    private String clientSecret;

    @Value("${oauth.google.redirect-uri}")
    private String redirectUri;

    public GoogleTokenResponse exchangeCodeForToken(String code) {
        return webClientBuilder.build().post()
                .uri(TOKEN_URI)
                .body(BodyInserters.fromFormData("code", code)
                        .with("client_id", clientId)
                        .with("client_secret", clientSecret)
                        .with("redirect_uri", redirectUri)
                        .with("grant_type", "authorization_code"))
                .retrieve()
                .bodyToMono(GoogleTokenResponse.class)
                .block();
    }

    public GoogleUserInfoResponse fetchUserInfo(String accessToken) {
        return webClientBuilder.build().get()
                .uri(USERINFO_URI)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(GoogleUserInfoResponse.class)
                .block();
    }
}

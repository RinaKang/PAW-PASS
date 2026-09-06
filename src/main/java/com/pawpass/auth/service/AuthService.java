package com.pawpass.auth.service;

import com.pawpass.auth.client.GoogleOAuthClient;
import com.pawpass.auth.domain.BlacklistedRefreshToken;
import com.pawpass.auth.dto.AccessTokenResponse;
import com.pawpass.auth.dto.GoogleTokenResponse;
import com.pawpass.auth.dto.GoogleUserInfoResponse;
import com.pawpass.auth.dto.LoginResponse;
import com.pawpass.auth.jwt.JwtProvider;
import com.pawpass.auth.repository.BlacklistedRefreshTokenRepository;
import com.pawpass.user.domain.User;
import com.pawpass.user.dto.UserResponse;
import com.pawpass.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final GoogleOAuthClient googleOAuthClient;
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final BlacklistedRefreshTokenRepository blacklistedRefreshTokenRepository;

    @Transactional
    public LoginResponse login(String code) {
        GoogleTokenResponse googleToken = googleOAuthClient.exchangeCodeForToken(code);
        GoogleUserInfoResponse userInfo = googleOAuthClient.fetchUserInfo(googleToken.accessToken());

        User user = userRepository.findByGoogleId(userInfo.sub())
                .orElseGet(() -> userRepository.save(User.builder()
                        .googleId(userInfo.sub())
                        .email(userInfo.email())
                        .name(userInfo.name())
                        .picture(userInfo.picture())
                        .build()));

        return new LoginResponse(
                jwtProvider.generateAccessToken(user.getId()),
                jwtProvider.generateRefreshToken(user.getId()),
                UserResponse.from(user)
        );
    }

    /** 명세서 기준: refresh는 access token만 재발급하고 refresh token은 로그인 시 발급된 것을 만료 전까지 그대로 사용 */
    public AccessTokenResponse refresh(String refreshToken) {
        validateRefreshToken(refreshToken);
        Long userId = jwtProvider.getUserId(refreshToken);
        return new AccessTokenResponse(jwtProvider.generateAccessToken(userId));
    }

    @Transactional
    public void logout(String refreshToken) {
        validateRefreshToken(refreshToken);
        String jti = jwtProvider.getJti(refreshToken);
        blacklistedRefreshTokenRepository.save(
                new BlacklistedRefreshToken(jti, jwtProvider.getExpiration(refreshToken))
        );
    }

    private void validateRefreshToken(String refreshToken) {
        if (!jwtProvider.validateToken(refreshToken) || !jwtProvider.isRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 refresh token입니다.");
        }
        if (blacklistedRefreshTokenRepository.existsById(jwtProvider.getJti(refreshToken))) {
            throw new IllegalArgumentException("로그아웃 처리된 토큰입니다.");
        }
    }
}

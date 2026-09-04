package com.pawpass.auth.service;

import com.pawpass.auth.client.GoogleOAuthClient;
import com.pawpass.auth.dto.GoogleTokenResponse;
import com.pawpass.auth.dto.GoogleUserInfoResponse;
import com.pawpass.auth.dto.TokenResponse;
import com.pawpass.auth.jwt.JwtProvider;
import com.pawpass.user.domain.User;
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

    @Transactional
    public TokenResponse login(String code) {
        GoogleTokenResponse googleToken = googleOAuthClient.exchangeCodeForToken(code);
        GoogleUserInfoResponse userInfo = googleOAuthClient.fetchUserInfo(googleToken.accessToken());

        User user = userRepository.findByGoogleId(userInfo.sub())
                .orElseGet(() -> userRepository.save(User.builder()
                        .googleId(userInfo.sub())
                        .email(userInfo.email())
                        .name(userInfo.name())
                        .picture(userInfo.picture())
                        .build()));

        return issueTokens(user.getId());
    }

    public TokenResponse refresh(String refreshToken) {
        if (!jwtProvider.validateToken(refreshToken) || !jwtProvider.isRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 refresh token입니다.");
        }
        Long userId = jwtProvider.getUserId(refreshToken);
        return issueTokens(userId);
    }

    private TokenResponse issueTokens(Long userId) {
        return new TokenResponse(
                jwtProvider.generateAccessToken(userId),
                jwtProvider.generateRefreshToken(userId)
        );
    }
}

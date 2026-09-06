package com.pawpass.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 로그아웃된 refresh token을 기록해두는 블랙리스트.
 * access token은 수명이 짧아(1시간) 그대로 만료되도록 두고, refresh token만 무효화 대상으로 관리한다.
 */
@Entity
@Table(name = "blacklisted_refresh_tokens")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BlacklistedRefreshToken {

    @Id
    @Column(name = "token_id", length = 36)
    private String tokenId; // JWT의 jti claim

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    public BlacklistedRefreshToken(String tokenId, LocalDateTime expiresAt) {
        this.tokenId = tokenId;
        this.expiresAt = expiresAt;
    }
}

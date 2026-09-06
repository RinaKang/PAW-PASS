package com.pawpass.auth.repository;

import com.pawpass.auth.domain.BlacklistedRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlacklistedRefreshTokenRepository extends JpaRepository<BlacklistedRefreshToken, String> {
}

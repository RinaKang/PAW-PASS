package com.pawpass.auth.controller;

import com.pawpass.auth.dto.TokenResponse;
import com.pawpass.auth.service.AuthService;
import com.pawpass.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * API 명세서 - RQ-06 사용자 인증
 * POST /auth/google
 * POST /auth/refresh
 */
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    public record LoginRequest(String code) {}
    public record RefreshRequest(String refreshToken) {}

    @PostMapping("/auth/google")
    public ApiResponse<TokenResponse> login(@RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request.code()));
    }

    @PostMapping("/auth/refresh")
    public ApiResponse<TokenResponse> refresh(@RequestBody RefreshRequest request) {
        return ApiResponse.success(authService.refresh(request.refreshToken()));
    }
}

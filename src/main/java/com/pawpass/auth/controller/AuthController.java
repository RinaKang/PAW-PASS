package com.pawpass.auth.controller;

import com.pawpass.auth.dto.AccessTokenResponse;
import com.pawpass.auth.dto.LoginResponse;
import com.pawpass.auth.service.AuthService;
import com.pawpass.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * API 명세서 - RQ-06 사용자 인증
 * POST /auth/google_id
 * POST /auth/refresh
 * POST /auth/logout
 */
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    public record LoginRequest(String code) {}
    public record RefreshRequest(String refreshToken) {}

    @PostMapping("/auth/google_id")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request.code()));
    }

    @PostMapping("/auth/refresh")
    public ApiResponse<AccessTokenResponse> refresh(@RequestBody RefreshRequest request) {
        return ApiResponse.success(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
    }
}

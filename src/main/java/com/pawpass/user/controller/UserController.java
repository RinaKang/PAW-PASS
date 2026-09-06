package com.pawpass.user.controller;

import com.pawpass.global.response.ApiResponse;
import com.pawpass.user.dto.TravelConditionRequest;
import com.pawpass.user.dto.TravelConditionResponse;
import com.pawpass.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * API 명세서 - 여행조건 설정 / 회원탈퇴 (RQ-02, RQ-08)
 * PUT    /users/me/travel-condition
 * DELETE /users/me
 */
@RestController
@RequestMapping("/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PutMapping("/travel-condition")
    public ApiResponse<TravelConditionResponse> updateTravelCondition(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody TravelConditionRequest request
    ) {
        return ApiResponse.success(userService.updateTravelCondition(userId, request));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdraw(@AuthenticationPrincipal Long userId) {
        userService.withdraw(userId);
    }
}

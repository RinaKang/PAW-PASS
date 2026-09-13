package com.pawpass.user.controller;

import com.pawpass.global.response.ApiResponse;
import com.pawpass.user.dto.PrimaryPetRequest;
import com.pawpass.user.dto.PrimaryPetResponse;
import com.pawpass.user.dto.TravelConditionRequest;
import com.pawpass.user.dto.TravelConditionResponse;
import com.pawpass.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 여행조건 설정 / 대표 반려동물 지정 / 회원탈퇴
 * PUT    /users/me/travel-condition
 * PUT    /users/me/primary-pet
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

    /**
     * 반려동물 1마리면 등록 시 자동으로 대표가 되고(PetService.create), 2마리 이상이면 이 엔드포인트로
     * 명시적으로 골라야 한다 - /explore, /tours(/facilities){id}/match가 petId를 안 받으면 이 값을 쓴다.
     */
    @PutMapping("/primary-pet")
    public ApiResponse<PrimaryPetResponse> updatePrimaryPet(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PrimaryPetRequest request
    ) {
        return ApiResponse.success(userService.updatePrimaryPet(userId, request.petId()));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdraw(@AuthenticationPrincipal Long userId) {
        userService.withdraw(userId);
    }
}

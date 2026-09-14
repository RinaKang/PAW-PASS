package com.pawpass.user.controller;

import com.pawpass.global.response.ApiResponse;
import com.pawpass.user.dto.PrimaryPetRequest;
import com.pawpass.user.dto.PrimaryPetResponse;
import com.pawpass.user.dto.TravelConditionRequest;
import com.pawpass.user.dto.TravelConditionResponse;
import com.pawpass.user.dto.UserResponse;
import com.pawpass.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 여행조건 설정 / 대표 반려동물 지정 / 프로필 이미지 업로드 / 회원탈퇴
 * PUT    /users/me/travel-condition
 * PUT    /users/me/primary-pet
 * POST   /users/me/profile-image (multipart/form-data, 2026-09-15 추가)
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

    /**
     * multipart/form-data, 파일 파트 이름은 "image" 고정. jpg/png/webp, 5MB 이하만 허용
     * (ProfileImageStorage 참고) - 그 외엔 400.
     */
    @PostMapping(value = "/profile-image", consumes = "multipart/form-data")
    public ApiResponse<UserResponse> updateProfileImage(
            @AuthenticationPrincipal Long userId,
            @RequestParam("image") MultipartFile image
    ) {
        return ApiResponse.success(userService.updateProfileImage(userId, image));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdraw(@AuthenticationPrincipal Long userId) {
        userService.withdraw(userId);
    }
}

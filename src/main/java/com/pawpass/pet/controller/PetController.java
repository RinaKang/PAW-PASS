package com.pawpass.pet.controller;

import com.pawpass.global.response.ApiResponse;
import com.pawpass.pet.dto.PetRequest;
import com.pawpass.pet.dto.PetResponse;
import com.pawpass.pet.service.PetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 반려동물 프로필 관리
 * POST   /pets
 * GET    /pets
 * PUT    /pets/{id}
 * DELETE /pets/{id}
 * POST   /pets/{id}/profile-image (multipart/form-data, 2026-09-17 추가)
 */
@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;

    @PostMapping
    public ApiResponse<PetResponse> create(@AuthenticationPrincipal Long userId, @Valid @RequestBody PetRequest request) {
        return ApiResponse.success(petService.create(userId, request));
    }

    @GetMapping
    public ApiResponse<List<PetResponse>> getMyPets(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(petService.findAllByUser(userId));
    }

    @PutMapping("/{id}")
    public ApiResponse<PetResponse> update(@AuthenticationPrincipal Long userId, @PathVariable Long id, @RequestBody PetRequest request) {
        return ApiResponse.success(petService.update(userId, id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        petService.delete(userId, id);
    }

    /**
     * multipart/form-data, 파일 파트 이름은 "image" 고정. jpg/png/webp, 5MB 이하만 허용
     * (ProfileImageStorage 참고) - 그 외엔 400. 본인 소유가 아닌 반려동물이면 400.
     */
    @PostMapping(value = "/{id}/profile-image", consumes = "multipart/form-data")
    public ApiResponse<PetResponse> updateProfileImage(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id,
            @RequestParam("image") MultipartFile image
    ) {
        return ApiResponse.success(petService.updateProfileImage(userId, id, image));
    }
}

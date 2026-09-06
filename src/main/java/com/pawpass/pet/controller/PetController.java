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

import java.util.List;

/**
 * 반려동물 프로필 관리
 * POST   /pets
 * GET    /pets
 * PUT    /pets/{id}
 * DELETE /pets/{id}
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
}

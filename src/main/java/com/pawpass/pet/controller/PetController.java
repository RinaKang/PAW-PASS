package com.pawpass.pet.controller;

import com.pawpass.global.response.ApiResponse;
import com.pawpass.pet.dto.PetRequest;
import com.pawpass.pet.dto.PetResponse;
import com.pawpass.pet.service.PetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API 명세서 - RQ-01 반려동물 프로필 관리
 * POST   /pets
 * GET    /pets
 * PUT    /pets/{id}
 * DELETE /pets/{id}
 *
 * TODO: 인증 붙으면 @AuthenticationPrincipal 등으로 userId 추출하도록 교체
 */
@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;

    @PostMapping
    public ApiResponse<PetResponse> create(@Valid @RequestBody PetRequest request /*, Long userId (인증 붙으면 추가) */) {
        // TODO: userId는 인증 붙기 전까지 임시로 request에 포함하거나 고정값 사용
        return ApiResponse.success(petService.create(request));
    }

    @GetMapping
    public ApiResponse<List<PetResponse>> getMyPets(/* Long userId */) {
        return ApiResponse.success(petService.findAllByUser());
    }

    @PutMapping("/{id}")
    public ApiResponse<PetResponse> update(@PathVariable Long id, @RequestBody PetRequest request) {
        return ApiResponse.success(petService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        petService.delete(id);
    }
}

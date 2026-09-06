package com.pawpass.favorite.controller;

import com.pawpass.favorite.dto.FavoriteRequest;
import com.pawpass.favorite.dto.FavoriteResponse;
import com.pawpass.favorite.service.FavoriteService;
import com.pawpass.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 *  즐겨찾기
 * POST   /favorites   { source, content_id }
 * DELETE /favorites/{id}
 * GET    /favorites
 */
@RestController
@RequestMapping("/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping
    public ApiResponse<FavoriteResponse> add(@AuthenticationPrincipal Long userId, @Valid @RequestBody FavoriteRequest request) {
        return ApiResponse.success(favoriteService.add(userId, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        favoriteService.remove(userId, id);
    }

    @GetMapping
    public ApiResponse<List<FavoriteResponse>> list(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(favoriteService.findAllByUser(userId));
    }
}

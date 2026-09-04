package com.pawpass.favorite.controller;

import com.pawpass.global.response.ApiResponse;
import com.pawpass.global.util.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * API 명세서 - RQ-07 즐겨찾기
 * POST   /favorites   { source, content_id }
 * DELETE /favorites/{id}
 * GET    /favorites
 */
@RestController
@RequiredArgsConstructor
public class FavoriteController {

    public record FavoriteRequest(DataSource source, String contentId) {}

    @PostMapping("/favorites")
    public ApiResponse<Object> add(@RequestBody FavoriteRequest request) {
        // TODO: FavoriteService 구현 - UNIQUE(user_id, source, content_id) 위반 시 예외 처리
        throw new UnsupportedOperationException("TODO: FavoriteService 구현 필요");
    }

    @DeleteMapping("/favorites/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long id) {
        throw new UnsupportedOperationException("TODO: FavoriteService 구현 필요");
    }

    @GetMapping("/favorites")
    public ApiResponse<Object> list() {
        // TODO: source별로 tour(실시간) / facility(DB) 상세정보 조인해서 반환
        throw new UnsupportedOperationException("TODO: FavoriteService 구현 필요");
    }
}

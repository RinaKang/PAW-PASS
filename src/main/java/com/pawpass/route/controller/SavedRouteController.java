package com.pawpass.route.controller;

import com.pawpass.global.response.ApiResponse;
import com.pawpass.route.dto.RoutePlaceResponse;
import com.pawpass.route.dto.RoutePlacesRequest;
import com.pawpass.route.service.SavedRouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 사용자가 편집 중인 동선(장소 목록) 저장/조회 - 사용자당 1개, PC/모바일 등 기기 간 동기화용.
 * POST /route/suggest(RouteController)와 달리 이쪽은 로그인 필요.
 *
 * GET /routes
 * 응답: [ { id, source, content_id, title, lat, lng } ] - 배열 순서 = 동선 순서
 *
 * PUT /routes
 * 요청: { places: [ { source, content_id, title, lat, lng } ] } - 추가/삭제/순서변경 구분 없이
 * 통째로 교체한다. 프론트가 지금 화면에 들고 있는 최종 목록을 그대로 보내면 된다.
 */
@RestController
@RequestMapping("/routes")
@RequiredArgsConstructor
public class SavedRouteController {

    private final SavedRouteService savedRouteService;

    @GetMapping
    public ApiResponse<List<RoutePlaceResponse>> list(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(savedRouteService.findAll(userId));
    }

    @PutMapping
    public ApiResponse<List<RoutePlaceResponse>> replace(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody RoutePlacesRequest request
    ) {
        return ApiResponse.success(savedRouteService.replaceAll(userId, request.places()));
    }
}

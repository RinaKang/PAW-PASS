package com.pawpass.route.controller;

import com.pawpass.global.response.ApiResponse;
import com.pawpass.route.dto.RouteSuggestRequest;
import com.pawpass.route.dto.RouteSuggestResponse;
import com.pawpass.route.service.RouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 선택한 지점들의 동선(방문 순서) 추천.
 * POST /route/suggest
 */
@RestController
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    @PostMapping("/route/suggest")
    public ApiResponse<RouteSuggestResponse> suggest(@Valid @RequestBody RouteSuggestRequest request) {
        return ApiResponse.success(routeService.suggest(request));
    }
}

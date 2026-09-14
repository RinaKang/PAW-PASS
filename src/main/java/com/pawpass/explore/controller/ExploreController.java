package com.pawpass.explore.controller;

import com.pawpass.explore.dto.ExploreItem;
import com.pawpass.explore.service.ExploreService;
import com.pawpass.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API 명세서 - GET /explore
 * tour(실시간) + facility(DB) 결과를 합쳐서 반환. 중복 시 관광공사(tourapi) 우선 노출.
 * petId를 넘기면 항목마다 실제 매칭 상태(match_status)를 계산해서 채운다 - 안 넘기면 전부 "확인필요".
 * petId는 있고 matchStatus를 명시하지 않은 기본 요청은 가능/조건부만 반환한다
 * 불가/확인필요까지 보려면 matchStatus를 명시적으로 넘겨야 한다.
 * keyword를 넘기면(2026-09-14 추가, 동선 화면 장소 검색용) regionCode/category는 무시하고 이름/주소로
 * 검색하며, 이 모드에서는 가능/조건부 기본 필터도 적용하지 않는다(찾는 장소가 걸러지면 안 되므로).
 */
@RestController
@RequiredArgsConstructor
public class ExploreController {

    private final ExploreService exploreService;

    @GetMapping("/explore")
    public ApiResponse<List<ExploreItem>> explore(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) String regionCode,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String matchStatus,
            @RequestParam(required = false) Long petId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.success(exploreService.explore(userId, regionCode, category, matchStatus, petId, page, keyword));
    }
}

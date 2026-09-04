package com.pawpass.explore.controller;

import com.pawpass.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * API 명세서 - GET /explore
 * tour(실시간) + facility(DB) 결과를 합쳐서 반환. 중복 시 관광공사(tourapi) 우선 노출.
 *
 * TODO: ExploreService가 TourApiService + FacilityService 둘 다 주입받아 병합 로직 수행
 * TODO: 중복 판단 기준 정의 필요 (주소 유사도 or 좌표 근접도 - 팀 협의 필요)
 */
@RestController
@RequiredArgsConstructor
public class ExploreController {

    @GetMapping("/explore")
    public ApiResponse<Object> explore(
            @RequestParam(required = false) String regionCode,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String matchStatus,
            @RequestParam(defaultValue = "1") int page
    ) {
        throw new UnsupportedOperationException("TODO: ExploreService 구현 필요");
    }
}

package com.pawpass.tour.controller;

import com.pawpass.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * API 명세서 - RQ-02 관광지 탐색 (한국관광공사 TourAPI)
 * GET /tours
 * GET /tours/{contentId}
 *
 * ⚠️ 중요: 이 도메인은 절대 DB에 저장하지 않음 (관광공사 공식 정책 - 실시간 호출만 허용).
 * 캐싱/영속화 로직을 여기 추가하면 안 됨. facility 패키지(KCISA)와 반드시 구분할 것.
 *
 * TODO: TourApiClient 구현 (WebClient로 areaBasedList2, detailPetTour2 등 호출)
 * TODO: TourApiService에서 응답 파싱 + PetConditionDto 매핑
 */
@RestController
@RequiredArgsConstructor
public class TourController {

    // private final TourApiService tourApiService;

    @GetMapping("/tours")
    public ApiResponse<Object> search(
            @RequestParam(required = false) String regionCode,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") int page
    ) {
        // TODO: tourApiService.search(regionCode, category, page) 실시간 호출
        throw new UnsupportedOperationException("TODO: TourApiService 구현 필요");
    }

    @GetMapping("/tours/{contentId}")
    public ApiResponse<Object> getDetail(@PathVariable String contentId) {
        // TODO: tourApiService.getDetail(contentId) - detailCommon2 + detailPetTour2 조합 호출
        throw new UnsupportedOperationException("TODO: TourApiService 구현 필요");
    }
}

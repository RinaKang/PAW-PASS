package com.pawpass.tour.controller;

import com.pawpass.global.response.ApiResponse;
import com.pawpass.tour.dto.TourSummaryResponse;
import com.pawpass.tour.service.TourService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 관광지 탐색 (한국관광공사 TourAPI)
 * GET /tours
 * GET /tours/{contentId}
 *
 * 중요: 이 도메인은 절대 DB에 저장하지 않음 (관광공사 공모전 정책 - 실시간 호출만 허용).
 * 캐싱/영속화 로직을 여기 추가하면 안 됨. facility 패키지(KCISA)와 반드시 구분할 것.
 *
 * TODO: /tours/{contentId} - detailCommon2 + detailIntro2 + detailPetTour2 조합 구현 필요
 */
@RestController
@RequiredArgsConstructor
public class TourController {

    private final TourService tourService;

    @GetMapping("/tours")
    public ApiResponse<List<TourSummaryResponse>> search(
            @RequestParam(required = false) String regionCode,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") int page
    ) {
        return ApiResponse.success(tourService.search(regionCode, category, page));
    }

    @GetMapping("/tours/{contentId}")
    public ApiResponse<Object> getDetail(@PathVariable String contentId) {
        // TODO: tourApiService.getDetail(contentId) - detailCommon2 + detailPetTour2 조합 호출
        throw new UnsupportedOperationException("TODO: TourApiService 구현 필요");
    }
}

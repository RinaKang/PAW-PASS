package com.pawpass.facility.controller;

import com.pawpass.facility.dto.FacilityDetailResponse;
import com.pawpass.facility.dto.FacilitySummaryResponse;
import com.pawpass.facility.service.FacilityService;
import com.pawpass.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API 명세서 - RQ-02 문화시설 탐색 (한국문화정보원 KCISA, 우리 DB 조회)
 * GET /facilities
 * GET /facilities/{id}
 *
 * ⚠️ regionCode는 법정동 코드가 아니라 address 부분 일치 검색임 (PetFacilityRepository.search 참고).
 */
@RestController
@RequiredArgsConstructor
public class FacilityController {

    private final FacilityService facilityService;

    @GetMapping("/facilities")
    public ApiResponse<List<FacilitySummaryResponse>> search(
            @RequestParam(required = false) String regionCode,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") int page
    ) {
        return ApiResponse.success(facilityService.search(regionCode, category, page));
    }

    @GetMapping("/facilities/{id}")
    public ApiResponse<FacilityDetailResponse> getDetail(@PathVariable String id) {
        return ApiResponse.success(facilityService.getDetail(id));
    }
}

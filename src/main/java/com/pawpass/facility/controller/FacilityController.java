package com.pawpass.facility.controller;

import com.pawpass.facility.dto.FacilityDetailResponse;
import com.pawpass.facility.dto.FacilityImageResponse;
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
 * GET /facilities/{id}/image
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

    /**
     * 시설 본문(getDetail)과 사진을 의도적으로 분리한 엔드포인트 - 구글 Places 사진은 매번 실시간 조회가
     * 필요해서(캐싱 금지) 본문에 얹으면 상세 페이지 진입 자체가 그 지연을 떠안는다. 프론트가 본문은 먼저
     * 받아 바로 렌더링하고, 이 엔드포인트는 병렬로(또는 렌더링 후 지연) 호출해서 사진만 나중에 채우는
     * 용도로 쓰면 된다(FacilityImageResponse 주석 참고).
     */
    @GetMapping("/facilities/{id}/image")
    public ApiResponse<FacilityImageResponse> getImage(@PathVariable String id) {
        return ApiResponse.success(facilityService.getImage(id));
    }
}

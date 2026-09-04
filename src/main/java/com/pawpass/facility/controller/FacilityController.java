package com.pawpass.facility.controller;

import com.pawpass.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * API 명세서 - RQ-02 문화시설 탐색 (한국문화정보원 KCISA, 우리 DB 조회)
 * GET /facilities
 * GET /facilities/{id}
 *
 * ⚠️ 전제조건: 캐싱 허용 확정 시에만 이 구조 유지.
 * "불허" 답변 오면 tour 패키지처럼 실시간 호출 구조로 재설계 필요.
 *
 * TODO: FacilityRepository - 위치기반(lat/lng 반경) 검색 쿼리 구현 필요
 */
@RestController
@RequiredArgsConstructor
public class FacilityController {

    // private final FacilityService facilityService;

    @GetMapping("/facilities")
    public ApiResponse<Object> search(
            @RequestParam(required = false) String regionCode,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") int page
    ) {
        // TODO: facilityService.search(regionCode, category, page) - pet_facilities 테이블 조회
        throw new UnsupportedOperationException("TODO: FacilityService 구현 필요");
    }

    @GetMapping("/facilities/{id}")
    public ApiResponse<Object> getDetail(@PathVariable String id) {
        throw new UnsupportedOperationException("TODO: FacilityService 구현 필요");
    }
}

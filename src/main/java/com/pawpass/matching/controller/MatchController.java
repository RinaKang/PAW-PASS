package com.pawpass.matching.controller;

import com.pawpass.global.response.ApiResponse;
import com.pawpass.matching.dto.MatchResponse;
import com.pawpass.matching.service.MatchingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 개인별 방문 가능 여부 매칭 (핵심 기능)
 * GET /tours/{contentId}/match?pet_id=
 * GET /facilities/{id}/match?pet_id=
 *
 * 응답: { status: 가능|조건부|불가|확인필요, reason, raw_text }
 *
 * petId는 이제 선택 사항이다(2026-09-13) - 안 넘기면 대표 반려동물(User.primaryPetId)로 매칭한다.
 * 대표 반려동물도 없으면 400으로 "먼저 선택해주세요" 안내(MatchingService.requirePetForMatch 참고).
 */
@RestController
@RequiredArgsConstructor
public class MatchController {

    private final MatchingService matchingService;

    @GetMapping("/tours/{contentId}/match")
    public ApiResponse<MatchResponse> matchTour(
            @AuthenticationPrincipal Long userId, @PathVariable String contentId,
            @RequestParam(required = false) Long petId) {
        return ApiResponse.success(matchingService.matchTour(userId, contentId, petId));
    }

    @GetMapping("/facilities/{id}/match")
    public ApiResponse<MatchResponse> matchFacility(
            @AuthenticationPrincipal Long userId, @PathVariable String id,
            @RequestParam(required = false) Long petId) {
        return ApiResponse.success(matchingService.matchFacility(userId, id, petId));
    }
}

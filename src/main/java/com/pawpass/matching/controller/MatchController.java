package com.pawpass.matching.controller;

import com.pawpass.global.response.ApiResponse;
import com.pawpass.matching.dto.MatchResponse;
import com.pawpass.matching.service.MatchingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 개인별 방문 가능 여부 매칭 (핵심 기능)
 * GET /tours/{contentId}/match?pet_id=
 * GET /facilities/{id}/match?pet_id=
 *
 * 응답: { status: 가능|조건부|불가|확인필요, reason, raw_text }
 *
 * petId는 이제 선택 사항이다(2026-09-13) - 안 넘기면 대표 반려동물(User.primaryPetId)로 매칭한다.
 * 대표 반려동물도 없으면 400으로 "먼저 선택해주세요" 안내(MatchingService.requirePetForMatch 참고).
 *
 * petIds(2026-09-19 추가, 다견 AND 판정)를 넘기면 petId 대신 그 목록을 쓴다 - 콤마로 구분한 값
 * (petIds=1,2,3)과 반복 파라미터(petIds=1&petIds=2) 둘 다 지원한다(Spring 기본 바인딩). 선택된
 * 반려동물 전부가 함께 이용 가능해야 "가능"이고, 하나라도 걸리면 그 반려동물 이름과 사유를 담아
 * 가장 제한적인 상태(불가 > 확인필요 > 조건부)를 반환한다. petIds와 petId를 둘 다 넘기면 petIds가 우선한다.
 */
@RestController
@RequiredArgsConstructor
public class MatchController {

    private final MatchingService matchingService;

    @GetMapping("/tours/{contentId}/match")
    public ApiResponse<MatchResponse> matchTour(
            @AuthenticationPrincipal Long userId, @PathVariable String contentId,
            @RequestParam(required = false) Long petId,
            @RequestParam(required = false) List<Long> petIds) {
        return ApiResponse.success(matchingService.matchTour(userId, contentId, resolvePetIds(petId, petIds)));
    }

    @GetMapping("/facilities/{id}/match")
    public ApiResponse<MatchResponse> matchFacility(
            @AuthenticationPrincipal Long userId, @PathVariable String id,
            @RequestParam(required = false) Long petId,
            @RequestParam(required = false) List<Long> petIds) {
        return ApiResponse.success(matchingService.matchFacility(userId, id, resolvePetIds(petId, petIds)));
    }

    private List<Long> resolvePetIds(Long petId, List<Long> petIds) {
        if (petIds != null && !petIds.isEmpty()) {
            return petIds;
        }
        return petId == null ? List.of() : List.of(petId);
    }
}

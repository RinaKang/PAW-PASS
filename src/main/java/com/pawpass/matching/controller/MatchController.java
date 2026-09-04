package com.pawpass.matching.controller;

import com.pawpass.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * API 명세서 - RQ-03 개인별 방문 가능 여부 매칭 (핵심 기능)
 * GET /tours/{contentId}/match?pet_id=
 * GET /facilities/{id}/match?pet_id=
 *
 * 응답: { status: 가능|조건부|불가|확인필요, reason, raw_text }
 *
 * TODO: AI 파싱 방식 미확정 (matching/ai/PetConditionAiParser.java에서 구현 예정)
 * TODO: tour는 매번 실시간으로 원문 받아서 그 자리에서 파싱, facility는 이미 파싱되어 저장된 값 사용
 */
@RestController
@RequiredArgsConstructor
public class MatchController {

    @GetMapping("/tours/{contentId}/match")
    public ApiResponse<Object> matchTour(@PathVariable String contentId, @RequestParam Long petId) {
        throw new UnsupportedOperationException("TODO: MatchingService + AI 파싱 로직 구현 필요");
    }

    @GetMapping("/facilities/{id}/match")
    public ApiResponse<Object> matchFacility(@PathVariable String id, @RequestParam Long petId) {
        throw new UnsupportedOperationException("TODO: MatchingService 구현 필요 (facility는 이미 파싱된 필드 사용)");
    }
}

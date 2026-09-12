package com.pawpass.route.dto;

import java.util.List;

/**
 * order: 총 이동거리가 최소가 되는 방문 순서(id 배열). 실제 도로 경로가 아니라 직선거리(하버사인)
 * 기준이다 - 카카오모빌리티 길찾기 API 같은 유료/승인 필요한 외부 API 없이 좌표만으로 계산한다
 * (사용자 결정, 2026-09-12: "그냥 동선 추천 정도만 해주고 싶은" 범위).
 */
public record RouteSuggestResponse(
        List<String> order,
        Double totalDistanceKm,
        List<Leg> legs
) {
    public record Leg(String fromId, String toId, Double distanceKm) {
    }
}

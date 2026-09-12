package com.pawpass.route.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 동선 추천 요청. 프론트가 /explore 등에서 이미 갖고 있는 id+좌표를 그대로 보내면 된다(재조회 불필요).
 * 지점 수 상한(8개)은 RouteService의 순열 전수조사 비용(N!) 때문 - 그 이상은 한 번에 고를 실익도 낮다.
 */
public record RouteSuggestRequest(
        @NotNull
        @Size(min = 2, max = 8, message = "동선을 추천하려면 2~8개의 지점이 필요합니다.")
        List<@Valid Point> points
) {
    public record Point(
            @NotBlank String id,
            @NotNull Double lat,
            @NotNull Double lng
    ) {
    }
}

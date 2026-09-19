package com.pawpass.route.dto;

import com.pawpass.global.util.DataSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * PUT /routes 배열의 항목 하나 - 별도 순번 필드는 없고 배열 안에서의 위치가 곧 동선 순서다.
 * 프론트가 /explore 등에서 이미 갖고 있는 값을 그대로 보내면 된다(재조회 불필요, RouteSuggestRequest와
 * 같은 설계 원칙).
 */
public record RoutePlaceRequest(
        @NotNull DataSource source,
        @NotBlank String contentId,
        @NotBlank String title,
        @NotNull Double lat,
        @NotNull Double lng
) {
}

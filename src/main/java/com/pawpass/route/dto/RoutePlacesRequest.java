package com.pawpass.route.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * PUT /routes의 요청 바디 - 배열을 그대로 최상위 바디로 받으면(List<@Valid X> request 파라미터)
 * 원소별 @Valid가 실제로 실행되지 않아(스프링이 List 타입 인자에 캐스케이드 검증을 안 태움, 라이브 테스트로
 * 확인 - 2026-09-19), lat 누락 같은 값이 검증 없이 그대로 DB까지 내려가 500(SQL NOT NULL 위반)이 났다.
 * 객체로 한 번 감싸서 @Valid가 확실히 작동하게 한다 - 필드 하나 잘못 보내면 400으로 명확히 알려준다.
 */
public record RoutePlacesRequest(
        @NotNull List<@Valid RoutePlaceRequest> places
) {
}

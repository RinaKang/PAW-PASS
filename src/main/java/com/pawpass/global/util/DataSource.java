package com.pawpass.global.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * favorites/trips 등에서 콘텐츠 출처를 구분하기 위한 enum.
 * TOURAPI: 한국관광공사 API (실시간 호출, content_id = 관광공사 contentid)
 * KCISA: 한국문화정보원 API (우리 DB pet_facilities에 미러링, content_id = pet_facilities.id)
 *
 * JSON 입출력은 소문자(tourapi/kcisa)로 통일한다 - /explore의 source 필드가 이미 소문자 문자열
 * ("tourapi"/"kcisa", ExploreItem 참고)이라 프론트가 그 값을 그대로 POST /favorites에 돌려보내는 게
 * 자연스러운 흐름인데, 이 enum이 대문자만 받으면 그 흐름이 그대로 깨진다(2026-09-13, 실제로 프론트가
 * "tourapi"를 보내서 500이 났던 걸로 발견됨). DB 저장(@Enumerated(EnumType.STRING))은 이 어노테이션과
 * 무관하게 그대로 enum 이름(TOURAPI/KCISA, 대문자)으로 저장된다.
 */
public enum DataSource {
    TOURAPI, KCISA;

    @JsonValue
    public String toJson() {
        return name().toLowerCase();
    }

    @JsonCreator
    public static DataSource fromJson(String value) {
        for (DataSource source : values()) {
            if (source.name().equalsIgnoreCase(value)) {
                return source;
            }
        }
        throw new IllegalArgumentException("알 수 없는 source 값입니다: " + value);
    }
}

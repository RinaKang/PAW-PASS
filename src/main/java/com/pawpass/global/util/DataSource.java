package com.pawpass.global.util;

/**
 * favorites/trips 등에서 콘텐츠 출처를 구분하기 위한 enum.
 * TOURAPI: 한국관광공사 API (실시간 호출, content_id = 관광공사 contentid)
 * KCISA: 한국문화정보원 API (우리 DB pet_facilities에 미러링, content_id = pet_facilities.id)
 */
public enum DataSource {
    TOURAPI, KCISA
}

package com.pawpass.facility.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

/**
 * Google Places API (New) - Place Details 응답 (X-Goog-FieldMask: photos 요청 시).
 * photos[].name은 구글 정책상 캐싱 금지 대상이라 이 응답 자체를 저장해선 안 되고, 매 요청마다 재조회해야 함.
 */
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public record GooglePlaceDetailsPhotosResponse(List<Photo> photos) {

    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Photo(String name, List<AuthorAttribution> authorAttributions) {
    }

    /**
     * 구글 정책상 사진을 노출하는 화면엔 이 저작자 표시를 같이 보여줘야 한다. uri는 기고자 프로필 링크
     * (사진 URL이 아님 - authorAttributions.photoUri는 기고자 아바타라 여기선 안 씀, 헷갈리기 쉬움).
     */
    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AuthorAttribution(String displayName, String uri) {
    }
}

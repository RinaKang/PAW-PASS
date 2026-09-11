package com.pawpass.facility.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

/**
 * Google Places API (New) - Text Search 응답 (필요한 필드만: places.id).
 * 전역 Jackson 설정(SNAKE_CASE)과 달리 이 API는 lowerCamelCase 그대로 와서 별도 네이밍 전략 지정 필요.
 */
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public record GooglePlacesTextSearchResponse(List<Place> places) {

    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Place(String id) {
    }
}

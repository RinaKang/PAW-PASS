package com.pawpass.facility.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * Google Places API (New) - Photo Media 응답 (?skipHttpRedirect=true 요청 시 실제 리다이렉트 대신 JSON으로 받음).
 * photoUri는 실제 이미지가 걸린 lh3.googleusercontent.com URL. 이 값도 캐싱 금지 대상.
 */
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public record GooglePlacePhotoMediaResponse(String name, String photoUri) {
}

package com.pawpass.tour.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * TourAPI detailImage2 응답의 item 하나 (관광지당 여러 장). 상세 화면엔 썸네일만 있으면 돼서
 * 원본 크기인 originimgurl은 안 받고 smallimageurl만 받는다.
 */
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public record TourDetailImageItem(String smallimageurl) {
}

package com.pawpass.tour.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * TourAPI areaBasedList2 응답의 item 하나.
 * 필드명이 TourAPI 원문 그대로라 LowerCamelCase(=변환 없음)로 고정하고,
 * 원문과 Java 필드명이 다른 것만 @JsonProperty로 명시.
 */
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public record TourAreaItem(
        String addr1,
        String addr2,
        @JsonProperty("contentid") String contentId,
        @JsonProperty("contenttypeid") String contentTypeId,
        @JsonProperty("createdtime") String createdTime,
        String firstimage,
        String firstimage2,
        String cpyrhtDivCd,
        String mapx,
        String mapy,
        String mlevel,
        @JsonProperty("modifiedtime") String modifiedTime,
        String tel,
        String title,
        String zipcode,
        String lDongRegnCd,
        String lDongSignguCd,
        String lclsSystm1,
        String lclsSystm2,
        String lclsSystm3,
        String cat3
) {
}

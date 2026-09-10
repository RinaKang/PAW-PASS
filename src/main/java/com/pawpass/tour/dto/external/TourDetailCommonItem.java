package com.pawpass.tour.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * TourAPI detailCommon2 응답의 item 하나.
 */
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public record TourDetailCommonItem(
        @JsonProperty("contentid") String contentId,
        @JsonProperty("contenttypeid") String contentTypeId,
        String title,
        String addr1,
        String addr2,
        String tel,
        String firstimage,
        String firstimage2,
        String overview,
        @JsonProperty("modifiedtime") String modifiedTime
) {
}

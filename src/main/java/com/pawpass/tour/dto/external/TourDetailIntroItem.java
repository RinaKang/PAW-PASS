package com.pawpass.tour.dto.external;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * TourAPI detailIntro2 응답의 item 하나.
 * 운영시간 관련 필드명이 contentTypeId별로 다르게 내려온다
 * (관광지 usetime, 문화시설 usetimeculture, 행사 usetimefestival, 레포츠 usetimeleports,
 *  음식점 opentimefood, 쇼핑 opentime 등) - JsonAlias로 하나의 필드에 모아 받는다.
 * 숙박(32)처럼 checkintime/checkouttime으로만 오는 타입은 대응 필드가 없어 hours가 null로 남는다.
 */
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public record TourDetailIntroItem(
        @JsonProperty("contentid") String contentId,
        @JsonAlias({"usetime", "usetimeculture", "usetimefestival", "usetimeleports", "opentimefood", "opentime"})
        String hours
) {
}

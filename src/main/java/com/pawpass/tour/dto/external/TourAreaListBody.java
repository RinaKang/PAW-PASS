package com.pawpass.tour.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public record TourAreaListBody(Items items, Integer numOfRows, Integer pageNo, Integer totalCount) {

    // TODO: 매뉴얼엔 명시 안 됐지만, TourAPI 계열은 결과 0건일 때 items가 "" (빈 문자열)로 오는 경우가 흔함.
    // 실제 서비스키로 빈 결과 케이스 테스트해보고 필요하면 커스텀 디시리얼라이저로 보완할 것.
    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Items(List<TourAreaItem> item) {
    }
}

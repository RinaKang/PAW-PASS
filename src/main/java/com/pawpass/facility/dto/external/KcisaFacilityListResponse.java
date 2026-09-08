package com.pawpass.facility.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

/**
 * odcloud.kr 게이트웨이 표준 응답 포맷 (TourAPI의 response.header/body 같은 중첩 구조가 아니라 평평한 구조).
 */
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public record KcisaFacilityListResponse(
        Integer page,
        Integer perPage,
        Integer totalCount,
        Integer currentCount,
        Integer matchCount,
        List<KcisaFacilityItem> data
) {
}

package com.pawpass.tour.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * TourAPI(KorPetTourService2) detailPetTour2 응답의 item 하나.
 * 반려동물 동반 조건은 구조화된 필드가 아니라 자유서술 문장으로 내려온다 (README 참고 - AI 파싱 대상).
 */
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public record TourDetailPetTourItem(
        @JsonProperty("contentid") String contentId,
        String acmpyTypeCd,
        String acmpyPsblCpam,
        String acmpyNeedMtr,
        String etcAcmpyInfo
) {
}

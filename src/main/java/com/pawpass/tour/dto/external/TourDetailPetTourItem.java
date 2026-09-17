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
        String etcAcmpyInfo,
        // 2026-09-15 추가 - 원래도 TourAPI 응답엔 있었지만 이 DTO에 필드가 없어서
        // @JsonIgnoreProperties(ignoreUnknown=true)에 의해 조용히 버려지고 있었음(프론트 리포트로 발견).
        String relaPosesFclty,   // 관련 구비 시설
        String relaFrnshPrdlst,  // 관련 비치 품목
        String relaPurcPrdlst,   // 관련 구매 품목
        String relaRntlPrdlst    // 관련 렌탈 품목
) {
}

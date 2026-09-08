package com.pawpass.facility.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 한국문화정보원_전국 반려동물 동반 가능 문화시설 위치 데이터 응답 item 하나.
 * 원본 필드명이 한글이라 Jackson naming 전략을 탈 수 없어서 전부 @JsonProperty로 명시.
 * 출처: 공공데이터포털(odcloud.kr) Swagger 문서, uddi:41944402-8249-4e45-9e9d-a52d0a7db1cc
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KcisaFacilityItem(
        @JsonProperty("시설명") String title,
        @JsonProperty("카테고리1") String category1,
        @JsonProperty("카테고리2") String category2,
        @JsonProperty("카테고리3") String category3,
        @JsonProperty("시도 명칭") String regionName,
        @JsonProperty("시군구 명칭") String sigunguName,
        @JsonProperty("법정읍면동명칭") String eupmyeondongName,
        @JsonProperty("리 명칭") String riName,
        @JsonProperty("번지") String lotNumber,
        @JsonProperty("도로명 이름") String roadName,
        @JsonProperty("건물 번호") String buildingNumber,
        @JsonProperty("위도") String lat,
        @JsonProperty("경도") String lng,
        @JsonProperty("우편번호") Integer zipcode,
        @JsonProperty("도로명주소") String roadAddress,
        @JsonProperty("지번주소") String lotAddress,
        @JsonProperty("전화번호") String tel,
        @JsonProperty("홈페이지") String homepage,
        @JsonProperty("휴무일") String closedDays,
        @JsonProperty("운영시간") String operatingHours,
        @JsonProperty("주차 가능여부") String parkingAvailable,
        @JsonProperty("입장(이용료)가격 정보") String charge,
        @JsonProperty("반려동물 동반 가능정보") String petAllowed,
        @JsonProperty("반려동물 전용 정보") String petExclusive,
        @JsonProperty("입장 가능 동물 크기") String allowedPetSize,
        @JsonProperty("반려동물 제한사항") String petRestriction,
        @JsonProperty("장소(실내) 여부") String indoor,
        @JsonProperty("장소(실외)여부") String outdoor,
        @JsonProperty("기본 정보_장소설명") String description,
        @JsonProperty("애견 동반 추가 요금") String additionalPetFee,
        @JsonProperty("최종작성일") String issuedDate
) {
}

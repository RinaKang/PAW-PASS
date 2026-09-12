package com.pawpass.explore.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * /explore가 받는 공통 검색값(지역명/카테고리)을 tour(TourAPI)와 facility(KCISA) 각자의 실제 규격으로
 * 바꿔주는 어댑터. GET /tours, GET /facilities 엔드포인트 자체의 파라미터 규격은 그대로 두고,
 * 변환은 전부 이 클래스(및 ExploreService의 호출부)에서만 한다.
 *

 * - 지역: SearchPage.jsx 드롭다운 하드코딩 값 - 강릉/서울/제주
 * - 카테고리: mocks/tourist-spots.json의 category 값 - NATURE/CAFE/CULTURE/STAY
 * 새 지역/카테고리가 프론트에 추가되면 아래 두 맵에 항목만 추가하면 된다.
 */
final class ExploreConditionMapper {

    private ExploreConditionMapper() {
    }

    // ===== 지역 =====

    record TourRegion(String lDongRegnCd, String lDongSignguCd) {
    }

    // 시/도 17개 전체(법정동 코드 기준 - 구 areaCode 아님, 2026-09-10 실측 확인: lDongRegnCd=11은 서울
    // 결과가 나오지만 구코드인 lDongRegnCd=1은 0건) + 프론트가 시/군 단위로 쓰는 "강릉" 특례.
    private static final Map<String, TourRegion> TOUR_REGION_MAP = Map.ofEntries(
            Map.entry("서울", new TourRegion("11", null)),
            Map.entry("부산", new TourRegion("26", null)),
            Map.entry("대구", new TourRegion("27", null)),
            Map.entry("인천", new TourRegion("28", null)),
            Map.entry("광주", new TourRegion("29", null)),
            Map.entry("대전", new TourRegion("30", null)),
            Map.entry("울산", new TourRegion("31", null)),
            Map.entry("세종", new TourRegion("36", null)),
            Map.entry("경기", new TourRegion("41", null)),
            Map.entry("강원", new TourRegion("51", null)),
            Map.entry("충북", new TourRegion("43", null)),
            Map.entry("충남", new TourRegion("44", null)),
            Map.entry("전북", new TourRegion("52", null)),
            Map.entry("전남", new TourRegion("46", null)),
            Map.entry("경북", new TourRegion("47", null)),
            Map.entry("경남", new TourRegion("48", null)),
            Map.entry("제주", new TourRegion("50", null)),
            // 강원도 강릉시 - 프론트 드롭다운이 시/도가 아니라 시/군 단위로 씀. lDongRegnCd=51+lDongSignguCd=150
            Map.entry("강릉", new TourRegion("51", "150"))
    );

    /** tour(TourAPI) 쪽 지역 파라미터. 매핑 테이블에 없는 지역명이면 필터 없이(Optional.empty) 전체 조회로 대체한다. */
    static Optional<TourRegion> toTourRegion(String commonRegion) {
        if (commonRegion == null || commonRegion.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(TOUR_REGION_MAP.get(commonRegion.trim()));
    }

    /** facility(KCISA)는 지역 코드 체계가 없어 원문 지역명을 그대로 주소 부분일치 키워드로 쓴다 (매핑 불필요). */
    static String toFacilityRegionKeyword(String commonRegion) {
        return (commonRegion == null || commonRegion.isBlank()) ? null : commonRegion.trim();
    }

    // ===== 카테고리 =====

    /**
     * cat1/cat2/cat3는 CAFE처럼 contentTypeId만으로 안 갈라지는 세분류를 "포함"시킬 때만 채움(그 외엔 null).
     * excludedTourCat3는 반대로 FOOD처럼 특정 cat3 값을 "제외"시킬 때만 채움(TourAPI가 제외 필터 자체를
     * 지원 안 해서, 서버가 응답을 받은 뒤 이 값과 일치하는 항목을 걸러내는 용도 - ExploreService 참고).
     */
    private record CategoryMapping(String tourContentTypeId, String tourCat1, String tourCat2, String tourCat3,
                                     String excludedTourCat3, List<String> facilityCategory3Values) {
        private CategoryMapping(String tourContentTypeId, List<String> facilityCategory3Values) {
            this(tourContentTypeId, null, null, null, null, facilityCategory3Values);
        }

        private CategoryMapping(String tourContentTypeId, String tourCat1, String tourCat2, String tourCat3,
                                 List<String> facilityCategory3Values) {
            this(tourContentTypeId, tourCat1, tourCat2, tourCat3, null, facilityCategory3Values);
        }

        private CategoryMapping(String tourContentTypeId, List<String> facilityCategory3Values, String excludedTourCat3) {
            this(tourContentTypeId, null, null, null, excludedTourCat3, facilityCategory3Values);
        }
    }

    // 프론트 목업 카테고리(NATURE/CAFE/CULTURE/STAY) 기준 + 사용자 요청으로 추가한 FOOD(음식점).
    // 1:N인 것(문화시설→박물관/미술관/문예회관, 숙박→펜션/호텔)은 실제 pet_facilities.category3 데이터를
    // 조회해서 채웠다.
    // CAFE/FOOD 둘 다 TourAPI contentTypeId=39(음식점) 안에 있는데, contentTypeId만으로는 카페와 일반
    // 음식점이 안 갈라져서(구 분류체계 cat1/cat2/cat3 실측 결과: lclsSystm 신 분류체계는 카페/일반식당이
    // 섞여서 나와 구분이 안 됨, 2026-09-12) CAFE만 cat1=A05/cat2=A0502/cat3=A05020900(카페/전통찻집)로
    // 정밀 필터링한다(실측 확인: 해당 코드로 필터링 시 29건 전부 카페·디저트류, live-verified).
    // FOOD 쪽은 TourAPI가 "~카페 아닌 것만" 식의 서버단 제외 필터를 지원하지 않아서, cat3="A05020900"
    // (카페)으로 명시 태그된 항목만 응답을 받은 뒤 ExploreService에서 걸러낸다(CAFE_TOUR_CAT3 상수 재사용).
    // 2026-09-12 실사용 테스트에서 FOOD 결과 대부분이 카페였던 걸 이걸로 고쳤다 - 다만 cat3 태그가 아예
    // 없는 항목(전국 72건 중 23건, 실측)까지는 걸러내지 못한다(그중 일부는 실제로도 카페) - TourAPI
    // 원본 데이터 자체의 한계라 서버에서 더 할 수 있는 게 없음, 알려진 잔여 한계로 남겨둠.
    // facility 쪽은 pet_facilities.category3="식당"(카페와 완전히 분리된 값, 실측 확인)이라 겹침 없음.
    private static final String CAFE_TOUR_CAT3 = "A05020900";

    private static final Map<String, CategoryMapping> CATEGORY_MAP = Map.of(
            "NATURE", new CategoryMapping("12", List.of("여행지")),
            "CAFE", new CategoryMapping("39", "A05", "A0502", CAFE_TOUR_CAT3, List.of("카페")),
            "FOOD", new CategoryMapping("39", List.of("식당"), CAFE_TOUR_CAT3),
            "CULTURE", new CategoryMapping("14", List.of("박물관", "미술관", "문예회관")),
            "STAY", new CategoryMapping("32", List.of("펜션", "호텔"))
    );

    /** 매핑 테이블에 없는 카테고리 값이면 필터 없이(Optional.empty) 전체 조회로 대체한다 - 값 하나 잘못 왔다고 막지 않음. */
    static Optional<String> toTourContentTypeId(String commonCategory) {
        return lookup(commonCategory).map(CategoryMapping::tourContentTypeId);
    }

    /** cat1/cat2/cat3 세분류가 없는 카테고리(NATURE/FOOD/CULTURE/STAY 등)는 전부 null - contentTypeId만으로 충분. */
    static String toTourCat1(String commonCategory) {
        return lookup(commonCategory).map(CategoryMapping::tourCat1).orElse(null);
    }

    static String toTourCat2(String commonCategory) {
        return lookup(commonCategory).map(CategoryMapping::tourCat2).orElse(null);
    }

    static String toTourCat3(String commonCategory) {
        return lookup(commonCategory).map(CategoryMapping::tourCat3).orElse(null);
    }

    /** 이 값과 cat3가 일치하는 tour 항목은 응답에서 제외해야 한다(FOOD만 해당, 그 외엔 전부 null). */
    static String toExcludedTourCat3(String commonCategory) {
        return lookup(commonCategory).map(CategoryMapping::excludedTourCat3).orElse(null);
    }

    /** 1:N이라 리스트로 반환 - 비어있으면(매핑 없음/카테고리 미지정) facility 쪽도 필터 없이 전체 조회. */
    static List<String> toFacilityCategory3Values(String commonCategory) {
        return lookup(commonCategory).map(CategoryMapping::facilityCategory3Values).orElse(List.of());
    }

    private static Optional<CategoryMapping> lookup(String commonCategory) {
        if (commonCategory == null || commonCategory.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(CATEGORY_MAP.get(commonCategory.trim().toUpperCase()));
    }
}

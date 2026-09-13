package com.pawpass.explore.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ExploreConditionMapperTest {

    @Test
    void 서울은_시도코드_11로_매핑된다() {
        Optional<ExploreConditionMapper.TourRegion> region = ExploreConditionMapper.toTourRegion("서울");

        assertThat(region).isPresent();
        assertThat(region.get().lDongRegnCd()).isEqualTo("11");
        assertThat(region.get().lDongSignguCd()).isNull();
    }

    @Test
    void 강릉은_강원_시도코드와_시군구코드_조합으로_매핑된다() {
        Optional<ExploreConditionMapper.TourRegion> region = ExploreConditionMapper.toTourRegion("강릉");

        assertThat(region).isPresent();
        assertThat(region.get().lDongRegnCd()).isEqualTo("51");
        assertThat(region.get().lDongSignguCd()).isEqualTo("150");
    }

    @Test
    void 매핑에_없는_지역명은_비어있다() {
        assertThat(ExploreConditionMapper.toTourRegion("속초")).isEmpty();
    }

    @Test
    void 지역명이_없으면_비어있다() {
        assertThat(ExploreConditionMapper.toTourRegion(null)).isEmpty();
        assertThat(ExploreConditionMapper.toTourRegion("")).isEmpty();
        assertThat(ExploreConditionMapper.toTourRegion("  ")).isEmpty();
    }

    @Test
    void facility_지역_키워드는_원문_그대로_트림만_해서_돌려준다() {
        assertThat(ExploreConditionMapper.toFacilityRegionKeyword(" 서울 ")).isEqualTo("서울");
        assertThat(ExploreConditionMapper.toFacilityRegionKeyword(null)).isNull();
        assertThat(ExploreConditionMapper.toFacilityRegionKeyword("")).isNull();
    }

    @Test
    void NATURE는_tour_12_facility_여행지로_매핑된다() {
        assertThat(ExploreConditionMapper.toTourContentTypeId("NATURE")).contains("12");
        assertThat(ExploreConditionMapper.toFacilityCategory3Values("NATURE")).containsExactly("여행지");
    }

    @Test
    void CAFE는_tour_39_facility_카페로_매핑되고_카페_세분류_필터까지_같이_붙는다() {
        assertThat(ExploreConditionMapper.toTourContentTypeId("CAFE")).contains("39");
        assertThat(ExploreConditionMapper.toFacilityCategory3Values("CAFE")).containsExactly("카페");
        assertThat(ExploreConditionMapper.toTourCat1("CAFE")).isEqualTo("A05");
        assertThat(ExploreConditionMapper.toTourCat2("CAFE")).isEqualTo("A0502");
        assertThat(ExploreConditionMapper.toTourCat3("CAFE")).isEqualTo("A05020900");
    }

    @Test
    void FOOD는_tour_39_facility_식당으로_매핑되고_카페와_달리_포함_필터_대신_제외_필터를_쓴다() {
        assertThat(ExploreConditionMapper.toTourContentTypeId("FOOD")).contains("39");
        assertThat(ExploreConditionMapper.toFacilityCategory3Values("FOOD")).containsExactly("식당");
        assertThat(ExploreConditionMapper.toTourCat1("FOOD")).isNull();
        assertThat(ExploreConditionMapper.toTourCat2("FOOD")).isNull();
        assertThat(ExploreConditionMapper.toTourCat3("FOOD")).isNull();
        assertThat(ExploreConditionMapper.toExcludedTourCat3("FOOD")).isEqualTo("A05020900");
    }

    @Test
    void CAFE는_포함_필터를_쓰므로_제외_필터는_없다() {
        assertThat(ExploreConditionMapper.toExcludedTourCat3("CAFE")).isNull();
        assertThat(ExploreConditionMapper.shouldExcludeCafeLikeNames("CAFE")).isFalse();
    }

    // 2026-09-13: cat3 태그가 아예 없는 카페(전국 72건 중 23건, 실측)를 이름 키워드로 추가 필터링.
    @Test
    void FOOD는_이름_기반_카페_필터링도_적용한다() {
        assertThat(ExploreConditionMapper.shouldExcludeCafeLikeNames("FOOD")).isTrue();
        assertThat(ExploreConditionMapper.shouldExcludeCafeLikeNames("NATURE")).isFalse();
        assertThat(ExploreConditionMapper.shouldExcludeCafeLikeNames("CULTURE")).isFalse();
        assertThat(ExploreConditionMapper.shouldExcludeCafeLikeNames("STAY")).isFalse();
    }

    @Test
    void 이름에_카페_관련_키워드가_있으면_카페로_인식한다() {
        assertThat(ExploreConditionMapper.looksLikeCafeByName("누닝 펫푸드카페")).isTrue();
        assertThat(ExploreConditionMapper.looksLikeCafeByName("맥파이앤타이거 성수티룸")).isTrue();
        assertThat(ExploreConditionMapper.looksLikeCafeByName("도깨비젤라또")).isTrue();
        assertThat(ExploreConditionMapper.looksLikeCafeByName("Blue Bottle Coffee")).isTrue();
        assertThat(ExploreConditionMapper.looksLikeCafeByName("강릉 한우타운")).isFalse();
        assertThat(ExploreConditionMapper.looksLikeCafeByName(null)).isFalse();
    }

    @Test
    void 제외_필터가_없는_카테고리는_전부_null이다() {
        assertThat(ExploreConditionMapper.toExcludedTourCat3("NATURE")).isNull();
        assertThat(ExploreConditionMapper.toExcludedTourCat3("CULTURE")).isNull();
        assertThat(ExploreConditionMapper.toExcludedTourCat3("STAY")).isNull();
        assertThat(ExploreConditionMapper.toExcludedTourCat3("UNKNOWN")).isNull();
        assertThat(ExploreConditionMapper.toExcludedTourCat3(null)).isNull();
    }

    @Test
    void 세분류_필터가_없는_카테고리는_cat1_2_3이_전부_null이다() {
        assertThat(ExploreConditionMapper.toTourCat1("NATURE")).isNull();
        assertThat(ExploreConditionMapper.toTourCat1("CULTURE")).isNull();
        assertThat(ExploreConditionMapper.toTourCat1("STAY")).isNull();
        assertThat(ExploreConditionMapper.toTourCat1("UNKNOWN")).isNull();
        assertThat(ExploreConditionMapper.toTourCat1(null)).isNull();
    }

    @Test
    void CULTURE는_tour_14_facility_박물관_미술관_문예회관_세_개로_매핑된다() {
        assertThat(ExploreConditionMapper.toTourContentTypeId("CULTURE")).contains("14");
        assertThat(ExploreConditionMapper.toFacilityCategory3Values("CULTURE"))
                .containsExactlyInAnyOrder("박물관", "미술관", "문예회관");
    }

    @Test
    void STAY는_tour_32_facility_펜션_호텔_두_개로_매핑된다() {
        assertThat(ExploreConditionMapper.toTourContentTypeId("STAY")).contains("32");
        assertThat(ExploreConditionMapper.toFacilityCategory3Values("STAY"))
                .containsExactlyInAnyOrder("펜션", "호텔");
    }

    // 2026-09-13: TourAPI엔 "동물병원" 개념 자체가 없어서 facility(KCISA)만 있는 카테고리로 추가.
    @Test
    void HOSPITAL은_facility_동물병원으로만_매핑되고_tour_조회_자체를_건너뛴다() {
        assertThat(ExploreConditionMapper.toFacilityCategory3Values("HOSPITAL")).containsExactly("동물병원");
        assertThat(ExploreConditionMapper.toTourContentTypeId("HOSPITAL")).isEmpty();
        assertThat(ExploreConditionMapper.isFacilityOnly("HOSPITAL")).isTrue();
    }

    @Test
    void facilityOnly가_아닌_카테고리는_전부_false다() {
        assertThat(ExploreConditionMapper.isFacilityOnly("NATURE")).isFalse();
        assertThat(ExploreConditionMapper.isFacilityOnly("CAFE")).isFalse();
        assertThat(ExploreConditionMapper.isFacilityOnly("FOOD")).isFalse();
        assertThat(ExploreConditionMapper.isFacilityOnly("CULTURE")).isFalse();
        assertThat(ExploreConditionMapper.isFacilityOnly("STAY")).isFalse();
        assertThat(ExploreConditionMapper.isFacilityOnly("UNKNOWN")).isFalse();
        assertThat(ExploreConditionMapper.isFacilityOnly(null)).isFalse();
    }

    @Test
    void 소문자로_와도_대소문자_구분없이_매핑된다() {
        assertThat(ExploreConditionMapper.toTourContentTypeId("culture")).contains("14");
    }

    @Test
    void 매핑에_없는_카테고리는_둘_다_비어있다() {
        assertThat(ExploreConditionMapper.toTourContentTypeId("UNKNOWN")).isEmpty();
        assertThat(ExploreConditionMapper.toFacilityCategory3Values("UNKNOWN")).isEqualTo(List.of());
    }

    @Test
    void 카테고리가_없으면_둘_다_비어있다() {
        assertThat(ExploreConditionMapper.toTourContentTypeId(null)).isEmpty();
        assertThat(ExploreConditionMapper.toFacilityCategory3Values(null)).isEqualTo(List.of());
    }
}

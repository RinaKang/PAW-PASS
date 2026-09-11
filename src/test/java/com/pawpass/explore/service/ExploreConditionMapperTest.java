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
    void FOOD는_tour_39_facility_식당으로_매핑되고_카페와_달리_세분류_필터가_없다() {
        assertThat(ExploreConditionMapper.toTourContentTypeId("FOOD")).contains("39");
        assertThat(ExploreConditionMapper.toFacilityCategory3Values("FOOD")).containsExactly("식당");
        assertThat(ExploreConditionMapper.toTourCat1("FOOD")).isNull();
        assertThat(ExploreConditionMapper.toTourCat2("FOOD")).isNull();
        assertThat(ExploreConditionMapper.toTourCat3("FOOD")).isNull();
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

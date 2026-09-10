package com.pawpass.explore.service;

import com.pawpass.explore.dto.ExploreItem;
import com.pawpass.facility.dto.FacilitySummaryResponse;
import com.pawpass.facility.service.FacilityService;
import com.pawpass.matching.dto.MatchResponse;
import com.pawpass.matching.service.MatchingService;
import com.pawpass.pet.domain.Pet;
import com.pawpass.pet.domain.PetSize;
import com.pawpass.tour.dto.TourSummaryResponse;
import com.pawpass.tour.service.TourService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExploreServiceTest {

    @Mock
    private TourService tourService;

    @Mock
    private FacilityService facilityService;

    @Mock
    private MatchingService matchingService;

    @InjectMocks
    private ExploreService exploreService;

    private static final Pet PET = Pet.builder()
            .userId(1L).name("초코").species("강아지").breed("말티즈").weight(3.0).size(PetSize.SMALL)
            .hasCarrier(true).hasLeash(true).build();

    @Test
    void 좌표가_50m_이내면_제목이_달라도_같은_장소로_보고_tourapi가_우선한다() {
        // 서울시청 근처 좌표, 두 번째 좌표는 약 30m 정도만 떨어져 있음
        TourSummaryResponse tour = new TourSummaryResponse("t1", "행복 펜션", "서울시", "tel", "img", 126.9780, 37.5665);
        FacilitySummaryResponse duplicate = new FacilitySummaryResponse("f1", "다른이름 카페", "서울시 다른표기", "tel2", 37.5667, 126.9782);
        FacilitySummaryResponse unique = new FacilitySummaryResponse("f2", "먼 장소", "부산시", "tel3", 35.1796, 129.0756);

        when(tourService.search(null, null, null, 1)).thenReturn(List.of(tour));
        when(facilityService.search(null, null, 1)).thenReturn(List.of(duplicate, unique));

        List<ExploreItem> result = exploreService.explore(1L, null, null, null, null, 1);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ExploreItem::id).containsExactly("t1", "f2");
        assertThat(result).extracting(ExploreItem::source).containsExactly("tourapi", "kcisa");
    }

    @Test
    void 좌표가_충분히_멀면_같은_제목이어도_둘_다_남는다() {
        TourSummaryResponse tour = new TourSummaryResponse("t1", "행복 펜션", "서울시", "tel", "img", 126.9780, 37.5665);
        FacilitySummaryResponse farAway = new FacilitySummaryResponse("f1", "행복 펜션", "서울시", "tel2", 35.1796, 129.0756);

        when(tourService.search(null, null, null, 1)).thenReturn(List.of(tour));
        when(facilityService.search(null, null, 1)).thenReturn(List.of(farAway));

        List<ExploreItem> result = exploreService.explore(1L, null, null, null, null, 1);

        assertThat(result).hasSize(2);
    }

    @Test
    void 좌표가_없으면_제목_정규화로_폴백한다() {
        TourSummaryResponse tour = new TourSummaryResponse("t1", "행복 펜션", "서울시", "tel", "img", null, null);
        FacilitySummaryResponse duplicate = new FacilitySummaryResponse("f1", "행복 펜션", "서울시 다른표기", "tel2", null, null);

        when(tourService.search(null, null, null, 1)).thenReturn(List.of(tour));
        when(facilityService.search(null, null, 1)).thenReturn(List.of(duplicate));

        List<ExploreItem> result = exploreService.explore(1L, null, null, null, null, 1);

        assertThat(result).hasSize(1);
        assertThat(result).extracting(ExploreItem::source).containsExactly("tourapi");
    }

    @Test
    void matchStatus_필터가_기본값과_일치하지_않으면_빈_목록() {
        TourSummaryResponse tour = new TourSummaryResponse("t1", "제목", "주소", "tel", "img", 1.0, 1.0);
        when(tourService.search(null, null, null, 1)).thenReturn(List.of(tour));
        when(facilityService.search(null, null, 1)).thenReturn(List.of());

        List<ExploreItem> result = exploreService.explore(1L, null, null, "가능", null, 1);

        assertThat(result).isEmpty();
    }

    @Test
    void matchStatus가_확인필요면_전부_반환() {
        TourSummaryResponse tour = new TourSummaryResponse("t1", "제목", "주소", "tel", "img", 1.0, 1.0);
        when(tourService.search(null, null, null, 1)).thenReturn(List.of(tour));
        when(facilityService.search(null, null, 1)).thenReturn(List.of());

        List<ExploreItem> result = exploreService.explore(1L, null, null, "확인필요", null, 1);

        assertThat(result).hasSize(1);
    }

    @Test
    void petId가_있으면_항목마다_실제_매칭을_계산한다() {
        TourSummaryResponse tour = new TourSummaryResponse("t1", "제목", "주소", "tel", "img", 1.0, 1.0);
        FacilitySummaryResponse facility = new FacilitySummaryResponse("f1", "다른 장소", "부산시", "tel3", 35.0, 129.0);

        when(tourService.search(null, null, null, 1)).thenReturn(List.of(tour));
        when(facilityService.search(null, null, 1)).thenReturn(List.of(facility));
        when(matchingService.requireOwnedPet(1L, 9L)).thenReturn(PET);
        when(matchingService.matchTourForPet(PET, "t1"))
                .thenReturn(new MatchResponse(MatchResponse.STATUS_ALLOWED, "가능합니다", "raw"));
        when(matchingService.matchFacilityForPet(PET, "f1"))
                .thenReturn(new MatchResponse(MatchResponse.STATUS_DENIED, "불가합니다", "raw"));

        // matchStatus를 명시하면 기본 필터(가능/조건부만) 대신 그 값으로 필터링한다 - "불가"도 명시적으로 요청하면 보임
        List<ExploreItem> explicit = exploreService.explore(1L, null, null, "불가", 9L, 1);
        assertThat(explicit).extracting(ExploreItem::id, ExploreItem::matchStatus)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("f1", MatchResponse.STATUS_DENIED));
    }

    // 기능 명세서 4.2.1: petId는 있는데 matchStatus를 명시하지 않은 "기본" 요청은 가능/조건부만 노출해야 한다.
    @Test
    void petId만_있고_matchStatus가_없으면_가능_조건부만_기본_노출된다() {
        TourSummaryResponse allowed = new TourSummaryResponse("t1", "가능한곳", "주소1", "tel", "img", 1.0, 1.0);
        TourSummaryResponse conditional = new TourSummaryResponse("t2", "조건부인곳", "주소2", "tel", "img", 2.0, 2.0);
        TourSummaryResponse denied = new TourSummaryResponse("t3", "불가인곳", "주소3", "tel", "img", 3.0, 3.0);
        TourSummaryResponse unknown = new TourSummaryResponse("t4", "확인필요인곳", "주소4", "tel", "img", 4.0, 4.0);

        when(tourService.search(null, null, null, 1)).thenReturn(List.of(allowed, conditional, denied, unknown));
        when(facilityService.search(null, null, 1)).thenReturn(List.of());
        when(matchingService.requireOwnedPet(1L, 9L)).thenReturn(PET);
        when(matchingService.matchTourForPet(PET, "t1"))
                .thenReturn(new MatchResponse(MatchResponse.STATUS_ALLOWED, "", "raw"));
        when(matchingService.matchTourForPet(PET, "t2"))
                .thenReturn(new MatchResponse(MatchResponse.STATUS_CONDITIONAL, "", "raw"));
        when(matchingService.matchTourForPet(PET, "t3"))
                .thenReturn(new MatchResponse(MatchResponse.STATUS_DENIED, "", "raw"));
        when(matchingService.matchTourForPet(PET, "t4"))
                .thenReturn(new MatchResponse(MatchResponse.STATUS_UNKNOWN, "", "raw"));

        List<ExploreItem> result = exploreService.explore(1L, null, null, null, 9L, 1);

        assertThat(result).extracting(ExploreItem::id)
                .containsExactlyInAnyOrder("t1", "t2");
    }

    // petId가 없을 때(반려동물 미등록 사용자 등)는 전부 확인필요라 기본 필터를 적용하면 항상 빈 목록이 되므로
    // 적용하지 않는다 - 개인화가 안 될 때는 전체 목록을 그대로 보여준다.
    @Test
    void petId가_없으면_기본_필터를_적용하지_않고_전부_반환한다() {
        TourSummaryResponse tour = new TourSummaryResponse("t1", "제목", "주소", "tel", "img", 1.0, 1.0);
        when(tourService.search(null, null, null, 1)).thenReturn(List.of(tour));
        when(facilityService.search(null, null, 1)).thenReturn(List.of());

        List<ExploreItem> result = exploreService.explore(1L, null, null, null, null, 1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).matchStatus()).isEqualTo("확인필요");
    }

    @Test
    void 항목_하나의_매칭_계산이_실패해도_나머지는_정상_반환된다() {
        TourSummaryResponse tourOk = new TourSummaryResponse("t1", "제목1", "주소", "tel", "img", 1.0, 1.0);
        TourSummaryResponse tourFails = new TourSummaryResponse("t2", "제목2", "주소2", "tel", "img", 2.0, 2.0);

        when(tourService.search(null, null, null, 1)).thenReturn(List.of(tourOk, tourFails));
        when(facilityService.search(null, null, 1)).thenReturn(List.of());
        when(matchingService.requireOwnedPet(1L, 9L)).thenReturn(PET);
        when(matchingService.matchTourForPet(PET, "t1"))
                .thenReturn(new MatchResponse(MatchResponse.STATUS_ALLOWED, "가능합니다", "raw"));
        when(matchingService.matchTourForPet(PET, "t2"))
                .thenThrow(new RuntimeException("Gemini 429 등 외부 호출 실패 시뮬레이션"));

        // 기본 필터: 실패해서 확인필요로 대체된 t2는 안 보이고, 정상 계산된 t1(가능)만 보인다
        List<ExploreItem> defaultView = exploreService.explore(1L, null, null, null, 9L, 1);
        assertThat(defaultView).extracting(ExploreItem::id, ExploreItem::matchStatus)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("t1", MatchResponse.STATUS_ALLOWED));

        // matchStatus=확인필요로 명시적으로 요청하면 실패 대체된 t2도 조회 가능
        List<ExploreItem> unknownView = exploreService.explore(1L, null, null, "확인필요", 9L, 1);
        assertThat(unknownView).extracting(ExploreItem::id, ExploreItem::matchStatus)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("t2", "확인필요"));
    }

    // ===== 공통 region/category 값을 tour/facility 각자의 규격으로 변환하는지 =====

    @Test
    void 지역명이_매핑되면_tour는_법정동코드로_facility는_원문_그대로_조회한다() {
        when(tourService.search("11", null, null, 1)).thenReturn(List.of());
        when(facilityService.search("서울", null, 1)).thenReturn(List.of());

        exploreService.explore(1L, "서울", null, null, null, 1);

        verify(tourService).search("11", null, null, 1);
        verify(facilityService).search("서울", null, 1);
    }

    @Test
    void 시군_단위_지역명은_시도코드_시군구코드_조합으로_변환된다() {
        when(tourService.search("51", "150", null, 1)).thenReturn(List.of());
        when(facilityService.search("강릉", null, 1)).thenReturn(List.of());

        exploreService.explore(1L, "강릉", null, null, null, 1);

        verify(tourService).search("51", "150", null, 1);
        verify(facilityService).search("강릉", null, 1);
    }

    @Test
    void 매핑_테이블에_없는_지역명은_tour는_필터없이_facility는_원문_키워드로_조회한다() {
        when(tourService.search(null, null, null, 1)).thenReturn(List.of());
        when(facilityService.search("속초", null, 1)).thenReturn(List.of());

        exploreService.explore(1L, "속초", null, null, null, 1);

        verify(tourService).search(null, null, null, 1);
        verify(facilityService).search("속초", null, 1);
    }

    @Test
    void CULTURE_카테고리는_tour_문화시설코드와_facility_박물관_미술관_문예회관을_합쳐_조회한다() {
        FacilitySummaryResponse museum = new FacilitySummaryResponse("m1", "박물관1", "주소", "tel", 1.0, 1.0);
        FacilitySummaryResponse gallery = new FacilitySummaryResponse("g1", "미술관1", "주소", "tel", 2.0, 2.0);
        FacilitySummaryResponse hall = new FacilitySummaryResponse("h1", "문예회관1", "주소", "tel", 3.0, 3.0);

        when(tourService.search(null, null, "14", 1)).thenReturn(List.of());
        when(facilityService.search(null, "박물관", 1)).thenReturn(List.of(museum));
        when(facilityService.search(null, "미술관", 1)).thenReturn(List.of(gallery));
        when(facilityService.search(null, "문예회관", 1)).thenReturn(List.of(hall));

        List<ExploreItem> result = exploreService.explore(1L, null, "CULTURE", null, null, 1);

        verify(tourService).search(null, null, "14", 1);
        assertThat(result).extracting(ExploreItem::id).containsExactlyInAnyOrder("m1", "g1", "h1");
    }

    @Test
    void STAY_카테고리는_tour_숙박코드와_facility_펜션_호텔을_합쳐_조회한다() {
        FacilitySummaryResponse pension = new FacilitySummaryResponse("p1", "펜션1", "주소", "tel", 1.0, 1.0);
        FacilitySummaryResponse hotel = new FacilitySummaryResponse("h1", "호텔1", "주소", "tel", 2.0, 2.0);

        when(tourService.search(null, null, "32", 1)).thenReturn(List.of());
        when(facilityService.search(null, "펜션", 1)).thenReturn(List.of(pension));
        when(facilityService.search(null, "호텔", 1)).thenReturn(List.of(hotel));

        List<ExploreItem> result = exploreService.explore(1L, null, "STAY", null, null, 1);

        verify(tourService).search(null, null, "32", 1);
        assertThat(result).extracting(ExploreItem::id).containsExactlyInAnyOrder("p1", "h1");
    }

    @Test
    void 매핑_테이블에_없는_카테고리는_tour_facility_둘_다_필터없이_조회한다() {
        when(tourService.search(null, null, null, 1)).thenReturn(List.of());
        when(facilityService.search(null, null, 1)).thenReturn(List.of());

        exploreService.explore(1L, null, "UNKNOWN_CATEGORY", null, null, 1);

        verify(tourService).search(null, null, null, 1);
        verify(facilityService).search(null, null, 1);
    }

    @Test
    void 지역_카테고리_둘_다_생략하면_양쪽_다_필터없이_전체_조회한다() {
        TourSummaryResponse tour = new TourSummaryResponse("t1", "제목", "주소", "tel", "img", 1.0, 1.0);
        FacilitySummaryResponse facility = new FacilitySummaryResponse("f1", "다른곳", "주소2", "tel", 90.0, 90.0);

        when(tourService.search(null, null, null, 1)).thenReturn(List.of(tour));
        when(facilityService.search(null, null, 1)).thenReturn(List.of(facility));

        List<ExploreItem> result = exploreService.explore(1L, null, null, null, null, 1);

        assertThat(result).extracting(ExploreItem::id).containsExactlyInAnyOrder("t1", "f1");
    }
}

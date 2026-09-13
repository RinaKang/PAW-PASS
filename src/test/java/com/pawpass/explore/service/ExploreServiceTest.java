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

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
    void tourapi_항목은_image가_그대로_실리고_kcisa_항목은_null이다() {
        TourSummaryResponse tour = new TourSummaryResponse("t1", "제목", "주소", "tel", "http://img.jpg", 1.0, 1.0);
        FacilitySummaryResponse facility = new FacilitySummaryResponse("f1", "다른곳", "주소2", "tel", 90.0, 90.0, null, null);

        when(tourService.search(null, null, null, null, null, null, 1)).thenReturn(List.of(tour));
        when(facilityService.search(null, null, 1)).thenReturn(List.of(facility));

        List<ExploreItem> result = exploreService.explore(1L, null, null, null, null, 1);

        assertThat(result).extracting(ExploreItem::id, ExploreItem::image)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("t1", "http://img.jpg"),
                        org.assertj.core.groups.Tuple.tuple("f1", null)
                );
    }

    // 2026-09-13: 프론트가 카테고리별 플레이스홀더 이미지를 고를 수 있게 category 필드 추가 - 소스별
    // 원본 규격 그대로(tourapi=contentTypeId, kcisa=category3) 통과시킨다(공통 규격으로 변환 안 함).
    @Test
    void category는_소스별_원본_규격_그대로_통과된다() {
        TourSummaryResponse tour = new TourSummaryResponse("t1", "제목", "주소", "tel", "img", 1.0, 1.0, "A05020900", "39");
        FacilitySummaryResponse facility = new FacilitySummaryResponse("f1", "다른곳", "주소2", "tel", 90.0, 90.0, null, null, "동물병원");

        when(tourService.search(null, null, null, null, null, null, 1)).thenReturn(List.of(tour));
        when(facilityService.search(null, null, 1)).thenReturn(List.of(facility));

        List<ExploreItem> result = exploreService.explore(1L, null, null, null, null, 1);

        assertThat(result).extracting(ExploreItem::id, ExploreItem::category)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("t1", "39"),
                        org.assertj.core.groups.Tuple.tuple("f1", "동물병원")
                );
    }

    @Test
    void 좌표가_50m_이내면_제목이_달라도_같은_장소로_보고_tourapi가_우선한다() {
        // 서울시청 근처 좌표, 두 번째 좌표는 약 30m 정도만 떨어져 있음
        TourSummaryResponse tour = new TourSummaryResponse("t1", "행복 펜션", "서울시", "tel", "img", 126.9780, 37.5665);
        FacilitySummaryResponse duplicate = new FacilitySummaryResponse("f1", "다른이름 카페", "서울시 다른표기", "tel2", 37.5667, 126.9782, null, null);
        FacilitySummaryResponse unique = new FacilitySummaryResponse("f2", "먼 장소", "부산시", "tel3", 35.1796, 129.0756, null, null);

        when(tourService.search(null, null, null, null, null, null, 1)).thenReturn(List.of(tour));
        when(facilityService.search(null, null, 1)).thenReturn(List.of(duplicate, unique));

        List<ExploreItem> result = exploreService.explore(1L, null, null, null, null, 1);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ExploreItem::id).containsExactly("t1", "f2");
        assertThat(result).extracting(ExploreItem::source).containsExactly("tourapi", "kcisa");
    }

    @Test
    void 좌표가_충분히_멀면_같은_제목이어도_둘_다_남는다() {
        TourSummaryResponse tour = new TourSummaryResponse("t1", "행복 펜션", "서울시", "tel", "img", 126.9780, 37.5665);
        FacilitySummaryResponse farAway = new FacilitySummaryResponse("f1", "행복 펜션", "서울시", "tel2", 35.1796, 129.0756, null, null);

        when(tourService.search(null, null, null, null, null, null, 1)).thenReturn(List.of(tour));
        when(facilityService.search(null, null, 1)).thenReturn(List.of(farAway));

        List<ExploreItem> result = exploreService.explore(1L, null, null, null, null, 1);

        assertThat(result).hasSize(2);
    }

    @Test
    void 좌표가_없으면_제목_정규화로_폴백한다() {
        TourSummaryResponse tour = new TourSummaryResponse("t1", "행복 펜션", "서울시", "tel", "img", null, null);
        FacilitySummaryResponse duplicate = new FacilitySummaryResponse("f1", "행복 펜션", "서울시 다른표기", "tel2", null, null, null, null);

        when(tourService.search(null, null, null, null, null, null, 1)).thenReturn(List.of(tour));
        when(facilityService.search(null, null, 1)).thenReturn(List.of(duplicate));

        List<ExploreItem> result = exploreService.explore(1L, null, null, null, null, 1);

        assertThat(result).hasSize(1);
        assertThat(result).extracting(ExploreItem::source).containsExactly("tourapi");
    }

    @Test
    void matchStatus_필터가_기본값과_일치하지_않으면_빈_목록() {
        TourSummaryResponse tour = new TourSummaryResponse("t1", "제목", "주소", "tel", "img", 1.0, 1.0);
        when(tourService.search(null, null, null, null, null, null, 1)).thenReturn(List.of(tour));
        when(facilityService.search(null, null, 1)).thenReturn(List.of());

        List<ExploreItem> result = exploreService.explore(1L, null, null, "가능", null, 1);

        assertThat(result).isEmpty();
    }

    @Test
    void matchStatus가_확인필요면_전부_반환() {
        TourSummaryResponse tour = new TourSummaryResponse("t1", "제목", "주소", "tel", "img", 1.0, 1.0);
        when(tourService.search(null, null, null, null, null, null, 1)).thenReturn(List.of(tour));
        when(facilityService.search(null, null, 1)).thenReturn(List.of());

        List<ExploreItem> result = exploreService.explore(1L, null, null, "확인필요", null, 1);

        assertThat(result).hasSize(1);
    }

    @Test
    void petId가_있으면_항목마다_실제_매칭을_계산한다() {
        TourSummaryResponse tour = new TourSummaryResponse("t1", "제목", "주소", "tel", "img", 1.0, 1.0);
        FacilitySummaryResponse facility = new FacilitySummaryResponse("f1", "다른 장소", "부산시", "tel3", 35.0, 129.0, null, null);

        when(tourService.search(null, null, null, null, null, null, 1)).thenReturn(List.of(tour));
        when(facilityService.search(null, null, 1)).thenReturn(List.of(facility));
        when(matchingService.resolveOptionalPet(1L, 9L)).thenReturn(PET);
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

        when(tourService.search(null, null, null, null, null, null, 1)).thenReturn(List.of(allowed, conditional, denied, unknown));
        when(facilityService.search(null, null, 1)).thenReturn(List.of());
        when(matchingService.resolveOptionalPet(1L, 9L)).thenReturn(PET);
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
        when(tourService.search(null, null, null, null, null, null, 1)).thenReturn(List.of(tour));
        when(facilityService.search(null, null, 1)).thenReturn(List.of());

        List<ExploreItem> result = exploreService.explore(1L, null, null, null, null, 1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).matchStatus()).isEqualTo("확인필요");
    }

    @Test
    void 항목_하나의_매칭_계산이_실패해도_나머지는_정상_반환된다() {
        TourSummaryResponse tourOk = new TourSummaryResponse("t1", "제목1", "주소", "tel", "img", 1.0, 1.0);
        TourSummaryResponse tourFails = new TourSummaryResponse("t2", "제목2", "주소2", "tel", "img", 2.0, 2.0);

        when(tourService.search(null, null, null, null, null, null, 1)).thenReturn(List.of(tourOk, tourFails));
        when(facilityService.search(null, null, 1)).thenReturn(List.of());
        when(matchingService.resolveOptionalPet(1L, 9L)).thenReturn(PET);
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

    // tour 항목이 비용이 제일 크다(TourAPI 3번+AI) - 상한(10개)을 넘는 tour 항목은 매칭을 아예 시도하지
    // 않고 기본값("확인필요")으로 남아야 한다. facility는 상한이 없어 전부 매칭을 시도해야 한다.
    @Test
    void tour_항목은_상한을_넘으면_매칭을_시도하지_않고_facility는_전부_시도한다() {
        List<TourSummaryResponse> tours = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            tours.add(new TourSummaryResponse("t" + i, "제목" + i, "주소" + i, "tel", "img", (double) i, (double) i));
        }
        FacilitySummaryResponse facility = new FacilitySummaryResponse("f1", "시설", "주소f", "tel", 90.0, 90.0, null, null);

        when(tourService.search(null, null, null, null, null, null, 1)).thenReturn(tours);
        when(facilityService.search(null, null, 1)).thenReturn(List.of(facility));
        when(matchingService.resolveOptionalPet(1L, 9L)).thenReturn(PET);
        when(matchingService.matchTourForPet(eq(PET), anyString()))
                .thenReturn(new MatchResponse(MatchResponse.STATUS_ALLOWED, "", "raw"));
        when(matchingService.matchFacilityForPet(PET, "f1"))
                .thenReturn(new MatchResponse(MatchResponse.STATUS_ALLOWED, "", "raw"));

        List<ExploreItem> result = exploreService.explore(1L, null, null, "확인필요", 9L, 1);

        // 상한을 넘겨 매칭을 아예 안 시도한 tour 항목들만 기본값("확인필요")으로 남아 이 필터에 걸린다
        assertThat(result).hasSize(2);
        assertThat(result).extracting(ExploreItem::id).containsExactlyInAnyOrder("t11", "t12");
        verify(matchingService, org.mockito.Mockito.times(10)).matchTourForPet(eq(PET), anyString());
        verify(matchingService, never()).matchTourForPet(PET, "t11");
        verify(matchingService, never()).matchTourForPet(PET, "t12");
    }

    // ===== 공통 region/category 값을 tour/facility 각자의 규격으로 변환하는지 =====

    @Test
    void 지역명이_매핑되면_tour는_법정동코드로_facility는_원문_그대로_조회한다() {
        when(tourService.search("11", null, null, null, null, null, 1)).thenReturn(List.of());
        when(facilityService.search("서울", null, 1)).thenReturn(List.of());

        exploreService.explore(1L, "서울", null, null, null, 1);

        verify(tourService).search("11", null, null, null, null, null, 1);
        verify(facilityService).search("서울", null, 1);
    }

    @Test
    void 시군_단위_지역명은_시도코드_시군구코드_조합으로_변환된다() {
        when(tourService.search("51", "150", null, null, null, null, 1)).thenReturn(List.of());
        when(facilityService.search("강릉", null, 1)).thenReturn(List.of());

        exploreService.explore(1L, "강릉", null, null, null, 1);

        verify(tourService).search("51", "150", null, null, null, null, 1);
        verify(facilityService).search("강릉", null, 1);
    }

    @Test
    void 매핑_테이블에_없는_지역명은_tour는_필터없이_facility는_원문_키워드로_조회한다() {
        when(tourService.search(null, null, null, null, null, null, 1)).thenReturn(List.of());
        when(facilityService.search("속초", null, 1)).thenReturn(List.of());

        exploreService.explore(1L, "속초", null, null, null, 1);

        verify(tourService).search(null, null, null, null, null, null, 1);
        verify(facilityService).search("속초", null, 1);
    }

    @Test
    void CULTURE_카테고리는_tour_문화시설코드와_facility_박물관_미술관_문예회관을_합쳐_조회한다() {
        FacilitySummaryResponse museum = new FacilitySummaryResponse("m1", "박물관1", "주소", "tel", 1.0, 1.0, null, null);
        FacilitySummaryResponse gallery = new FacilitySummaryResponse("g1", "미술관1", "주소", "tel", 2.0, 2.0, null, null);
        FacilitySummaryResponse hall = new FacilitySummaryResponse("h1", "문예회관1", "주소", "tel", 3.0, 3.0, null, null);

        when(tourService.search(null, null, "14", null, null, null, 1)).thenReturn(List.of());
        when(facilityService.search(null, "박물관", 1)).thenReturn(List.of(museum));
        when(facilityService.search(null, "미술관", 1)).thenReturn(List.of(gallery));
        when(facilityService.search(null, "문예회관", 1)).thenReturn(List.of(hall));

        List<ExploreItem> result = exploreService.explore(1L, null, "CULTURE", null, null, 1);

        verify(tourService).search(null, null, "14", null, null, null, 1);
        assertThat(result).extracting(ExploreItem::id).containsExactlyInAnyOrder("m1", "g1", "h1");
    }

    @Test
    void STAY_카테고리는_tour_숙박코드와_facility_펜션_호텔을_합쳐_조회한다() {
        FacilitySummaryResponse pension = new FacilitySummaryResponse("p1", "펜션1", "주소", "tel", 1.0, 1.0, null, null);
        FacilitySummaryResponse hotel = new FacilitySummaryResponse("h1", "호텔1", "주소", "tel", 2.0, 2.0, null, null);

        when(tourService.search(null, null, "32", null, null, null, 1)).thenReturn(List.of());
        when(facilityService.search(null, "펜션", 1)).thenReturn(List.of(pension));
        when(facilityService.search(null, "호텔", 1)).thenReturn(List.of(hotel));

        List<ExploreItem> result = exploreService.explore(1L, null, "STAY", null, null, 1);

        verify(tourService).search(null, null, "32", null, null, null, 1);
        assertThat(result).extracting(ExploreItem::id).containsExactlyInAnyOrder("p1", "h1");
    }

    // CAFE는 tour contentTypeId=39(음식점) 하나만으로는 카페/일반식당이 안 갈라져서 cat1/cat2/cat3
    // (A05/A0502/A05020900, 카페·전통찻집 세분류)까지 같이 넘긴다 - 2026-09-12 실측 확인.
    @Test
    void CAFE_카테고리는_tour_음식점코드에_카페_세분류_필터까지_같이_넘긴다() {
        FacilitySummaryResponse cafe = new FacilitySummaryResponse("c1", "카페1", "주소", "tel", 1.0, 1.0, null, null);

        when(tourService.search(null, null, "39", "A05", "A0502", "A05020900", 1)).thenReturn(List.of());
        when(facilityService.search(null, "카페", 1)).thenReturn(List.of(cafe));

        List<ExploreItem> result = exploreService.explore(1L, null, "CAFE", null, null, 1);

        verify(tourService).search(null, null, "39", "A05", "A0502", "A05020900", 1);
        assertThat(result).extracting(ExploreItem::id).containsExactly("c1");
    }

    // FOOD(음식점)는 cat1/cat2/cat3 세분류 필터 없이 39 전체를 요청은 하지만, 응답에서 카페로 명시
    // 태그된 항목(cat3="A05020900")은 걸러낸다 - 2026-09-12 실사용 테스트에서 FOOD 결과 대부분이 카페였던
    // 문제를 고친 것. facility 쪽은 category3="식당"이라 애초에 카페와 안 겹친다.
    @Test
    void FOOD_카테고리는_tour_음식점코드_전체를_요청하되_카페로_태그된_항목은_걸러낸다() {
        TourSummaryResponse taggedCafe = new TourSummaryResponse("t1", "카페처럼보이는곳", "주소", "tel", "img", 1.0, 1.0, "A05020900");
        TourSummaryResponse untaggedItem = new TourSummaryResponse("t2", "태그없는곳", "주소", "tel", "img", 2.0, 2.0, null);
        TourSummaryResponse taggedRestaurant = new TourSummaryResponse("t3", "한식당", "주소", "tel", "img", 3.0, 3.0, "A05020100");
        FacilitySummaryResponse restaurant = new FacilitySummaryResponse("r1", "식당1", "주소", "tel", 1.0, 1.0, null, null);

        when(tourService.search(null, null, "39", null, null, null, 1))
                .thenReturn(List.of(taggedCafe, untaggedItem, taggedRestaurant));
        when(facilityService.search(null, "식당", 1)).thenReturn(List.of(restaurant));

        List<ExploreItem> result = exploreService.explore(1L, null, "FOOD", null, null, 1);

        // cat3="A05020900"로 명시 태그된 t1만 빠지고, 태그 없는 t2(알려진 잔여 한계 - 걸러낼 수 없음)와
        // 카페가 아닌 태그의 t3, facility 쪽 r1은 그대로 남는다
        assertThat(result).extracting(ExploreItem::id).containsExactlyInAnyOrder("t2", "t3", "r1");
    }

    @Test
    void 매핑_테이블에_없는_카테고리는_tour_facility_둘_다_필터없이_조회한다() {
        when(tourService.search(null, null, null, null, null, null, 1)).thenReturn(List.of());
        when(facilityService.search(null, null, 1)).thenReturn(List.of());

        exploreService.explore(1L, null, "UNKNOWN_CATEGORY", null, null, 1);

        verify(tourService).search(null, null, null, null, null, null, 1);
        verify(facilityService).search(null, null, 1);
    }

    @Test
    void 지역_카테고리_둘_다_생략하면_양쪽_다_필터없이_전체_조회한다() {
        TourSummaryResponse tour = new TourSummaryResponse("t1", "제목", "주소", "tel", "img", 1.0, 1.0);
        FacilitySummaryResponse facility = new FacilitySummaryResponse("f1", "다른곳", "주소2", "tel", 90.0, 90.0, null, null);

        when(tourService.search(null, null, null, null, null, null, 1)).thenReturn(List.of(tour));
        when(facilityService.search(null, null, 1)).thenReturn(List.of(facility));

        List<ExploreItem> result = exploreService.explore(1L, null, null, null, null, 1);

        assertThat(result).extracting(ExploreItem::id).containsExactlyInAnyOrder("t1", "f1");
    }
}

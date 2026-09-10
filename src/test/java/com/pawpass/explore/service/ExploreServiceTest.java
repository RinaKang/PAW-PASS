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

        when(tourService.search(null, null, 1)).thenReturn(List.of(tour));
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

        when(tourService.search(null, null, 1)).thenReturn(List.of(tour));
        when(facilityService.search(null, null, 1)).thenReturn(List.of(farAway));

        List<ExploreItem> result = exploreService.explore(1L, null, null, null, null, 1);

        assertThat(result).hasSize(2);
    }

    @Test
    void 좌표가_없으면_제목_정규화로_폴백한다() {
        TourSummaryResponse tour = new TourSummaryResponse("t1", "행복 펜션", "서울시", "tel", "img", null, null);
        FacilitySummaryResponse duplicate = new FacilitySummaryResponse("f1", "행복 펜션", "서울시 다른표기", "tel2", null, null);

        when(tourService.search(null, null, 1)).thenReturn(List.of(tour));
        when(facilityService.search(null, null, 1)).thenReturn(List.of(duplicate));

        List<ExploreItem> result = exploreService.explore(1L, null, null, null, null, 1);

        assertThat(result).hasSize(1);
        assertThat(result).extracting(ExploreItem::source).containsExactly("tourapi");
    }

    @Test
    void matchStatus_필터가_기본값과_일치하지_않으면_빈_목록() {
        TourSummaryResponse tour = new TourSummaryResponse("t1", "제목", "주소", "tel", "img", 1.0, 1.0);
        when(tourService.search(null, null, 1)).thenReturn(List.of(tour));
        when(facilityService.search(null, null, 1)).thenReturn(List.of());

        List<ExploreItem> result = exploreService.explore(1L, null, null, "가능", null, 1);

        assertThat(result).isEmpty();
    }

    @Test
    void matchStatus가_확인필요면_전부_반환() {
        TourSummaryResponse tour = new TourSummaryResponse("t1", "제목", "주소", "tel", "img", 1.0, 1.0);
        when(tourService.search(null, null, 1)).thenReturn(List.of(tour));
        when(facilityService.search(null, null, 1)).thenReturn(List.of());

        List<ExploreItem> result = exploreService.explore(1L, null, null, "확인필요", null, 1);

        assertThat(result).hasSize(1);
    }

    @Test
    void petId가_있으면_항목마다_실제_매칭을_계산한다() {
        TourSummaryResponse tour = new TourSummaryResponse("t1", "제목", "주소", "tel", "img", 1.0, 1.0);
        FacilitySummaryResponse facility = new FacilitySummaryResponse("f1", "다른 장소", "부산시", "tel3", 35.0, 129.0);

        when(tourService.search(null, null, 1)).thenReturn(List.of(tour));
        when(facilityService.search(null, null, 1)).thenReturn(List.of(facility));
        when(matchingService.requireOwnedPet(1L, 9L)).thenReturn(PET);
        when(matchingService.matchTourForPet(PET, "t1"))
                .thenReturn(new MatchResponse(MatchResponse.STATUS_ALLOWED, "가능합니다", "raw"));
        when(matchingService.matchFacilityForPet(PET, "f1"))
                .thenReturn(new MatchResponse(MatchResponse.STATUS_DENIED, "불가합니다", "raw"));

        List<ExploreItem> result = exploreService.explore(1L, null, null, null, 9L, 1);

        assertThat(result).extracting(ExploreItem::id, ExploreItem::matchStatus)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("t1", MatchResponse.STATUS_ALLOWED),
                        org.assertj.core.groups.Tuple.tuple("f1", MatchResponse.STATUS_DENIED)
                );
    }

    @Test
    void 항목_하나의_매칭_계산이_실패해도_나머지는_정상_반환된다() {
        TourSummaryResponse tourOk = new TourSummaryResponse("t1", "제목1", "주소", "tel", "img", 1.0, 1.0);
        TourSummaryResponse tourFails = new TourSummaryResponse("t2", "제목2", "주소2", "tel", "img", 2.0, 2.0);

        when(tourService.search(null, null, 1)).thenReturn(List.of(tourOk, tourFails));
        when(facilityService.search(null, null, 1)).thenReturn(List.of());
        when(matchingService.requireOwnedPet(1L, 9L)).thenReturn(PET);
        when(matchingService.matchTourForPet(PET, "t1"))
                .thenReturn(new MatchResponse(MatchResponse.STATUS_ALLOWED, "가능합니다", "raw"));
        when(matchingService.matchTourForPet(PET, "t2"))
                .thenThrow(new RuntimeException("Gemini 429 등 외부 호출 실패 시뮬레이션"));

        List<ExploreItem> result = exploreService.explore(1L, null, null, null, 9L, 1);

        assertThat(result).extracting(ExploreItem::id, ExploreItem::matchStatus)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("t1", MatchResponse.STATUS_ALLOWED),
                        org.assertj.core.groups.Tuple.tuple("t2", "확인필요")
                );
    }
}

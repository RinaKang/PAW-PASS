package com.pawpass.facility.service;

import com.pawpass.facility.client.GooglePlacesClient;
import com.pawpass.facility.domain.PetFacility;
import com.pawpass.facility.dto.FacilityImageResponse;
import com.pawpass.facility.dto.FacilitySummaryResponse;
import com.pawpass.facility.repository.PetFacilityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FacilityServiceTest {

    @Mock
    private PetFacilityRepository petFacilityRepository;

    @Mock
    private GooglePlacesClient googlePlacesClient;

    private FacilityService facilityService;

    private void init() {
        facilityService = new FacilityService(petFacilityRepository, googlePlacesClient);
    }

    private void stubSearch(List<PetFacility> facilities) {
        when(petFacilityRepository.search(isNull(), isNull(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(facilities);
    }

    @Test
    void place_id가_이미_있는_시설은_다시_찾지_않고_사진과_저작자_표시를_받아온다() {
        init();
        PetFacility facility = facility("f1");
        facility.setGooglePlaceId("places/abc");
        stubSearch(List.of(facility));
        when(googlePlacesClient.fetchPhoto("places/abc", 800))
                .thenReturn(new GooglePlacesClient.PlacePhoto("https://lh3.googleusercontent.com/photo", "홍길동"));

        List<FacilitySummaryResponse> result = facilityService.search(null, null, 1);

        assertThat(result).extracting(FacilitySummaryResponse::image)
                .containsExactly("https://lh3.googleusercontent.com/photo");
        assertThat(result).extracting(FacilitySummaryResponse::imageAttribution).containsExactly("홍길동");
        verify(googlePlacesClient, never()).searchPlaceId(anyString());
        verify(petFacilityRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void place_id가_없는_시설은_조회_시점에_찾아서_저장하고_그_id로_사진을_받아온다() {
        init();
        PetFacility facility = facility("f1"); // googlePlaceId 미설정(null)
        stubSearch(List.of(facility));
        when(googlePlacesClient.searchPlaceId("시설-f1 주소")).thenReturn("places/new");
        when(googlePlacesClient.fetchPhoto("places/new", 800))
                .thenReturn(new GooglePlacesClient.PlacePhoto("https://img", null));

        List<FacilitySummaryResponse> result = facilityService.search(null, null, 1);

        assertThat(result).extracting(FacilitySummaryResponse::image).containsExactly("https://img");
        ArgumentCaptor<PetFacility> captor = ArgumentCaptor.forClass(PetFacility.class);
        verify(petFacilityRepository).save(captor.capture());
        assertThat(captor.getValue().getGooglePlaceId()).isEqualTo("places/new");
    }

    @Test
    void 검색해도_못_찾으면_빈_문자열로_저장해서_다음부턴_재시도하지_않는다() {
        init();
        PetFacility facility = facility("f1");
        stubSearch(List.of(facility));
        when(googlePlacesClient.searchPlaceId("시설-f1 주소")).thenReturn(null); // 정상 응답인데 결과 없음

        List<FacilitySummaryResponse> result = facilityService.search(null, null, 1);

        assertThat(result).extracting(FacilitySummaryResponse::image).containsExactly(new String[]{null});
        ArgumentCaptor<PetFacility> captor = ArgumentCaptor.forClass(PetFacility.class);
        verify(petFacilityRepository).save(captor.capture());
        assertThat(captor.getValue().getGooglePlaceId()).isEqualTo("");
        verify(googlePlacesClient, never()).fetchPhoto(anyString(), anyInt());
    }

    @Test
    void 이미_빈_문자열로_캐싱된_시설은_다시_검색하지_않는다() {
        init();
        PetFacility facility = facility("f1");
        facility.setGooglePlaceId(""); // 예전에 조회했는데 못 찾았던 시설
        stubSearch(List.of(facility));

        List<FacilitySummaryResponse> result = facilityService.search(null, null, 1);

        assertThat(result).extracting(FacilitySummaryResponse::image).containsExactly(new String[]{null});
        verify(googlePlacesClient, never()).searchPlaceId(anyString());
        verify(petFacilityRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void place_id_조회_자체가_실패하면_저장하지_않고_다음_조회때_재시도한다() {
        init();
        PetFacility facility = facility("f1");
        stubSearch(List.of(facility));
        when(googlePlacesClient.searchPlaceId("시설-f1 주소")).thenThrow(new RuntimeException("레이트리밋 등 일시적 실패"));

        List<FacilitySummaryResponse> result = facilityService.search(null, null, 1);

        assertThat(result).extracting(FacilitySummaryResponse::image).containsExactly(new String[]{null});
        verify(petFacilityRepository, never()).save(org.mockito.ArgumentMatchers.any());
        assertThat(facility.getGooglePlaceId()).isNull(); // 다음 조회 때 다시 시도할 수 있게 null 그대로
    }

    @Test
    void 한_페이지에서_사진_조회는_상한개수까지만_시도하고_나머지는_null이다() {
        init();
        List<PetFacility> facilities = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            PetFacility facility = facility("f" + i);
            facility.setGooglePlaceId("places/" + i);
            facilities.add(facility);
        }
        stubSearch(facilities);
        when(googlePlacesClient.fetchPhoto(anyString(), anyInt()))
                .thenReturn(new GooglePlacesClient.PlacePhoto("https://img", null));

        List<FacilitySummaryResponse> result = facilityService.search(null, null, 1);

        long withImage = result.stream().filter(r -> r.image() != null).count();
        assertThat(withImage).isEqualTo(10); // MAX_ITEMS_TO_FETCH_IMAGE
        verify(googlePlacesClient, times(10)).fetchPhoto(anyString(), anyInt());
    }

    // place_id 지연 해소(searchPlaceId)도 같은 상한을 공유한다 - 21,000여 건을 배치로 미리 다 채우는 대신
    // 실제 노출되는 시설만 그때그때 찾는 설계라, 여기서도 구글 호출량이 페이지당 무한정 늘지 않아야 한다.
    @Test
    void place_id_지연_해소도_같은_상한을_넘기지_않는다() {
        init();
        List<PetFacility> facilities = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            facilities.add(facility("f" + i)); // 전부 googlePlaceId 미설정
        }
        stubSearch(facilities);
        when(googlePlacesClient.searchPlaceId(anyString())).thenReturn(null);

        facilityService.search(null, null, 1);

        verify(googlePlacesClient, times(10)).searchPlaceId(anyString());
    }

    // getDetail()과 달리 getImage()는 GET /facilities/{id}/image 전용 - 상세 페이지 본문(getDetail)은
    // 구글 호출 없이 즉시 응답하고, 프론트가 사진만 이 엔드포인트로 따로(병렬/지연) 불러가게 하기 위함
    // (2026-09-12, 프론트에서 "상세 페이지 진입 시 구글 API 지연으로 체감 속도 저하" 요청에 대한 대응).
    @Test
    void getImage는_시설_하나의_사진만_가져온다() {
        init();
        PetFacility facility = facility("f1");
        facility.setGooglePlaceId("places/abc");
        when(petFacilityRepository.findById("f1")).thenReturn(Optional.of(facility));
        when(googlePlacesClient.fetchPhoto("places/abc", 800))
                .thenReturn(new GooglePlacesClient.PlacePhoto("https://img", "홍길동"));

        FacilityImageResponse result = facilityService.getImage("f1");

        assertThat(result.image()).isEqualTo("https://img");
        assertThat(result.imageAttribution()).isEqualTo("홍길동");
    }

    @Test
    void getImage는_존재하지_않는_시설이면_예외() {
        init();
        when(petFacilityRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> facilityService.getImage("missing"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // 2026-09-13: 프론트가 카테고리별 플레이스홀더 이미지를 고를 수 있게 category(=category3) 필드 추가.
    @Test
    void category는_category3_값을_그대로_실어준다() {
        init();
        PetFacility facility = facility("f1"); // category3="동물병원"
        stubSearch(List.of(facility));

        List<FacilitySummaryResponse> result = facilityService.search(null, null, 1);

        assertThat(result).extracting(FacilitySummaryResponse::category).containsExactly("동물병원");
    }

    // 2026-09-13: 목록(FacilitySummaryResponse)엔 lat/lng가 있는데 상세엔 없어서 상세 페이지 지도에 핀을
    // 못 찍던 실제 프론트 리포트로 발견 - PetFacility에 이미 저장된 값을 그대로 실어준다(외부 호출 불필요).
    @Test
    void getDetail은_좌표도_함께_반환한다() {
        init();
        PetFacility facility = PetFacility.builder()
                .id("f1").title("시설").address("주소").lat(37.5665).lng(126.9780).issuedDate("2026-01-01")
                .build();
        when(petFacilityRepository.findById("f1")).thenReturn(Optional.of(facility));

        var result = facilityService.getDetail("f1");

        assertThat(result.lat()).isEqualTo(37.5665);
        assertThat(result.lng()).isEqualTo(126.9780);
    }

    // 2026-09-14: 동선 화면 장소 검색(keyword) 추가 - region/category 조건 없이 이름/주소로 찾는
    // searchByKeyword() 경로. 이미지 예산 로직(상한 10개)은 search()와 완전히 같은 코드를 타므로
    // 여기선 조회 조건 위임과 매핑만 확인한다.
    @Test
    void searchByKeyword는_repository에_키워드_그대로_위임한다() {
        init();
        PetFacility facility = facility("f1");
        when(petFacilityRepository.searchByKeyword(org.mockito.ArgumentMatchers.eq("행복카페"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(facility));

        List<FacilitySummaryResponse> result = facilityService.searchByKeyword("행복카페", 1);

        assertThat(result).extracting(FacilitySummaryResponse::id).containsExactly("f1");
    }

    private PetFacility facility(String id) {
        return PetFacility.builder()
                .id(id).title("시설-" + id).address("주소").category3("동물병원").issuedDate("2026-01-01")
                .build();
    }
}

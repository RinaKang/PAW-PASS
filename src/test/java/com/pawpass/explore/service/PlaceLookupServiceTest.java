package com.pawpass.explore.service;

import com.pawpass.facility.dto.FacilityDetailResponse;
import com.pawpass.facility.service.FacilityService;
import com.pawpass.global.util.DataSource;
import com.pawpass.tour.service.TourService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaceLookupServiceTest {

    @Mock
    private TourService tourService;

    @Mock
    private FacilityService facilityService;

    @InjectMocks
    private PlaceLookupService placeLookupService;

    @Test
    void TOURAPI는_TourService_요약을_사용한다() {
        when(tourService.getSummary("123")).thenReturn(new TourService.PlaceSummary("제목", "주소"));

        PlaceLookupService.PlaceDetail detail = placeLookupService.lookup(DataSource.TOURAPI, "123");

        assertThat(detail.title()).isEqualTo("제목");
        assertThat(detail.addr()).isEqualTo("주소");
    }

    @Test
    void TOURAPI_원본이_없으면_null() {
        when(tourService.getSummary("123")).thenReturn(null);

        assertThat(placeLookupService.lookup(DataSource.TOURAPI, "123")).isNull();
    }

    @Test
    void KCISA는_FacilityService_상세를_사용한다() {
        FacilityDetailResponse detail = new FacilityDetailResponse(
                "시설명", "시설주소", "tel", "hours", null, null, null, null);
        when(facilityService.getDetail("f1")).thenReturn(detail);

        PlaceLookupService.PlaceDetail result = placeLookupService.lookup(DataSource.KCISA, "f1");

        assertThat(result.title()).isEqualTo("시설명");
        assertThat(result.addr()).isEqualTo("시설주소");
    }

    @Test
    void 조회_중_예외가_나면_null로_대체된다() {
        when(facilityService.getDetail("missing")).thenThrow(new IllegalArgumentException("존재하지 않는 시설입니다"));

        assertThat(placeLookupService.lookup(DataSource.KCISA, "missing")).isNull();
    }

    // 2026-09-13: 원래 IllegalArgumentException("존재하지 않음")만 잡아서, TourAPI 레이트리밋/타임아웃
    // 같은 다른 실패(WebClientResponseException 등)는 그대로 새어나가 즐겨찾기/방문기록 목록 전체를
    // 500으로 깨뜨리던 실제 버그 - 넓게 잡도록 고친 뒤에도 여전히 null로 대체되는지 확인.
    @Test
    void 외부_API_호출_자체가_실패해도_null로_대체되고_전파되지_않는다() {
        when(tourService.getSummary("123")).thenThrow(new RuntimeException("TourAPI 일일 요청 한도 초과(429)"));

        assertThat(placeLookupService.lookup(DataSource.TOURAPI, "123")).isNull();
    }
}

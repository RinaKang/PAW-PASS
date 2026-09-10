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
                "시설명", "시설주소", "tel", "hours", null, null);
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
}

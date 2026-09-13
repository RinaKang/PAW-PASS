package com.pawpass.tour.service;

import com.pawpass.tour.client.TourApiClient;
import com.pawpass.tour.dto.TourDetailResponse;
import com.pawpass.tour.dto.external.TourDetailCommonItem;
import com.pawpass.tour.dto.external.TourDetailImageItem;
import com.pawpass.tour.dto.external.TourDetailIntroItem;
import com.pawpass.tour.dto.external.TourDetailPetTourItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TourServiceTest {

    @Mock
    private TourApiClient tourApiClient;

    @InjectMocks
    private TourService tourService;

    @Test
    void getDetail_존재하지_않으면_예외() {
        when(tourApiClient.detailCommon("999")).thenReturn(null);

        assertThatThrownBy(() -> tourService.getDetail("999"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getDetail_세_API_응답을_하나로_합친다() {
        TourDetailCommonItem common = new TourDetailCommonItem(
                "123", "12", "행복 반려동물 공원", "서울시 강남구", "테헤란로 1", "02-1234-5678",
                "http://img1.jpg", "http://img2.jpg", "설명", "20260101120000");
        TourDetailIntroItem intro = new TourDetailIntroItem("123", "09:00~18:00");
        TourDetailPetTourItem pet = new TourDetailPetTourItem("123", "가능", "소형견만 가능", "목줄 필수", "실내 동반 불가");

        when(tourApiClient.detailCommon("123")).thenReturn(common);
        when(tourApiClient.detailIntro("123", "12")).thenReturn(intro);
        when(tourApiClient.detailPetTour("123")).thenReturn(pet);

        TourDetailResponse result = tourService.getDetail("123");

        assertThat(result.contentId()).isEqualTo("123");
        assertThat(result.title()).isEqualTo("행복 반려동물 공원");
        assertThat(result.addr()).isEqualTo("서울시 강남구 테헤란로 1");
        assertThat(result.hours()).isEqualTo("09:00~18:00");
        assertThat(result.images()).containsExactly("http://img1.jpg", "http://img2.jpg");
        assertThat(result.issuedDate()).isEqualTo("20260101120000");
        assertThat(result.petCondition().acmpyTypeCd()).isEqualTo("가능");
        assertThat(result.petCondition().acmpyPsblCpam()).isEqualTo("소형견만 가능");
    }

    // 2026-09-13: 목록(TourSummaryResponse)엔 mapX/mapY가 있는데 상세엔 없어서 상세 페이지 지도에 핀을
    // 못 찍던 실제 프론트 리포트로 발견 - detailCommon2의 mapx/mapy를 받아서 실어준다.
    @Test
    void getDetail_좌표도_함께_반환한다() {
        TourDetailCommonItem common = new TourDetailCommonItem(
                "123", "12", "제목", "주소1", null, "tel", "img", null, "overview",
                "126.9780", "37.5665", "modified");
        when(tourApiClient.detailCommon("123")).thenReturn(common);
        when(tourApiClient.detailIntro("123", "12")).thenReturn(null);
        when(tourApiClient.detailPetTour("123")).thenReturn(null);

        TourDetailResponse result = tourService.getDetail("123");

        assertThat(result.mapX()).isEqualTo(126.9780);
        assertThat(result.mapY()).isEqualTo(37.5665);
    }

    @Test
    void getDetail_intro나_petTour가_없어도_common만으로_응답한다() {
        TourDetailCommonItem common = new TourDetailCommonItem(
                "123", "12", "제목", "주소1", null, "tel", "img", null, "overview", "modified");

        when(tourApiClient.detailCommon("123")).thenReturn(common);
        when(tourApiClient.detailIntro("123", "12")).thenReturn(null);
        when(tourApiClient.detailPetTour("123")).thenReturn(null);

        TourDetailResponse result = tourService.getDetail("123");

        assertThat(result.addr()).isEqualTo("주소1");
        assertThat(result.hours()).isNull();
        assertThat(result.images()).containsExactly("img");
        assertThat(result.petCondition().acmpyTypeCd()).isNull();
    }

    @Test
    void getDetail_detailImage2_썸네일이_있으면_firstimage_대신_그걸_쓴다() {
        TourDetailCommonItem common = new TourDetailCommonItem(
                "123", "12", "제목", "주소1", null, "tel", "http://firstimage.jpg", "http://firstimage2.jpg", "overview", "modified");
        when(tourApiClient.detailCommon("123")).thenReturn(common);
        when(tourApiClient.detailIntro("123", "12")).thenReturn(null);
        when(tourApiClient.detailPetTour("123")).thenReturn(null);
        when(tourApiClient.detailImage("123")).thenReturn(List.of(
                new TourDetailImageItem("http://thumb1.jpg"),
                new TourDetailImageItem("http://thumb2.jpg"),
                new TourDetailImageItem("http://thumb3.jpg")
        ));

        TourDetailResponse result = tourService.getDetail("123");

        assertThat(result.images()).containsExactly("http://thumb1.jpg", "http://thumb2.jpg", "http://thumb3.jpg");
    }

    @Test
    void getDetail_detailImage2가_비어있으면_firstimage로_폴백한다() {
        TourDetailCommonItem common = new TourDetailCommonItem(
                "123", "12", "제목", "주소1", null, "tel", "http://firstimage.jpg", "http://firstimage2.jpg", "overview", "modified");
        when(tourApiClient.detailCommon("123")).thenReturn(common);
        when(tourApiClient.detailIntro("123", "12")).thenReturn(null);
        when(tourApiClient.detailPetTour("123")).thenReturn(null);
        when(tourApiClient.detailImage("123")).thenReturn(List.of());

        TourDetailResponse result = tourService.getDetail("123");

        assertThat(result.images()).containsExactly("http://firstimage.jpg", "http://firstimage2.jpg");
    }

    @Test
    void getSummary_존재하지_않으면_null() {
        when(tourApiClient.detailCommon("999")).thenReturn(null);

        assertThat(tourService.getSummary("999")).isNull();
    }

    @Test
    void getSummary_title과_addr만_반환() {
        TourDetailCommonItem common = new TourDetailCommonItem(
                "123", "12", "제목", "주소1", "주소2", "tel", "img", null, "overview", "modified");
        when(tourApiClient.detailCommon("123")).thenReturn(common);

        TourService.PlaceSummary summary = tourService.getSummary("123");

        assertThat(summary.title()).isEqualTo("제목");
        assertThat(summary.addr()).isEqualTo("주소1 주소2");
    }
}
